#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;

uniform float Speed;
uniform float Amplitude;
uniform float Frequency;
uniform float CenterX;
uniform float CenterY;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 Center = vec2(CenterX, CenterY);
    vec2 uv = texCoord;
    float aspectRatio = OutSize.x / OutSize.y;
    vec2 vecToCenter = uv - Center;
    vecToCenter.x *= aspectRatio;
    float dist = length(vecToCenter);
    float rippleTime = Time * Speed;
    float displacement = 0.0;
    float waveWidth = 0.1;

    if (dist > rippleTime - waveWidth && dist < rippleTime) {
        displacement = sin((dist - rippleTime) * Frequency) * Amplitude;
    }

    vec2 direction = normalize(uv - Center);
    vec2 distortedUV = uv + direction * displacement;

    fragColor = texture(DiffuseSampler, distortedUV);
}