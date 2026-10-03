#version 330

// A standing ripple through the frame: the Priest's rite bending what the
// witness is looking at.
//
// Deliberately static. Post passes are handed only SamplerInfo and their own
// config block — no clock reaches them — so an animated wobble would mean one
// compiled chain per phase. A frozen distortion at the right amplitude reads
// as "the air is wrong" perfectly well, and the scene around it is moving.

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
    float reach = Amount * 0.018;
    vec2 uv = texCoord;
    uv.x += sin(texCoord.y * 23.0) * reach;
    uv.y += cos(texCoord.x * 17.0) * reach * 0.75;
    uv += sin((texCoord.yx + texCoord.xy) * 41.0) * reach * 0.35;

    fragColor = vec4(texture(InSampler, clamp(uv, 0.0, 1.0)).rgb, 1.0);
}
