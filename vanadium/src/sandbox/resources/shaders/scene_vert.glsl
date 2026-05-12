#version 450

// Hardcoded positions for a triangle in Normalized Device Coordinates (NDC)
vec2 positions[3] = vec2[](
    vec2(0.0, -0.5), // Top
    vec2(0.5, 0.5),  // Bottom Right
    vec2(-0.5, 0.5)  // Bottom Left
);

layout(location = 0) out vec4 outColor;

void main() {
    // Select position based on vertex index (0, 1, or 2)
    gl_Position = vec4(positions[gl_VertexIndex], 0.0, 1.0);
    outColor = vec4(positions[gl_VertexIndex], 1.0, 1.0);
}