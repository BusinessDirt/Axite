package github.businessdirt.axite.vanadium.core.utils

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.renderer.graph.RenderPassNode
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiDockNodeFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object ImGuiUtils {

    /**
     * Scope wrapper for an ImGui window. Automatically handles calling [ImGui.end].
     * * Usage:
     * ```
     * ImGuiUtils.window("Settings") {
     * ImGui.text("Hello World")
     * }
     * ```
     */
    inline fun window(
        name: String,
        flags: Int = ImGuiWindowFlags.None,
        block: () -> Unit
    ) {
        if (ImGui.begin(name, flags)) {
            try {
                block()
            } finally {
                ImGui.end()
            }
        } else {
            ImGui.end() // ImGui requires calling end() even if begin() returns false
        }
    }

    /**
     * Scope wrapper for a generic ID stack element.
     * Prevents ID collision bugs across separate UI components.
     */
    inline fun withId(id: String, block: () -> Unit) {
        ImGui.pushID(id)
        try {
            block()
        } finally {
            ImGui.popID()
        }
    }

    inline fun withId(id: Int, block: () -> Unit) {
        ImGui.pushID(id)
        try {
            block()
        } finally {
            ImGui.popID()
        }
    }

    /**
     * Scope wrapper for disabled UI styling.
     */
    inline fun disabled(condition: Boolean = true, block: () -> Unit) {
        if (condition) ImGui.beginDisabled(true)
        try {
            block()
        } finally {
            if (condition) ImGui.endDisabled()
        }
    }

    /**
     * Scope wrapper for tree nodes (e.g., Scene Graph Hierarchies).
     */
    inline fun treeNode(label: String, block: () -> Unit) {
        if (ImGui.treeNode(label)) {
            try {
                block()
            } finally {
                ImGui.treePop()
            }
        }
    }

    /**
     * Creates a dockspace that covers the entire viewport.
     * This allows other windows to be docked into the background of the application.
     * Uses a transparent window to allow the scene to be visible.
     */
    fun dockspace(flags: Int = ImGuiDockNodeFlags.PassthruCentralNode) {
        val viewport = ImGui.getMainViewport()
        ImGui.setNextWindowPos(viewport.posX, viewport.posY)
        ImGui.setNextWindowSize(viewport.sizeX, viewport.sizeY)
        ImGui.setNextWindowViewport(viewport.id)

        val windowFlags = ImGuiWindowFlags.NoDocking or ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoBringToFrontOnFocus or
                ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoBackground

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)

        ImGui.begin("###DockSpaceWindow", windowFlags)
        ImGui.popStyleVar(3)

        val dockspaceId = ImGui.getID("MainDockSpace")
        ImGui.dockSpace(dockspaceId, 0.0f, 0.0f, flags)
        ImGui.end()
    }

    private fun drawArrow(drawList: imgui.ImDrawList, startX: Float, startY: Float, endX: Float, endY: Float, color: Int) {
        val thickness = 2f
        drawList.addLine(startX, startY, endX, endY, color, thickness)

        val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble()).toFloat()
        val arrowSize = 8f

        val x1 = endX - arrowSize * cos((angle - Math.PI / 6)).toFloat()
        val y1 = endY - arrowSize * sin((angle - Math.PI / 6)).toFloat()
        val x2 = endX - arrowSize * cos((angle + Math.PI / 6)).toFloat()
        val y2 = endY - arrowSize * sin((angle + Math.PI / 6)).toFloat()

        drawList.addTriangleFilled(endX, endY, x1, y1, x2, y2, color)
    }

    fun drawRenderGraph() {
        val rg = Vanadium.renderer.renderGraph
        if (rg.layers.isEmpty()) {
            ImGui.text("Graph is empty or not yet compiled.")
            return
        }

        val drawList = ImGui.getWindowDrawList()
        val startCursorX = ImGui.getCursorScreenPosX()
        val startCursorY = ImGui.getCursorScreenPosY()

        val passWidth = 150f
        val passHeight = 40f
        val resWidth = 120f
        val resHeight = 30f
        val horizontalSpacing = 60f
        val verticalSpacing = 20f

        // Track positions for arrows: Any -> (X, Y)
        // Passes are RenderPassNode, Resources are Pair<String, Int> (Name, Column)
        val nodePositions = mutableMapOf<Any, Pair<Float, Float>>()

        // Columns: Alternating Pass Layers and Resource Layers
        // We'll build a virtual layout first
        val columns = mutableListOf<List<Any>>()

        // Initial inputs (resources read but not yet written)
        val initialInputs = rg.nodes.flatMap { (it as? RenderPassNode)?.readResources ?: emptySet() }
            .filter { res -> rg.nodes.none { (it as? RenderPassNode)?.writeResources?.contains(res) == true } }
            .toSet()

        if (initialInputs.isNotEmpty()) {
            columns.add(initialInputs.toList())
        }

        rg.layers.forEach { layer ->
            // Add Pass Layer
            columns.add(layer)
            
            // Add Resource Layer (written by this pass layer)
            val writtenInLayer = layer.flatMap { (it as? RenderPassNode)?.writeResources ?: emptySet() }.toSet()
            if (writtenInLayer.isNotEmpty()) {
                columns.add(writtenInLayer.toList())
            }
        }

        // Calculate positions
        var currentX = startCursorX
        columns.forEachIndexed { colIdx, colNodes ->
            val isPassCol = colNodes.firstOrNull() is RenderPassNode
            val colWidth = if (isPassCol) passWidth else resWidth
            
            colNodes.forEachIndexed { rowIdx, node ->
                val x = currentX
                val y = startCursorY + rowIdx * (passHeight.coerceAtLeast(resHeight) + verticalSpacing)
                
                val key = if (node is String) node to colIdx else node
                nodePositions[key] = x to y
            }
            currentX += colWidth + horizontalSpacing
        }

        // Draw edges (arrows)
        columns.forEachIndexed { colIdx, colNodes ->
            colNodes.forEach { node ->
                val pos = nodePositions[if (node is String) node to colIdx else node]!!
                val x = pos.first
                val y = pos.second

                if (node is RenderPassNode) {
                    val centerY = y + passHeight / 2f
                    
                    // Reads: from previous resource version to this pass
                    node.readResources.forEach { resName ->
                        // Find the "latest" version of this resource in previous columns
                        for (prevCol in colIdx - 1 downTo 0) {
                            val resKey = resName to prevCol
                            val resPos = nodePositions[resKey]
                            if (resPos != null) {
                                drawArrow(drawList, resPos.first + resWidth, resPos.second + resHeight / 2f, x, centerY, ImGui.getColorU32(ImGuiCol.PlotLines))
                                break
                            }
                        }
                    }

                    // Writes: from this pass to the resource in the NEXT column
                    node.writeResources.forEach { resName ->
                        val nextCol = colIdx + 1
                        val resKey = resName to nextCol
                        val resPos = nodePositions[resKey]
                        if (resPos != null) {
                            drawArrow(drawList, x + passWidth, centerY, resPos.first, resPos.second + resHeight / 2f, ImGui.getColorU32(ImGuiCol.PlotLines))
                        }
                    }
                }
            }
        }

        // Draw nodes
        columns.forEachIndexed { colIdx, colNodes ->
            colNodes.forEach { node ->
                val key = if (node is String) node to colIdx else node
                val pos = nodePositions[key]!!
                val x = pos.first
                val y = pos.second

                if (node is RenderPassNode) {
                    drawList.addRectFilled(x, y, x + passWidth, y + passHeight, ImGui.getColorU32(ImGuiCol.Button))
                    drawList.addRect(x, y, x + passWidth, y + passHeight, ImGui.getColorU32(ImGuiCol.Border), 5f)
                    
                    val textSize = ImGui.calcTextSize(node.name)
                    drawList.addText(x + (passWidth - textSize.x) / 2f, y + (passHeight - textSize.y) / 2f, ImGui.getColorU32(ImGuiCol.Text), node.name)
                } else if (node is String) {
                    // Resource Node (Pill shape)
                    val color = if (node.contains("depth", ignoreCase = true)) 0xFF888844.toInt() else 0xFF448888.toInt()
                    drawList.addRectFilled(x, y, x + resWidth, y + resHeight, color, 15f)
                    drawList.addRect(x, y, x + resWidth, y + resHeight, ImGui.getColorU32(ImGuiCol.Border), 15f)
                    
                    val textSize = ImGui.calcTextSize(node)
                    drawList.addText(x + (resWidth - textSize.x) / 2f, y + (resHeight - textSize.y) / 2f, ImGui.getColorU32(ImGuiCol.Text), node)
                }
            }
        }

        // Advance cursor
        val totalWidth = currentX - startCursorX
        val maxHeight = columns.maxOfOrNull { it.size } ?: 0
        val totalHeight = maxHeight * (passHeight.coerceAtLeast(resHeight) + verticalSpacing)
        ImGui.dummy(totalWidth, totalHeight)
    }

    fun renderPasses(flags: Int = ImGuiWindowFlags.None) = window("Render Passes", flags) {
        if (ImGui.collapsingHeader("Individual Passes", ImGuiTreeNodeFlags.DefaultOpen)) {
            Vanadium.renderer.passes.filter { it.isInitialized }.forEach { pass ->
                if (ImGui.treeNode(pass.javaClass.simpleName)) {
                    pass.draw()
                    ImGui.treePop()
                }
            }
        }

        ImGui.spacing()

        if (ImGui.collapsingHeader("Render Graph", ImGuiTreeNodeFlags.DefaultOpen)) {
            drawRenderGraph()
        }
    }
}