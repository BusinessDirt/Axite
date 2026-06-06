package github.businessdirt.axite.vanadium.core.utils

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.core.dag.Node
import github.businessdirt.axite.vanadium.renderer.graph.RenderPassNode
import github.businessdirt.axite.vanadium.renderer.passes.PostProcessPass
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags

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

    fun drawRenderGraph() {
        val rg = Vanadium.renderer.renderGraph
        if (rg.layers.isEmpty()) {
            ImGui.text("Graph is empty or not yet compiled.")
            return
        }

        val drawList = ImGui.getWindowDrawList()
        val startCursorX = ImGui.getCursorScreenPosX()
        val startCursorY = ImGui.getCursorScreenPosY()

        val nodeWidth = 150f
        val nodeHeight = 50f
        val horizontalSpacing = 50f
        val verticalSpacing = 20f

        val nodePositions = mutableMapOf<Node<*>, Pair<Float, Float>>()

        rg.layers.forEachIndexed { layerIdx, layer ->
            layer.forEachIndexed { nodeIdx, node ->
                val x = startCursorX + layerIdx * (nodeWidth + horizontalSpacing)
                val y = startCursorY + nodeIdx * (nodeHeight + verticalSpacing)
                nodePositions[node] = x to y
            }
        }

        rg.layers.forEachIndexed { layerIdx, layer ->
            layer.forEachIndexed { nodeIdx, node ->
                val pos = nodePositions[node]!!
                val x = pos.first
                val y = pos.second

                val name = (node as? RenderPassNode)?.name ?: "Unknown"

                // Draw dependencies (lines first so they are under nodes)
                node.dependencies.forEach { dep ->
                    val depPos = nodePositions[dep]
                    if (depPos != null) {
                        val startX = depPos.first + nodeWidth
                        val startY = depPos.second + nodeHeight / 2f
                        val endX = x
                        val endY = y + nodeHeight / 2f
                        drawList.addLine(startX, startY, endX, endY, ImGui.getColorU32(ImGuiCol.PlotLines), 2f)
                    }
                }

                // Draw node box
                drawList.addRectFilled(x, y, x + nodeWidth, y + nodeHeight, ImGui.getColorU32(ImGuiCol.Button))
                drawList.addRect(x, y, x + nodeWidth, y + nodeHeight, ImGui.getColorU32(ImGuiCol.Border))

                // Draw node name
                val textSize = ImGui.calcTextSize(name)
                drawList.addText(x + (nodeWidth - textSize.x) / 2f, y + (nodeHeight - textSize.y) / 2f, ImGui.getColorU32(ImGuiCol.Text), name)

                // Tooltip for resources
                if (ImGui.isMouseHoveringRect(x, y, x + nodeWidth, y + nodeHeight)) {
                    ImGui.beginTooltip()
                    ImGui.text("Pass: $name")
                    ImGui.separator()
                    ImGui.text("Reads: ${(node as? RenderPassNode)?.readResources?.joinToString() ?: "None"}")
                    ImGui.text("Writes: ${(node as? RenderPassNode)?.writeResources?.joinToString() ?: "None"}")
                    ImGui.endTooltip()
                }
            }
        }

        // Advance cursor to reserve space
        val totalWidth = rg.layers.size * (nodeWidth + horizontalSpacing)
        val maxHeight = rg.layers.maxOfOrNull { it.size } ?: 0
        val totalHeight = maxHeight * (nodeHeight + verticalSpacing)
        ImGui.dummy(totalWidth, totalHeight)
    }

    fun renderPasses(flags: Int = ImGuiWindowFlags.None) = window("Render Passes", flags) {
        if (ImGui.collapsingHeader("Individual Passes", ImGuiTreeNodeFlags.DefaultOpen)) {
            Vanadium.renderer.passes.filter { it.isInitialized }.forEach { pass ->
                if (ImGui.treeNode(pass.javaClass.simpleName)) {
                    pass.renderImGui()
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