#version 150

in vec4 vertexColor;
in vec2 animationUv;

uniform float AnimationType;
uniform float Progress;
uniform float Time;
uniform vec2 ScreenSize;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;
uniform vec3 AccentColor;

out vec4 fragColor;

float hash(float value) {
    return fract(sin(value * 91.173) * 43758.5453);
}

float softLine(float value, float width, float softness) {
    return 1.0 - smoothstep(width, width + softness, abs(value));
}

float ring(float radius, float target, float width) {
    return softLine(radius - target, width, 0.012);
}

float easeInOut(float value) {
    float clampedValue = clamp(value, 0.0, 1.0);
    return clampedValue * clampedValue * (3.0 - 2.0 * clampedValue);
}

float easeOut(float value) {
    float clampedValue = clamp(value, 0.0, 1.0);
    float inverseValue = 1.0 - clampedValue;
    return 1.0 - inverseValue * inverseValue * inverseValue;
}

float glow(float distance, float radius) {
    return exp(-max(distance, 0.0) * radius);
}

vec3 mixLight(vec3 baseColor, float amount) {
    return mix(baseColor, vec3(1.0), clamp(amount, 0.0, 1.0));
}

vec3 renderMortalToQi(vec2 point, float progress, float time, out float alpha) {
    float rainProgress = clamp(progress / 0.29, 0.0, 1.0);
    float impactProgress = clamp((progress - 0.18) / 0.38, 0.0, 1.0);
    float vortexProgress = clamp((progress - 0.34) / 0.58, 0.0, 1.0);
    float rainEase = easeInOut(rainProgress);
    float impactEase = easeOut(impactProgress);
    float vortexEase = easeInOut(vortexProgress);

    vec3 color = vec3(0.0);
    float mask = 0.0;

    float dropY = -1.10 + rainEase * 1.10;
    vec2 dropPoint = point - vec2(0.0, dropY);
    float dropDistance = length(dropPoint * vec2(1.0, 0.62));
    float dropMask = glow(dropDistance, 42.0) * (1.0 - smoothstep(0.10, 0.19, rainProgress));
    float dropTail = softLine(
            point.x + sin(point.y * 38.0 + time * 6.0) * 0.006,
            0.008,
            0.012
    ) * smoothstep(-0.95, -0.10, point.y) * (1.0 - rainProgress);
    color += mixLight(SecondaryColor, 0.46) * (dropMask * 1.8 + dropTail * 0.44);
    mask = max(mask, dropMask + dropTail * 0.44);

    float impactRadius = mix(0.015, 0.78, impactEase);
    float impactRing = ring(length(point), impactRadius, 0.012 + impactProgress * 0.01);
    float impactRing2 = ring(length(point), impactRadius * 0.72, 0.008);
    float splash = smoothstep(0.22, 0.37, impactProgress)
            * (1.0 - smoothstep(0.70, 1.0, impactProgress));
    float angle = atan(point.y, point.x);
    float splashLines = smoothstep(0.68, 0.96, sin(angle * 16.0 + time * 1.4) * 0.5 + 0.5)
            * ring(length(point), impactRadius * 0.86, 0.014)
            * splash;
    float impactFlash = glow(length(point), 18.0) * (1.0 - smoothstep(0.0, 0.16, impactProgress));
    color += SecondaryColor * (impactRing * 1.05 + impactRing2 * 0.5 + splashLines * 0.7);
    color += AccentColor * impactFlash * 1.6;
    mask = max(mask, impactRing + impactRing2 * 0.55 + splashLines * 0.7 + impactFlash);

    float radius = length(point);
    float vortexEnvelope = smoothstep(0.0, 0.24, vortexProgress)
            * (1.0 - smoothstep(0.78, 1.0, progress));
    float vortexAngle = angle + 2.2 * (1.0 - smoothstep(0.0, 0.8, radius))
            + time * (0.72 + vortexEase * 0.35);
    float vortexBand = smoothstep(
            0.52,
            0.98,
            sin(vortexAngle * 7.0 + radius * 22.0 - vortexEase * 12.0) * 0.5 + 0.5
    );
    float vortexRings = ring(radius, 0.20 + 0.48 * fract(radius * 2.1 + time * 0.07), 0.008);
    float vortexCore = glow(radius, 16.0) * vortexEnvelope;
    float vortexMask = (vortexBand * 0.62 + vortexRings * 0.9 + vortexCore * 0.42)
            * vortexEnvelope;
    color += mix(PrimaryColor, SecondaryColor, vortexBand * 0.72)
            * (vortexMask * 0.78);
    color += AccentColor * vortexCore * 0.72;
    mask = max(mask, vortexMask);

    alpha = clamp(mask * (0.72 + 0.28 * smoothstep(0.0, 0.16, progress)), 0.0, 1.0);
    return color;
}

vec3 renderQiToFoundation(vec2 point, float progress, float time, out float alpha) {
    float vortexProgress = clamp(progress / 0.42, 0.0, 1.0);
    float platformProgress = clamp((progress - 0.24) / 0.30, 0.0, 1.0);
    float bloomProgress = clamp((progress - 0.48) / 0.22, 0.0, 1.0);
    float returnProgress = clamp((progress - 0.70) / 0.24, 0.0, 1.0);
    float vortexEase = easeInOut(vortexProgress);
    float platformEase = easeOut(platformProgress);
    float bloomEase = easeInOut(bloomProgress);
    float returnEase = easeInOut(returnProgress);

    vec3 color = vec3(0.0);
    float mask = 0.0;
    float radius = length(point);
    float angle = atan(point.y, point.x);

    float frozenVortex = smoothstep(0.04, 0.28, vortexProgress)
            * (1.0 - smoothstep(0.34, 0.58, progress));
    float vortexAngle = angle + radius * 19.0 - time * (1.0 - vortexEase * 0.82);
    float vortexBand = smoothstep(
            0.56,
            0.98,
            sin(vortexAngle * 8.0 + sin(angle * 3.0) * 1.6) * 0.5 + 0.5
    );
    float vortexRing = ring(radius, mix(0.70, 0.10, vortexEase), 0.012);
    float vortexMask = (vortexBand * 0.66 + vortexRing * 0.86)
            * (frozenVortex + 0.16 * vortexEase);
    color += mix(PrimaryColor, SecondaryColor, vortexBand * 0.76) * vortexMask;
    mask = max(mask, vortexMask);

    float platformScale = mix(0.72, 1.04, platformEase);
    platformScale *= 1.0 + (1.0 - returnEase) * bloomEase * 0.12;
    vec2 platformPoint = point / platformScale;
    float platformRadius = length(platformPoint * vec2(1.0, 1.76));
    float platformEdge = ring(platformRadius, 0.56, 0.014);
    float platformInner = ring(platformRadius, 0.42, 0.009);
    float platformOuter = ring(platformRadius, 0.73, 0.009);
    float platformFace = (1.0 - smoothstep(0.45, 0.62, platformRadius))
            * smoothstep(0.05, 0.28, platformProgress);
    float platformTop = softLine(platformPoint.y, 0.026, 0.018)
            * smoothstep(0.18, 0.48, abs(platformPoint.x))
            * (1.0 - smoothstep(0.48, 0.68, abs(platformPoint.x)));
    float spokes = (1.0 - smoothstep(0.0, 0.022, abs(sin(angle * 8.0))))
            * smoothstep(0.24, 0.44, platformRadius)
            * (1.0 - smoothstep(0.64, 0.76, platformRadius));
    float runes = smoothstep(
            0.54,
            0.92,
            sin(angle * 18.0 + time * 0.18) * 0.5 + 0.5
    ) * platformEdge;
    float platformMask = (platformEdge * 1.05 + platformInner * 0.72 + platformOuter * 0.52
            + platformFace * 0.22 + platformTop * 0.75 + spokes * 0.25 + runes * 0.34)
            * platformEase;
    vec3 platformColor = mix(SecondaryColor, AccentColor, bloomEase * 0.76);
    color += platformColor * platformMask;
    mask = max(mask, platformMask);

    float bloom = glow(radius, 7.0) * bloomEase * (1.0 - returnEase * 0.35);
    float flash = glow(radius, 24.0) * bloomEase * (1.0 - smoothstep(0.52, 0.76, bloomProgress));
    color += AccentColor * (bloom * 0.62 + flash * 1.6);
    mask = max(mask, bloom * 0.62 + flash);

    float returnGlow = (1.0 - returnEase) * bloomEase;
    alpha = clamp(mask * (0.76 + returnGlow * 0.24), 0.0, 1.0);
    return color;
}

void main() {
    float progress = clamp(Progress, 0.0, 1.0);
    float fadeIn = smoothstep(0.0, 0.055, progress);
    float fadeOut = 1.0 - smoothstep(0.86, 1.0, progress);
    float animationFade = fadeIn * fadeOut;

    vec2 point = animationUv * 2.0 - 1.0;
    point.x *= ScreenSize.x / max(ScreenSize.y, 1.0);

    float alpha;
    vec3 color;
    if (AnimationType < 0.5) {
        color = renderMortalToQi(point, progress, Time, alpha);
    } else {
        color = renderQiToFoundation(point, progress, Time, alpha);
    }

    color = mixLight(color, 0.10 + 0.10 * smoothstep(0.35, 0.8, progress));
    alpha *= vertexColor.a * animationFade;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 1.0));
}
