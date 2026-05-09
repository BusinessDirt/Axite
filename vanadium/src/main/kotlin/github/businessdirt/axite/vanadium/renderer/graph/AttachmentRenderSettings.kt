package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import org.lwjgl.vulkan.VK13.VK_ATTACHMENT_STORE_OP_STORE

data class AttachmentRenderSettings(
    val attachment: Attachment,
    val loadOp: Int, // e.g., VK_ATTACHMENT_LOAD_OP_CLEAR or VK_ATTACHMENT_LOAD_OP_LOAD
    val storeOp: Int = VK_ATTACHMENT_STORE_OP_STORE
)