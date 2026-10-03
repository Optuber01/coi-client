#version 330

// Mosaic plus colour quantisation — the frame coming apart into blocks.
// Both the block size and the number of levels ride Amount, so a low
// intensity is a faint stair-stepping rather than a different effect.

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
    float mosaic = max(1.0, mix(1.0, 7.0, Amount));
    float levels = mix(64.0, 6.0, Amount);

    vec2 blocks = InSize / mosaic;
    vec2 snapped = texCoord - fract(texCoord * blocks) / blocks;

    vec3 src = texture(InSampler, texCoord).rgb;
    vec3 bits = texture(InSampler, snapped).rgb;
    bits -= fract(bits * levels) / levels;

    fragColor = vec4(mix(src, bits, Amount), 1.0);
}
