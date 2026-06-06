#version 450

layout (location = 0) in vec2 inPos;
layout (location = 1) in vec2 inTextCoords;
layout (location = 2) in uint inColor;

layout (push_constant) uniform PushConstants {
    vec2 scale;
} pushConstants;

layout (location = 0) out vec2 outTextCoords;
layout (location = 1) out vec4 outColor;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    outTextCoords = inTextCoords;
    outColor = vec4(
        (inColor & 0xFFu) / 255.0,
        ((inColor >> 8u) & 0xFFu) / 255.0,
        ((inColor >> 16u) & 0xFFu) / 255.0,
        ((inColor >> 24u) & 0xFFu) / 255.0
    );
    gl_Position = vec4(inPos * pushConstants.scale + vec2(-1.0, -1.0), 0.0, 1.0);
}
