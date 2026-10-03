#version 330

// The Error's negative. A straight mix toward 1 - c, so the pass can be
// dialled in rather than slammed on.

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CeremonyConfig {
    float Amount;
};

out vec4 fragColor;

void main() {
    vec3 src = texture(InSampler, texCoord).rgb;
    fragColor = vec4(mix(src, 1.0 - src, Amount), 1.0);
}
