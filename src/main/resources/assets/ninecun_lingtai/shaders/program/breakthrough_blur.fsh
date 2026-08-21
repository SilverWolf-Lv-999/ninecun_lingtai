#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float BlurRadius;
uniform float Sigma;
uniform float Opacity;

out vec4 fragColor;

const int MAX_BLUR_RADIUS = 12;

void main() {
    vec2 baseUv = clamp(texCoord, 0.003, 0.997);
    float blurRadius = clamp(BlurRadius, 0.0, float(MAX_BLUR_RADIUS));
    float sigma = max(Sigma, 0.001);
    float twoSigmaSquared = 2.0 * sigma * sigma;
    vec4 blurredColor = vec4(0.0);
    float totalWeight = 0.0;

    for (int offsetX = -MAX_BLUR_RADIUS; offsetX <= MAX_BLUR_RADIUS; offsetX++) {
        for (int offsetY = -MAX_BLUR_RADIUS; offsetY <= MAX_BLUR_RADIUS; offsetY++) {
            vec2 offset = vec2(float(offsetX), float(offsetY));
            if (abs(offset.x) > blurRadius || abs(offset.y) > blurRadius) {
                continue;
            }

            float weight = exp(-dot(offset, offset) / twoSigmaSquared);
            vec2 sampleUv = clamp(baseUv + offset * oneTexel, 0.003, 0.997);
            blurredColor += texture(DiffuseSampler, sampleUv) * weight;
            totalWeight += weight;
        }
    }

    vec4 originalColor = texture(DiffuseSampler, baseUv);
    vec4 blurColor = vec4(blurredColor.rgb / max(totalWeight, 0.0001), originalColor.a);
    fragColor = vec4(mix(originalColor.rgb, blurColor.rgb, clamp(Opacity, 0.0, 1.0)), 1.0);
}
