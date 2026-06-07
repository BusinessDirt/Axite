package github.businessdirt.axite.vanadium.core.imgui.panels

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.renderer.passes.ImGuiPass
import imgui.ImGui
import imgui.flag.ImGuiWindowFlags

object ViewportPanel : ImGuiPanel("Viewport") {

    override fun draw() {
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowPadding, 0f, 0f)
        super.draw()
        ImGui.popStyleVar()
    }

    override fun drawContent() {
        val scene = Vanadium.scene ?: return
        val registry = Vanadium.renderer.renderGraph.registry
        
        // Use "viewport_target" if enabled, otherwise we shouldn't even be drawing this or it's empty
        val attachment = try {
            registry["viewport_target"]
        } catch (e: Exception) {
            return
        }

        val textureId = ImGuiPass.getTextureId(attachment)

        val windowWidth = ImGui.getContentRegionAvailX()
        val windowHeight = ImGui.getContentRegionAvailY()

        // Calculate aspect ratio
        val textureAspect = attachment.width.toFloat() / attachment.height.toFloat()
        val windowAspect = windowWidth / windowHeight

        var drawWidth = windowWidth
        var drawHeight = windowHeight

        if (windowAspect > textureAspect) {
            drawWidth = windowHeight * textureAspect
        } else {
            drawHeight = windowWidth / textureAspect
        }

        // Center the image
        val cursorX = ImGui.getCursorPosX() + (windowWidth - drawWidth) / 2f
        val cursorY = ImGui.getCursorPosY() + (windowHeight - drawHeight) / 2f
        ImGui.setCursorPos(cursorX, cursorY)

        ImGui.image(textureId, drawWidth, drawHeight, 0f, 0f, 1f, 1f)
    }
}
