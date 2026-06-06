package github.businessdirt.axite.vanadium.core.imgui.panels

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.core.imgui.RenderGraphDrawable
import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags

object RenderPassesPanel : ImGuiPanel("Render Passes") {

    override fun drawContent() {
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
            RenderGraphDrawable.draw()
        }
    }
}