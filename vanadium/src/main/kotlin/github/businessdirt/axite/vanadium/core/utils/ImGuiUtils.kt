package github.businessdirt.axite.vanadium.core.utils

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.renderer.passes.PostProcessPass
import imgui.ImGui
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

    fun renderPasses(flags: Int = ImGuiWindowFlags.None) = window("Render Passes", flags) {
        Vanadium.renderer.passes.filter{ it.isInitialized }.forEach { pass ->
            if (ImGui.collapsingHeader(pass.javaClass.simpleName)) pass.renderImGui()
        }
    }
}