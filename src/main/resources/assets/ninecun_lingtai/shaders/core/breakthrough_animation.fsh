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

const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float MORTAL_TO_QI_RAIN_END = 0.31;
const float MORTAL_TO_QI_RIPPLE_START = 0.20;
const float MORTAL_TO_QI_RIPPLE_DURATION = 0.38;
const float MORTAL_TO_QI_VORTEX_START = 0.36;
const float MORTAL_TO_QI_VORTEX_DURATION = 0.4875;

float hash(float value) {
    return fract(sin(value * 91.173) * 43758.5453);
}

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float lowerLeft = hash(cell);
    float lowerRight = hash(cell + vec2(1.0, 0.0));
    float upperLeft = hash(cell + vec2(0.0, 1.0));
    float upperRight = hash(cell + vec2(1.0, 1.0));
    return mix(
            mix(lowerLeft, lowerRight, local.x),
            mix(upperLeft, upperRight, local.x),
            local.y
    );
}

float cloudNoise(vec2 point) {
    float value = 0.0;
    float amplitude = 0.54;
    mat2 noiseRotation = mat2(0.82, -0.57, 0.57, 0.82);
    for (int octave = 0; octave < 5; octave++) {
        value += noise(point) * amplitude;
        point = noiseRotation * point * 2.03 + vec2(3.1, 1.7);
        amplitude *= 0.5;
    }
    return value;
}

mat2 rotation(float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return mat2(cosine, -sine, sine, cosine);
}

float easeInOut(float value) {
    float clampedValue = clamp(value, 0.0, 1.0);
    return clampedValue * clampedValue * (3.0 - 2.0 * clampedValue);
}

float easeIn(float value) {
    float clampedValue = clamp(value, 0.0, 1.0);
    return clampedValue * clampedValue;
}

float easeOut(float value) {
    float clampedValue = clamp(value, 0.0, 1.0);
    float inverseValue = 1.0 - clampedValue;
    return 1.0 - inverseValue * inverseValue * inverseValue;
}

float easeOutBack(float value) {
    float clampedValue = clamp(value, 0.0, 1.0);
    float inverseValue = clampedValue - 1.0;
    return 1.0 + inverseValue * inverseValue * (
            2.70158 * inverseValue + 1.70158
    );
}

float glow(float distance, float radius) {
    return exp(-max(distance, 0.0) * radius);
}

float softRing(float radius, float target, float width, float softness) {
    float distanceToRing = abs(radius - target);
    return 1.0 - smoothstep(width, width + softness, distanceToRing);
}

float softLine(float value, float width, float softness) {
    return 1.0 - smoothstep(width, width + softness, abs(value));
}

float segmentLine(vec2 point, vec2 startPoint, vec2 endPoint, float width) {
    vec2 direction = endPoint - startPoint;
    float directionLength = max(length(direction), 0.0001);
    float projection = clamp(
            dot(point - startPoint, direction) / (directionLength * directionLength),
            0.0,
            1.0
    );
    float distanceToSegment = length(point - mix(startPoint, endPoint, projection));
    return 1.0 - smoothstep(width, width + width * 1.8, distanceToSegment);
}

vec3 mixLight(vec3 baseColor, float amount) {
    return mix(baseColor, vec3(1.0), clamp(amount, 0.0, 1.0));
}

vec2 cubicBezier(
        vec2 startPoint,
        vec2 firstControl,
        vec2 secondControl,
        vec2 endPoint,
        float progress
) {
    float inverseProgress = 1.0 - progress;
    return inverseProgress * inverseProgress * inverseProgress * startPoint
            + 3.0 * inverseProgress * inverseProgress * progress * firstControl
            + 3.0 * inverseProgress * progress * progress * secondControl
            + progress * progress * progress * endPoint;
}

vec2 dropletContourPoint(float progress) {
    float segment = min(floor(progress * 6.0), 5.0);
    float localProgress = clamp(progress * 6.0 - segment, 0.0, 1.0);

    if (segment < 0.5) {
        return cubicBezier(
                vec2(0.0, -1.0),
                vec2(0.025, -0.92),
                vec2(0.34, -0.64),
                vec2(0.60, -0.20),
                localProgress
        );
    }
    if (segment < 1.5) {
        return cubicBezier(
                vec2(0.60, -0.20),
                vec2(0.72, 0.03),
                vec2(0.76, 0.28),
                vec2(0.68, 0.50),
                localProgress
        );
    }
    if (segment < 2.5) {
        return cubicBezier(
                vec2(0.68, 0.50),
                vec2(0.62, 0.70),
                vec2(0.34, 0.86),
                vec2(0.0, 0.88),
                localProgress
        );
    }
    if (segment < 3.5) {
        return cubicBezier(
                vec2(0.0, 0.88),
                vec2(-0.34, 0.86),
                vec2(-0.62, 0.70),
                vec2(-0.68, 0.50),
                localProgress
        );
    }
    if (segment < 4.5) {
        return cubicBezier(
                vec2(-0.68, 0.50),
                vec2(-0.76, 0.28),
                vec2(-0.72, 0.03),
                vec2(-0.60, -0.20),
                localProgress
        );
    }
    return cubicBezier(
            vec2(-0.60, -0.20),
            vec2(-0.34, -0.64),
            vec2(-0.025, -0.92),
            vec2(0.0, -1.0),
            localProgress
    );
}

float dropletContourDistance(vec2 localPoint) {
    float contourDistance = 10.0;
    vec2 previousPoint = dropletContourPoint(0.0);
    for (int index = 1; index <= 72; index++) {
        float progress = float(index) / 72.0;
        vec2 currentPoint = dropletContourPoint(progress);
        vec2 direction = currentPoint - previousPoint;
        float directionLength = max(dot(direction, direction), 0.00001);
        float projection = clamp(
                dot(localPoint - previousPoint, direction) / directionLength,
                0.0,
                1.0
        );
        contourDistance = min(
                contourDistance,
                length(localPoint - mix(previousPoint, currentPoint, projection))
        );
        previousPoint = currentPoint;
    }
    return contourDistance;
}

float dropletStroke(vec2 point, vec2 center, vec2 scale) {
    vec2 localPoint = (point - center) / scale;
    float contourDistance = dropletContourDistance(localPoint);
    float antiAliasWidth = max(fwidth(contourDistance), 0.0015);
    return 1.0 - smoothstep(
            0.026,
            0.026 + antiAliasWidth * 2.2,
            contourDistance
    );
}

float rippleNoise(vec2 point, float time, float seed) {
    float radius = length(point);
    float angle = atan(point.y, point.x);
    float broadNoise = cloudNoise(vec2(
            angle * (1.75 + seed * 0.11) + time * 0.10 + seed * 2.7,
            radius * 4.4 - time * 0.16 + seed * 3.1
    ));
    float detailNoise = cloudNoise(vec2(
            angle * (7.0 + seed * 0.42) - time * 0.07 + seed,
            radius * 12.0 + time * 0.13
    ));
    return broadNoise * 0.72 + detailNoise * 0.28;
}

float waterRipple(
        vec2 point,
        float targetRadius,
        float width,
        float time,
        float seed
) {
    float radius = length(point);
    float angle = atan(point.y, point.x);
    float localNoise = rippleNoise(point, time, seed);
    float broadWarp = (localNoise - 0.5) * (0.024 + targetRadius * 0.075);
    float slowWarp = sin(angle * 3.0 + time * 0.52 + seed * 1.7)
            * (0.008 + targetRadius * 0.028);
    float distanceToRipple = abs(radius + broadWarp + slowWarp - targetRadius);
    float antiAliasWidth = max(fwidth(distanceToRipple), 0.0012);
    float crest = 1.0 - smoothstep(
            width,
            width + max(antiAliasWidth * 2.0, 0.008),
            distanceToRipple
    );

    float crestNoise = cloudNoise(vec2(
            angle * (3.8 + seed * 0.35) - time * 0.18 + seed * 5.0,
            targetRadius * 9.0 + seed * 2.2
    ));
    float brokenCrest = smoothstep(0.22, 0.68, crestNoise);
    float highlightNoise = cloudNoise(vec2(
            angle * 10.0 + time * 0.12 + seed,
            targetRadius * 4.0 - time * 0.08
    ));
    float highlight = smoothstep(0.46, 0.86, highlightNoise);
    return crest
            * (0.20 + brokenCrest * 0.80)
            * (0.38 + highlight * 0.62);
}

float waterRippleField(vec2 point, float progress, float time) {
    float ripples = 0.0;
    for (int index = 0; index < 8; index++) {
        float indexValue = float(index);
        float delayedProgress = clamp(
                (progress - indexValue * 0.075) / 0.86,
                0.0,
                1.0
        );
        float radius = mix(
                0.018 + indexValue * 0.006,
                0.88 - indexValue * 0.025,
                easeOut(delayedProgress)
        );
        float alive = smoothstep(0.015, 0.12, delayedProgress)
                * (1.0 - smoothstep(0.76, 1.0, delayedProgress));
        float ring = waterRipple(
                point,
                radius,
                0.0075 - indexValue * 0.00035,
                time + indexValue * 0.45,
                indexValue + 0.7
        );
        ripples += ring * alive * (1.0 - indexValue * 0.075);
    }
    return ripples;
}

float vortexEnvelope(float radius, float progress) {
    float innerFade = smoothstep(0.010, 0.075, radius);
    float outerFade = 1.0 - smoothstep(0.78, 1.18, radius);
    float reveal = smoothstep(0.0, 0.72, progress);
    return innerFade * outerFade * reveal;
}

vec2 vortexWarp(vec2 point, float time, float amount) {
    float radius = length(point);
    float angle = atan(point.y, point.x);
    float polarWarp = cloudNoise(vec2(
            angle * 1.55 + time * 0.12,
            radius * 4.6 - time * 0.16
    )) - 0.5;
    float radialWarp = cloudNoise(
            rotation(0.72 + time * 0.08) * point * 3.5
                    + vec2(time * 0.10, -time * 0.13)
    ) - 0.5;
    float centerWeight = 1.0 - smoothstep(0.12, 0.92, radius);
    float warpedAngle = angle
            + amount * (1.0 - smoothstep(0.08, 1.0, radius))
            + polarWarp * 0.56 * (0.25 + centerWeight * 0.75);
    float warpedRadius = radius
            + radialWarp * 0.045 * (0.3 + centerWeight * 0.7);
    return vec2(cos(warpedAngle), sin(warpedAngle)) * warpedRadius;
}

float spiralCloud(
        vec2 point,
        float time,
        float armCount,
        float curl,
        float speed
) {
    float radius = max(length(point), 0.025);
    float angle = atan(point.y, point.x);
    float phase = angle * armCount
            + log(radius) * curl
            - time * speed;
    float phaseNoise = cloudNoise(vec2(
            angle * 1.35 + time * 0.13,
            radius * 5.2 - time * 0.19
    ));
    float broadWave = 0.5 + 0.5 * sin(phase + (phaseNoise - 0.5) * 3.9);
    float detailWave = 0.5 + 0.5 * sin(
            phase * 1.73
            - radius * 16.0
            + phaseNoise * 5.0
            + time * 0.44
    );
    return smoothstep(0.20, 0.86, broadWave)
            * (0.68 + detailWave * 0.32);
}

float condensationPulse(
        float radius,
        float progress,
        float time,
        float offset
) {
    float localProgress = clamp(progress * 1.28 - offset, 0.0, 1.0);
    float contraction = easeInOut(localProgress);
    float pulseRadius = mix(0.82, 0.035, contraction);
    pulseRadius += sin(time * 1.7 + offset * 8.0) * 0.018;
    float pulseWidth = mix(0.090, 0.020, contraction);
    float distanceToPulse = abs(radius - pulseRadius);
    float pulse = exp(-pow(distanceToPulse / max(pulseWidth, 0.001), 2.0));
    float reveal = smoothstep(0.02, 0.16, localProgress);
    return pulse * reveal * (1.0 - smoothstep(0.88, 1.0, localProgress));
}

float condensationStreams(vec2 point, float time, float progress) {
    float radius = length(point);
    float angle = atan(point.y, point.x);
    vec2 warpedPoint = vortexWarp(point, time, 2.6);
    float streamPhase = angle * 5.0
            + log(max(radius, 0.028)) * 7.4
            - time * 1.6;
    float streamNoise = cloudNoise(
            warpedPoint * 5.5 + vec2(time * 0.32, -time * 0.27)
    );
    float streams = smoothstep(
            0.53,
            0.92,
            0.5 + 0.5 * sin(streamPhase + streamNoise * 4.2)
    );
    float radialMask = smoothstep(0.06, 0.88, radius)
            * (1.0 - smoothstep(0.70, 1.04, radius));
    float convergence = smoothstep(0.08, 0.72, progress);
    return streams * radialMask * convergence
            * (0.48 + streamNoise * 0.52);
}

vec3 renderVortex(
        vec2 point,
        float vortexProgress,
        float time,
        out float alpha
) {
    float radius = length(point);
    float formation = smoothstep(0.0, 0.32, vortexProgress);
    float condensation = smoothstep(0.18, 0.92, vortexProgress);
    float envelope = vortexEnvelope(radius, vortexProgress);
    vec2 warpedPoint = vortexWarp(point, time, 2.35 + condensation * 1.55);

    float broadCloud = cloudNoise(
            rotation(0.50 + time * 0.10) * warpedPoint * 2.35
                    + vec2(time * 0.032, -time * 0.026)
    );
    float detailCloud = cloudNoise(
            rotation(-0.90 - time * 0.16) * warpedPoint * 5.7
                    - vec2(time * 0.095, time * 0.074)
    );
    float filamentCloud = cloudNoise(
            rotation(1.80 + time * 0.24) * warpedPoint * 11.0
                    + vec2(time * 0.17, -time * 0.13)
    );

    float primarySpiral = spiralCloud(
            warpedPoint,
            time,
            4.0,
            7.4 + condensation * 2.0,
            1.02
    );
    float secondarySpiral = spiralCloud(
            warpedPoint,
            time * 1.12 + 1.8,
            7.0,
            12.2,
            1.55
    );
    float broadVapor = smoothstep(
            0.20,
            0.76,
            broadCloud * 0.60 + detailCloud * 0.40
    );
    float turbulentVapor = smoothstep(
            0.28,
            0.82,
            broadCloud * 0.32
                    + detailCloud * 0.38
                    + filamentCloud * 0.30
    );
    float armVapor = primarySpiral * (
            0.58
            + broadVapor * 0.34
            + turbulentVapor * 0.22
    );
    float secondaryVapor = secondarySpiral
            * (0.20 + detailCloud * 0.34)
            * (1.0 - smoothstep(0.48, 0.92, radius));
    float inflow = condensationStreams(point, time, vortexProgress);
    float contractionPulse = condensationPulse(
            radius,
            vortexProgress,
            time,
            0.0
    ) + condensationPulse(
            radius,
            vortexProgress,
            time + 1.25,
            0.27
    ) * 0.74;
    float wisps = smoothstep(
            0.46,
            0.90,
            filamentCloud
    ) * (0.35 + primarySpiral * 0.65)
            * smoothstep(0.05, 0.64, radius);

    float coreRadius = mix(0.15, 0.040, condensation);
    float corePulse = 0.82 + 0.18 * sin(time * 5.4 + broadCloud * 4.0);
    float coreMass = exp(-radius / max(coreRadius, 0.001))
            * condensation
            * corePulse;
    float coreHalo = glow(radius, 8.0)
            * condensation
            * (0.34 + turbulentVapor * 0.28);
    float coreRing = softRing(
            radius,
            coreRadius * (1.10 + 0.08 * sin(time * 2.7)),
            0.010,
            0.026
    ) * condensation * (0.45 + corePulse * 0.55);
    float accretionFlash = exp(-pow(
            radius / (0.018 + condensation * 0.012),
            2.0
    )) * smoothstep(0.45, 0.96, vortexProgress)
            * (0.72 + 0.28 * sin(time * 3.6));

    vec3 cloudColor = mix(
            PrimaryColor,
            SecondaryColor,
            clamp(0.24 + primarySpiral * 0.48 + turbulentVapor * 0.22, 0.0, 1.0)
    );
    cloudColor = mix(
            cloudColor,
            mixLight(SecondaryColor, 0.72),
            smoothstep(0.54, 0.92, filamentCloud) * 0.42
    );

    vec3 color = cloudColor * (
            armVapor * 0.88
            + secondaryVapor * 0.46
            + wisps * 0.28
    ) * formation;
    color += SecondaryColor * (
            inflow * 0.62
            + contractionPulse * 0.74
    );
    color += AccentColor * (
            coreMass * 0.88
            + coreHalo * 0.32
            + coreRing * 0.78
            + accretionFlash * 1.45
    );

    alpha = clamp(
            envelope * (
                    armVapor * 0.78
                    + secondaryVapor * 0.28
                    + wisps * 0.28
                    + inflow * 0.50
                    + contractionPulse * 0.70
                    + coreMass * 0.46
                    + coreHalo * 0.20
                    + coreRing * 0.46
                    + accretionFlash * 0.74
            ),
            0.0,
            1.0
    );
    return color;
}

vec3 renderMortalToQi(
        vec2 point,
        float progress,
        float time,
        out float alpha
) {
    float rainProgress = clamp(progress / MORTAL_TO_QI_RAIN_END, 0.0, 1.0);
    float impactProgress = clamp(
            (progress - MORTAL_TO_QI_RIPPLE_START) / MORTAL_TO_QI_RIPPLE_DURATION,
            0.0,
            1.0
    );
    float vortexProgress = clamp(
            (progress - MORTAL_TO_QI_VORTEX_START) / MORTAL_TO_QI_VORTEX_DURATION,
            0.0,
            1.0
    );
    float rainEase = easeInOut(rainProgress);
    float vortexEase = easeInOut(vortexProgress);

    vec3 color = vec3(0.0);
    float mask = 0.0;

    float dropY = mix(-1.46, -0.08, rainEase);
    float dropVisibility = 1.0 - smoothstep(0.78, 0.96, rainProgress);
    vec2 dropCenter = vec2(
            sin(time * 1.7) * 0.008,
            dropY
    );
    vec2 dropScale = vec2(0.20, 0.26);
    float dropMask = dropletStroke(point, dropCenter, dropScale)
            * dropVisibility;
    float dropPulse = 0.88 + 0.12 * sin(time * 3.2);
    color += mix(SecondaryColor, AccentColor, 0.18) * dropMask * dropPulse;
    mask = max(mask, dropMask);

    float impactVisibility = smoothstep(0.0, 0.08, impactProgress)
            * (1.0 - smoothstep(0.86, 1.0, impactProgress));
    float ripples = waterRippleField(point, impactProgress, time);
    float centerFlash = glow(length(point), 34.0)
            * (1.0 - smoothstep(0.0, 0.18, impactProgress))
            * 0.18;
    float impactStrength = impactVisibility * (ripples + centerFlash);
    color += mix(SecondaryColor, AccentColor, 0.12)
            * ripples
            * impactVisibility;
    color += AccentColor * centerFlash * impactVisibility;
    mask = max(mask, impactStrength);

    float vortexFade = 1.0 - smoothstep(0.84, 0.92, progress);
    float vortexAlpha;
    vec3 vortexColor = renderVortex(
            point,
            vortexEase,
            time,
            vortexAlpha
    ) * vortexFade;
    color += vortexColor;
    mask = max(mask, vortexAlpha * vortexFade);

    alpha = clamp(
            mask * (0.76 + 0.24 * smoothstep(0.0, 0.18, progress)),
            0.0,
            1.0
    );
    return color;
}

vec3 renderQiToFoundation(
        vec2 point,
        float progress,
        float time,
        out float alpha
) {
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
    float vortexAngle = angle
            + radius * 19.0
            - time * (1.0 - vortexEase * 0.82);
    float vortexBand = smoothstep(
            0.56,
            0.98,
            sin(vortexAngle * 8.0 + sin(angle * 3.0) * 1.6) * 0.5 + 0.5
    );
    float vortexRing = softRing(
            radius,
            mix(0.70, 0.10, vortexEase),
            0.012,
            0.025
    );
    float vortexMask = (vortexBand * 0.66 + vortexRing * 0.86)
            * (frozenVortex + 0.16 * vortexEase);
    color += mix(PrimaryColor, SecondaryColor, vortexBand * 0.76)
            * vortexMask;
    mask = max(mask, vortexMask);

    float platformScale = mix(0.72, 1.04, platformEase);
    platformScale *= 1.0 + (1.0 - returnEase) * bloomEase * 0.12;
    vec2 platformPoint = point / platformScale;
    float platformRadius = length(platformPoint * vec2(1.0, 1.76));
    float platformEdge = softRing(platformRadius, 0.56, 0.014, 0.024);
    float platformInner = softRing(platformRadius, 0.42, 0.009, 0.020);
    float platformOuter = softRing(platformRadius, 0.73, 0.009, 0.020);
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
    float platformMask = (
            platformEdge * 1.05
            + platformInner * 0.72
            + platformOuter * 0.52
            + platformFace * 0.22
            + platformTop * 0.75
            + spokes * 0.25
            + runes * 0.34
    ) * platformEase;
    vec3 platformColor = mix(SecondaryColor, AccentColor, bloomEase * 0.76);
    color += platformColor * platformMask;
    mask = max(mask, platformMask);

    float bloom = glow(radius, 7.0)
            * bloomEase
            * (1.0 - returnEase * 0.35);
    float flash = glow(radius, 24.0)
            * bloomEase
            * (1.0 - smoothstep(0.52, 0.76, bloomProgress));
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

    vec2 safeScreenSize = max(ScreenSize, vec2(1.0));
    vec2 point = animationUv * 2.0 - 1.0;
    point.x *= safeScreenSize.x / safeScreenSize.y;

    float alpha;
    vec3 color;
    if (AnimationType < 0.5) {
        color = renderMortalToQi(point, progress, Time, alpha);
    } else {
        color = renderQiToFoundation(point, progress, Time, alpha);
    }

    color = mixLight(color, 0.08 + 0.10 * smoothstep(0.34, 0.82, progress));
    alpha *= vertexColor.a * animationFade;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 1.0));
}
