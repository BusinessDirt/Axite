package github.businessdirt.axite.vanadium.core.imgui

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.renderer.graph.RenderPassNode
import imgui.ImDrawList
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
     * Scope wrapper for an ImGui window. Automatically handles calling [imgui.ImGui.end].
     * * Usage:
     * ```
     * ImGuiUtils.window("Settings") {
     *     ImGui.text("Hello World")
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
        if (!ImGui.treeNode(label)) return

        try {
            block()
        } finally {
            ImGui.treePop()
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

        window("###DockSpaceWindow", windowFlags) {
            ImGui.popStyleVar(3)
            val dockspaceId = ImGui.getID("MainDockSpace")
            ImGui.dockSpace(dockspaceId, 0.0f, 0.0f, flags)
        }
    }

    fun drawArrow(drawList: ImDrawList, startX: Float, startY: Float, endX: Float, endY: Float, color: Int) {
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
}