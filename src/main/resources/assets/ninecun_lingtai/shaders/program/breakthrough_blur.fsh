#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float BlurRadius;
uniform float Sigma;
uniform float Opacity;
uniform float ShakeStrength;
uniform float ShakeTime;

out vec4 fragColor;

const int MAX_BLUR_RADIUS = 12;

mat2 rotation(float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return mat2(cosine, -sine, sine, cosine);
}

vec2 screenShakeUv(vec2 uv) {
    float strength = clamp(ShakeStrength, 0.0, 1.0);
    vec2 centered = uv * 2.0 - 1.0;
    vec2 shakePixels = vec2(
            sin(ShakeTime * 63.0 + 0.8) + sin(ShakeTime * 97.0 + 2.1) * 0.42,
            cos(ShakeTime * 71.0 + 1.4) + cos(ShakeTime * 113.0 + 0.3) * 0.36
    ) * (7.0 + 9.0 * strength) * strength;
    centered -= shakePixels * oneTexel;
    float shakeAngle = (
            sin(ShakeTime * 59.0 + 1.2) * 0.004
            + sin(ShakeTime * 89.0 + 0.6) * 0.0025
    ) * strength;
    centered = rotation(shakeAngle) * centered;
    return centered * 0.5 + 0.5;
}

void main() {
    vec2 baseUv = clamp(screenShakeUv(texCoord), 0.003, 0.997);
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
