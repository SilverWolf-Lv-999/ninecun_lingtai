#version 150

in vec4 vertexColor;
in vec2 constellationUv;

uniform float SoulTime;
uniform int LightMask;
uniform float IntroProgress;
uniform float BloomProgress;

out vec4 fragColor;

const int STAR_COUNT = 7;
const vec2 STAR_POSITIONS[STAR_COUNT] = vec2[STAR_COUNT](
    vec2(0.235, 0.315),
    vec2(0.300, 0.570),
    vec2(0.470, 0.535),
    vec2(0.515, 0.300),
    vec2(0.645, 0.275),
    vec2(0.760, 0.365),
    vec2(0.875, 0.500)
);

float segmentDistance(vec2 point, vec2 start, vec2 end) {
    vec2 direction = end - start;
    float projection = clamp(dot(point - start, direction) / dot(direction, direction), 0.0, 1.0);
    return length(point - start - direction * projection);
}

bool isIlluminated(int starIndex) {
    return (LightMask & (1 << starIndex)) != 0;
}

float lineGlow(vec2 point, int startIndex, int endIndex, float width) {
    return 1.0 - smoothstep(width, width + 0.0035, segmentDistance(
        point,
        STAR_POSITIONS[startIndex],
        STAR_POSITIONS[endIndex]
    ));
}

void addConnection(
        inout vec3 color,
        inout float alpha,
        vec2 point,
        int startIndex,
        int endIndex,
        float reveal
) {
    float dimLine = lineGlow(point, startIndex, endIndex, 0.0017) * reveal;
    float brightLine = lineGlow(point, startIndex, endIndex, 0.0032) * reveal;
    bool illuminated = isIlluminated(startIndex) && isIlluminated(endIndex);
    vec3 lineColor = illuminated ? vec3(0.49, 0.87, 1.0) : vec3(0.17, 0.25, 0.36);
    float lineStrength = illuminated ? brightLine : dimLine * 0.72;
    color += lineColor * lineStrength * (illuminated ? 0.82 : 0.34);
    alpha = max(alpha, lineStrength * (illuminated ? 0.72 : 0.34));
}

void main() {
    vec2 point = constellationUv;
    vec3 color = vec3(0.0);
    float alpha = 0.0;
    float reveal = smoothstep(0.0, 0.62, IntroProgress);

    addConnection(color, alpha, point, 0, 1, reveal);
    addConnection(color, alpha, point, 1, 2, reveal);
    addConnection(color, alpha, point, 2, 3, reveal);
    addConnection(color, alpha, point, 3, 0, reveal);
    addConnection(color, alpha, point, 3, 4, reveal);
    addConnection(color, alpha, point, 4, 5, reveal);
    addConnection(color, alpha, point, 5, 6, reveal);

    float bloomWave = sin(clamp(BloomProgress, 0.0, 1.0) * 3.14159265);
    float bloomArrival = smoothstep(0.0, 0.24, BloomProgress);
    for (int starIndex = 0; starIndex < STAR_COUNT; starIndex++) {
        float staggerStart = float(starIndex) * 0.07;
        float starReveal = smoothstep(staggerStart, staggerStart + 0.34, IntroProgress);
        vec2 localPoint = point - STAR_POSITIONS[starIndex];
        float distanceToStar = length(localPoint);
        bool illuminated = isIlluminated(starIndex);
        float pulse = 0.84 + 0.16 * sin(SoulTime * 3.1 + float(starIndex) * 1.37);
        float coreRadius = illuminated ? 0.0105 : 0.0065;
        float starCore = 1.0 - smoothstep(coreRadius * 0.36, coreRadius, distanceToStar);
        float haloRadius = illuminated ? 0.056 : 0.024;
        float halo = (1.0 - smoothstep(0.0, haloRadius, distanceToStar));
        halo *= halo;

        vec3 starColor = illuminated ? vec3(0.76, 0.95, 1.0) : vec3(0.27, 0.34, 0.45);
        float starStrength = starReveal * (starCore + halo * (illuminated ? 0.72 * pulse : 0.18));
        color += starColor * starStrength;
        alpha = max(alpha, starReveal * (starCore + halo * (illuminated ? 0.62 : 0.16)));

        if (illuminated && BloomProgress > 0.0) {
            float angle = SoulTime * 0.35 + float(starIndex) * 0.83;
            float cosine = cos(angle);
            float sine = sin(angle);
            vec2 rotatedPoint = vec2(
                localPoint.x * cosine - localPoint.y * sine,
                localPoint.x * sine + localPoint.y * cosine
            );
            float horizontalRay = exp(-abs(rotatedPoint.y) * 150.0)
                    * (1.0 - smoothstep(0.02, 0.22, abs(rotatedPoint.x)));
            float verticalRay = exp(-abs(rotatedPoint.x) * 150.0)
                    * (1.0 - smoothstep(0.02, 0.22, abs(rotatedPoint.y)));
            float bloomHalo = 1.0 - smoothstep(0.0, 0.18 + bloomWave * 0.08, distanceToStar);
            float burst = (horizontalRay + verticalRay) * bloomWave + bloomHalo * bloomArrival * 0.34;
            color += vec3(0.70, 0.93, 1.0) * burst * 1.35;
            alpha = max(alpha, burst * 0.72);
        }
    }

    vec2 centeredPoint = point * 2.0 - 1.0;
    float vignette = 1.0 - smoothstep(0.72, 1.35, length(centeredPoint));
    float finalAlpha = alpha * vertexColor.a * vignette;
    if (finalAlpha <= 0.002) {
        discard;
    }
    fragColor = vec4(color * vertexColor.rgb, finalAlpha);
}
