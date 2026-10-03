#version 330

// Luminance edge detection, laid back over the frame. Vanilla's own sobel
// pass reads the alpha of the entity-outline target and is useless on a
// fully opaque main target, so this one differentiates brightness instead.

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

const vec3 Luma = vec3(0.2126, 0.7152, 0.0722);

float brightness(vec2 uv) {
    return dot(texture(InSampler, uv).rgb, Luma);
}

void main() {
    vec2 texel = 1.0 / InSize;

    float tl = brightness(texCoord + texel * vec2(-1.0, -1.0));
    float tc = brightness(texCoord + texel * vec2( 0.0, -1.0));
    float tr = brightness(texCoord + texel * vec2( 1.0, -1.0));
    float ml = brightness(texCoord + texel * vec2(-1.0,  0.0));
    float mr = brightness(texCoord + texel * vec2( 1.0,  0.0));
    float bl = brightness(texCoord + texel * vec2(-1.0,  1.0));
    float bc = brightness(texCoord + texel * vec2( 0.0,  1.0));
    float br = brightness(texCoord + texel * vec2( 1.0,  1.0));

    float gx = (tr + 2.0 * mr + br) - (tl + 2.0 * ml + bl);
    float gy = (bl + 2.0 * bc + br) - (tl + 2.0 * tc + tr);
    float edge = clamp(sqrt(gx * gx + gy * gy), 0.0, 1.0);

    vec3 src = texture(InSampler, texCoord).rgb;
    // Edges keep the colour they were drawn from, everything else goes dark,
    // so the world reads as a line drawing of itself rather than as noise
    vec3 drawn = src * edge;
    fragColor = vec4(mix(src, drawn, Amount), 1.0);
}
