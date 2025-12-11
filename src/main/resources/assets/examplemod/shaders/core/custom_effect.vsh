#version 150

// Uniforms from Minecraft
uniform mat4 ModelViewMat;
uniform mat4 ProjectionMat;
uniform mat4 TextureMat; // Usually identity for texture atlases
uniform mat4 ColorModulator;
uniform vec4 FogStartEnd;
uniform vec4 FogColor;
uniform float GameTime;

// 【新增 Uniform】 x=u_offset, y=v_offset, z=u_scale, w=v_scale
uniform vec4 CustomUVTransform;

// Input Attributes (from VBO)
in vec3 Position;
in vec2 TexCoord; // VBO中的归一化 UV [0, 1]

// Output to Fragment Shader
out vec2 texCoord;
out float fogDist;

void main() {
    // Standard vertex position calculation
    gl_Position = ProjectionMat * ModelViewMat * vec4(Position, 1.0);

    // 【核心 UV 变换逻辑】
    // 1. 缩放: TexCoord * u_scale/v_scale
    // 2. 平移: + u_offset/v_offset
    texCoord = (TexCoord * CustomUVTransform.zw) + CustomUVTransform.xy;

    // Standard fog calculation
    fogDist = linear_depth(ModelViewMat * vec4(Position, 1.0));
}