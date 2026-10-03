#version 330

// Drains the colour out of the frame, Amount of the way. The Darkness
// ascension's signature: the world keeps every shape and loses every hue.

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

void main() {
    vec3 src = texture(InSampler, texCoord).rgb;
    float grey = dot(src, Luma);
    fragColor = vec4(mix(src, vec3(grey), Amount), 1.0);
}
