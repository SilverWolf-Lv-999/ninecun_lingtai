#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float BlurRadius;
uniform float Distortion;
uniform float ChromaticAberration;

out vec4 fragColor;

const int MAX_BLUR_RADIUS = 10;

vec2 distort(vec2 uv) {
    vec2 centered = uv * 2.0 - 1.0;
    float radius = dot(centered, centered);
    float wave = sin(centered.y * 31.0 + centered.x * 17.0) * 0.5
            + sin(centered.x * 47.0 - centered.y * 23.0) * 0.5;
    centered += centered * radius * Distortion * wave;
    return centered * 0.5 + 0.5;
}

void main() {
    vec2 baseUv = clamp(distort(texCoord), 0.003, 0.997);
    vec2 centered = texCoord * 2.0 - 1.0;
    vec2 radial = normalize(centered + vec2(0.0001));
    float blurRadius = clamp(BlurRadius, 0.0, float(MAX_BLUR_RADIUS));
    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    for (int index = -MAX_BLUR_RADIUS; index <= MAX_BLUR_RADIUS; index++) {
        float offset = float(index);
        if (abs(offset) > blurRadius) {
            continue;
        }
        float weight = 1.0 - abs(offset) / (blurRadius + 1.0);
        vec2 sampleUv = clamp(baseUv + radial * offset * oneTexel * 1.8, 0.003, 0.997);
        color += texture(DiffuseSampler, sampleUv) * weight;
        totalWeight += weight;
    }

    vec2 aberration = radial * ChromaticAberration;
    float red = texture(DiffuseSampler, clamp(baseUv + aberration, 0.003, 0.997)).r;
    float blue = texture(DiffuseSampler, clamp(baseUv - aberration, 0.003, 0.997)).b;
    vec4 blurred = color / max(totalWeight, 0.0001);
    fragColor = vec4(red, blurred.g, blue, 1.0);
}
