#version 150

in vec4 vertexColor;
in vec2 animationUv;

uniform float AnimationType;
uniform float Progress;
uniform float Time;
uniform float ShakeStrength;
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
const float QI_TO_FOUNDATION_VORTEX_DURATION = 0.54;
const float QI_TO_FOUNDATION_PLATFORM_REVEAL_START = 0.24;
const float QI_TO_FOUNDATION_PLATFORM_REVEAL_END = 0.46;
const float QI_TO_FOUNDATION_TABLET_REVEAL_START = 0.34;
const float QI_TO_FOUNDATION_TABLET_REVEAL_END = 0.58;
const float QI_TO_FOUNDATION_BLOOM_START = 0.46;
const float QI_TO_FOUNDATION_BLOOM_END = 0.80;
const float FOUNDATION_TO_GOLDEN_CORE_TREMBLE_END = 0.26666668;
const float FOUNDATION_TO_GOLDEN_CORE_BREAK_START = 0.26666668;
const float FOUNDATION_TO_GOLDEN_CORE_BREAK_END = 0.32000000;
const float FOUNDATION_TO_GOLDEN_CORE_VIOLENT_SHAKE_END = 0.40000000;
const float FOUNDATION_TO_GOLDEN_CORE_SETTLE_END = 0.60000000;
const float FOUNDATION_TO_GOLDEN_CORE_GATHER_START = 0.60000000;
const float FOUNDATION_TO_GOLDEN_CORE_GATHER_END = 0.84000000;
const float FOUNDATION_TO_GOLDEN_CORE_CORE_REVEAL_START = 0.72000000;
const float FOUNDATION_TO_GOLDEN_CORE_CORE_REVEAL_END = 0.84000000;
const float FOUNDATION_TO_GOLDEN_CORE_FLOW_START = 0.76000000;
const float FOUNDATION_TO_GOLDEN_CORE_FLOW_END = 0.84000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_TREMBLE_END = 0.20000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START = 0.20000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_END = 0.27000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_VIOLENT_SHAKE_END = 0.36500000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_SETTLE_END = 0.50000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_LOTUS_REVEAL_START = 0.28000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_LOTUS_REVEAL_END = 0.57000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_BABY_REVEAL_START = 0.45500000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_BABY_REVEAL_END = 0.76000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_START = 0.62000000;
const float GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_END = 0.82000000;

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

float tabletBand(float value, float lower, float upper, float softness) {
    return smoothstep(lower - softness, lower + softness, value)
            * (1.0 - smoothstep(upper - softness, upper + softness, value));
}

float tabletShape(vec2 point) {
    float body = tabletBand(point.y, -0.132, 0.138, 0.008)
            * (1.0 - smoothstep(0.092, 0.105, abs(point.x)));

    float topProgress = clamp((point.y + 0.205) / 0.080, 0.0, 1.0);
    float topWidth = 0.082 * topProgress;
    float top = tabletBand(point.y, -0.205, -0.118, 0.006)
            * (1.0 - smoothstep(topWidth, topWidth + 0.010, abs(point.x)));

    float bottomProgress = clamp((point.y - 0.135) / 0.040, 0.0, 1.0);
    float bottomWidth = mix(0.104, 0.064, bottomProgress);
    float bottom = tabletBand(point.y, 0.132, 0.175, 0.006)
            * (1.0 - smoothstep(bottomWidth, bottomWidth + 0.010, abs(point.x)));
    return max(body, max(top, bottom));
}

float tabletEdge(vec2 point) {
    vec2 vertex0 = vec2(0.0, -0.205);
    vec2 vertex1 = vec2(0.082, -0.118);
    vec2 vertex2 = vec2(0.104, 0.135);
    vec2 vertex3 = vec2(0.064, 0.175);
    vec2 vertex4 = vec2(-0.064, 0.175);
    vec2 vertex5 = vec2(-0.104, 0.135);
    vec2 vertex6 = vec2(-0.082, -0.118);
    float edge = 0.0;
    edge = max(edge, segmentLine(point, vertex0, vertex1, 0.006));
    edge = max(edge, segmentLine(point, vertex1, vertex2, 0.006));
    edge = max(edge, segmentLine(point, vertex2, vertex3, 0.006));
    edge = max(edge, segmentLine(point, vertex3, vertex4, 0.006));
    edge = max(edge, segmentLine(point, vertex4, vertex5, 0.006));
    edge = max(edge, segmentLine(point, vertex5, vertex6, 0.006));
    edge = max(edge, segmentLine(point, vertex6, vertex0, 0.006));
    return edge;
}

float platformTablet(
        vec2 point,
        float tabletAngle,
        float tabletRadius,
        float reveal,
        float time,
        out float sideLayer,
        out float faceLayer,
        out float edgeLayer,
        out float glyph
) {
    vec2 radialPoint = rotation(-tabletAngle) * point;
    float bob = sin(time * 1.15 + tabletAngle * 2.0) * 0.006;
    vec2 tabletPoint = (radialPoint - vec2(0.0, tabletRadius + bob)) * 1.16;

    vec2 lowerPoint = tabletPoint + vec2(0.0, 0.030);
    float face = tabletShape(tabletPoint);
    float side = tabletShape(lowerPoint) * (1.0 - face * 0.94);
    float underEdge = tabletEdge(lowerPoint) * (1.0 - face * 0.72);
    float edge = tabletEdge(tabletPoint);
    float insetFace = tabletShape(tabletPoint / vec2(0.86, 0.80));
    float insetEdge = tabletEdge(tabletPoint / vec2(0.86, 0.80));

    float centralSpine = segmentLine(
            tabletPoint,
            vec2(0.0, -0.135),
            vec2(0.0, 0.125),
            0.005
    );
    float upperDiamond = segmentLine(
            tabletPoint,
            vec2(-0.040, -0.080),
            vec2(0.0, -0.120),
            0.005
    ) + segmentLine(
            tabletPoint,
            vec2(0.0, -0.120),
            vec2(0.040, -0.080),
            0.005
    );
    float lowerDiamond = segmentLine(
            tabletPoint,
            vec2(-0.044, 0.055),
            vec2(0.0, 0.092),
            0.005
    ) + segmentLine(
            tabletPoint,
            vec2(0.0, 0.092),
            vec2(0.044, 0.055),
            0.005
    );
    float sideMarks = segmentLine(
            tabletPoint,
            vec2(-0.054, -0.012),
            vec2(-0.026, 0.018),
            0.004
    ) + segmentLine(
            tabletPoint,
            vec2(0.054, -0.012),
            vec2(0.026, 0.018),
            0.004
    );
    float runePoint = glow(length(tabletPoint - vec2(0.0, -0.145)), 42.0);
    glyph = (
            centralSpine * 0.72
            + upperDiamond * 0.88
            + lowerDiamond * 0.84
            + sideMarks * 0.78
            + runePoint * 0.26
    ) * insetFace;

    sideLayer = (side * 0.86 + underEdge * 0.42) * reveal;
    faceLayer = (face * 0.88 + insetFace * 0.12) * reveal;
    edgeLayer = (edge * 1.10 + insetEdge * 0.38) * reveal;
    glyph *= reveal;
    return max(max(sideLayer, faceLayer), max(edgeLayer, glyph));
}

float platformArc(
        float radius,
        float angle,
        float targetRadius,
        float width,
        float phase,
        float segmentCount
) {
    float ring = softRing(radius, targetRadius, width, 0.022);
    float segments = smoothstep(
            0.30,
            0.92,
            0.5 + 0.5 * sin(angle * segmentCount + phase)
    );
    return ring * (0.20 + segments * 0.80);
}

float platformSurfaceSigils(vec2 point, float time) {
    float sigils = 0.0;
    for (int index = 0; index < 4; index++) {
        float indexValue = float(index);
        float sigilAngle = indexValue * PI * 0.5 + PI * 0.25;
        vec2 sigilPoint = rotation(-sigilAngle) * point - vec2(0.0, 0.34);
        sigils += segmentLine(
                sigilPoint,
                vec2(-0.075, 0.028),
                vec2(0.0, -0.040),
                0.005
        ) + segmentLine(
                sigilPoint,
                vec2(0.0, -0.040),
                vec2(0.075, 0.028),
                0.005
        ) + segmentLine(
                sigilPoint,
                vec2(-0.042, 0.050),
                vec2(0.042, 0.050),
                0.004
        );
    }
    return sigils * (0.72 + 0.28 * sin(time * 0.8));
}

void blendLayer(
        inout vec3 baseColor,
        inout float baseAlpha,
        vec3 layerColor,
        float layerAlpha
) {
    float clampedAlpha = clamp(layerAlpha, 0.0, 1.0);
    baseColor = mix(baseColor, layerColor, clampedAlpha);
    baseAlpha = clampedAlpha + baseAlpha * (1.0 - clampedAlpha);
}

vec3 renderQiToFoundation(
        vec2 point,
        float progress,
        float time,
        out float alpha
) {
    float vortexProgress = clamp(
            progress / QI_TO_FOUNDATION_VORTEX_DURATION,
            0.0,
            1.0
    );
    float vortexEase = easeInOut(vortexProgress);
    float platformReveal = smoothstep(
            QI_TO_FOUNDATION_PLATFORM_REVEAL_START,
            QI_TO_FOUNDATION_PLATFORM_REVEAL_END,
            progress
    );
    float tabletReveal = smoothstep(
            QI_TO_FOUNDATION_TABLET_REVEAL_START,
            QI_TO_FOUNDATION_TABLET_REVEAL_END,
            progress
    );
    float bloomIn = smoothstep(
            QI_TO_FOUNDATION_BLOOM_START,
            0.60,
            progress
    );
    float bloomOut = 1.0 - smoothstep(0.66, QI_TO_FOUNDATION_BLOOM_END, progress);
    float bloomPulse = bloomIn * bloomOut;
    float platformReturn = smoothstep(0.68, 0.88, progress);
    float platformScale = mix(0.68, 0.86, easeOut(platformReveal));
    platformScale *= 1.0 + bloomPulse * 0.12;
    platformScale = mix(platformScale, 0.79, platformReturn * 0.72);
    float platformFade = smoothstep(0.18, 0.30, progress);
    float vortexFade = 1.0 - smoothstep(0.38, 0.58, progress);

    vec3 sideColor = mix(PrimaryColor, vec3(0.0), 0.28);
    vec3 faceColor = mix(PrimaryColor, SecondaryColor, 0.28);
    vec3 edgeColor = mix(SecondaryColor, AccentColor, 0.14);
    vec3 glyphColor = AccentColor;
    vec3 color = vec3(0.0);
    float colorAlpha = 0.0;

    float vortexAlpha;
    vec3 vortexColor = renderVortex(
            point,
            vortexEase,
            time * 0.92,
            vortexAlpha
    ) * vortexFade;
    blendLayer(color, colorAlpha, vortexColor, vortexAlpha * vortexFade * 0.76);

    vec2 platformCenter = vec2(0.0, 0.04);
    vec2 platformPoint = (point - platformCenter)
            / vec2(platformScale, platformScale * 0.46);
    float platformRadius = length(platformPoint);
    float platformAngle = atan(platformPoint.y, platformPoint.x);
    float face = 1.0 - smoothstep(0.90, 1.0, platformRadius);
    float lowerRadius = length(
            (point - platformCenter - vec2(0.0, 0.070))
                    / vec2(platformScale * 1.015, platformScale * 0.46)
    );
    float lowerFace = 1.0 - smoothstep(0.90, 1.0, lowerRadius);
    float lowerHalf = smoothstep(-0.12, 0.66, platformPoint.y);
    float side = lowerFace * (1.0 - face * 0.96) * lowerHalf;

    float outerEdge = softRing(platformRadius, 0.935, 0.018, 0.030);
    float middleEdge = softRing(platformRadius, 0.690, 0.012, 0.024);
    float innerEdge = softRing(platformRadius, 0.455, 0.010, 0.022);
    float coreRing = softRing(platformRadius, 0.220, 0.009, 0.020);
    float outerArc = platformArc(
            platformRadius,
            platformAngle,
            0.82,
            0.010,
            time * 0.42,
            8.0
    );
    float middleArc = platformArc(
            platformRadius,
            platformAngle,
            0.61,
            0.008,
            -time * 0.28 + 1.2,
            12.0
    );
    float innerArc = platformArc(
            platformRadius,
            platformAngle,
            0.36,
            0.006,
            time * 0.34 + 2.5,
            8.0
    );
    float surfaceSigils = platformSurfaceSigils(platformPoint, time);
    float surfaceLight = 0.74 + 0.16 * cloudNoise(
            platformPoint * 3.2 + vec2(time * 0.05, -time * 0.03)
    );
    float platformLayer = platformReveal * platformFade;

    float tabletRadius = mix(0.45, 0.72, easeOut(tabletReveal));
    float tabletLayer = tabletReveal * platformFade;

    for (int index = 0; index < 8; index++) {
        float tabletAngle = float(index) / 8.0 * TAU + PI * 0.125;
        float tabletDepth = -cos(tabletAngle);
        if (tabletDepth < 0.0) {
            float tabletSide;
            float tabletFace;
            float tabletEdge;
            float tabletGlyph;
            platformTablet(
                    platformPoint,
                    tabletAngle,
                    tabletRadius,
                    tabletLayer,
                    time,
                    tabletSide,
                    tabletFace,
                    tabletEdge,
                    tabletGlyph
            );
            blendLayer(color, colorAlpha, sideColor, tabletSide * 0.38);
            blendLayer(color, colorAlpha, faceColor, tabletFace * 0.42);
            blendLayer(color, colorAlpha, edgeColor, tabletEdge * 0.72);
            blendLayer(color, colorAlpha, glyphColor, tabletGlyph * 0.68);
        }
    }

    blendLayer(color, colorAlpha, sideColor, side * platformLayer * 0.72);
    blendLayer(
            color,
            colorAlpha,
            mix(sideColor, faceColor, 0.32),
            side * platformLayer * 0.20
    );
    blendLayer(color, colorAlpha, faceColor, face * platformLayer * 0.42);
    blendLayer(
            color,
            colorAlpha,
            mix(faceColor, SecondaryColor, 0.28),
            face * platformLayer * surfaceLight * 0.12
    );
    blendLayer(
            color,
            colorAlpha,
            edgeColor,
            (outerEdge * 0.84 + middleEdge * 0.42)
                    * platformLayer
                    * 0.76
    );
    blendLayer(
            color,
            colorAlpha,
            glyphColor,
            (innerEdge * 0.52 + coreRing * 0.62
                    + outerArc * 0.52 + middleArc * 0.40 + innerArc * 0.30
                    + surfaceSigils * 0.52)
                    * platformLayer
                    * (0.62 + bloomPulse * 0.38)
    );

    for (int index = 0; index < 8; index++) {
        float tabletAngle = float(index) / 8.0 * TAU + PI * 0.125;
        float tabletDepth = -cos(tabletAngle);
        if (tabletDepth >= 0.0) {
            float tabletSide;
            float tabletFace;
            float tabletEdge;
            float tabletGlyph;
            platformTablet(
                    platformPoint,
                    tabletAngle,
                    tabletRadius,
                    tabletLayer,
                    time,
                    tabletSide,
                    tabletFace,
                    tabletEdge,
                    tabletGlyph
            );
            blendLayer(color, colorAlpha, sideColor, tabletSide * 0.44);
            blendLayer(color, colorAlpha, faceColor, tabletFace * 0.48);
            blendLayer(color, colorAlpha, edgeColor, tabletEdge * 0.82);
            blendLayer(color, colorAlpha, glyphColor, tabletGlyph * 0.78);
        }
    }

    float bloom = glow(platformRadius, 4.2)
            * bloomPulse
            * (0.40 + platformReveal * 0.48);
    blendLayer(color, colorAlpha, mix(edgeColor, glyphColor, 0.28), bloom * 0.24);

    alpha = clamp(colorAlpha, 0.0, 1.0);
    return color;
}

float foundationFractureLines(vec2 point, float time) {
    float cracks = 0.0;
    for (int index = 0; index < 9; index++) {
        float indexValue = float(index);
        float seed = hash(indexValue + 4.7);
        float angle = indexValue / 9.0 * TAU
                + (seed - 0.5) * 0.24
                + sin(time * 0.45 + indexValue) * 0.012;
        float lengthValue = 0.46 + seed * 0.36;
        vec2 endPoint = vec2(cos(angle), sin(angle)) * lengthValue;
        cracks = max(
                cracks,
                segmentLine(point, vec2(0.0), endPoint, 0.0065)
        );

        float branchProgress = 0.38 + hash(indexValue * 2.31 + 1.0) * 0.30;
        vec2 branchStart = endPoint * branchProgress;
        float branchAngle = angle
                + (hash(indexValue * 3.77 + 7.0) - 0.5) * 1.24;
        vec2 branchEnd = branchStart
                + vec2(cos(branchAngle), sin(branchAngle))
                * (0.085 + seed * 0.08);
        cracks = max(
                cracks,
                segmentLine(point, branchStart, branchEnd, 0.0052)
        );
    }
    return cracks;
}

float signedAngleDistance(float angle, float target) {
    return atan(sin(angle - target), cos(angle - target));
}

float cross2(vec2 first, vec2 second) {
    return first.x * second.y - first.y * second.x;
}

float convexShardMask(
        vec2 point,
        vec2 vertex0,
        vec2 vertex1,
        vec2 vertex2,
        vec2 vertex3,
        vec2 vertex4,
        vec2 vertex5,
        vec2 vertex6,
        vec2 vertex7
) {
    float edgeDistance = 100.0;
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex1 - vertex0, point - vertex0)
                    / max(length(vertex1 - vertex0), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex2 - vertex1, point - vertex1)
                    / max(length(vertex2 - vertex1), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex3 - vertex2, point - vertex2)
                    / max(length(vertex3 - vertex2), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex4 - vertex3, point - vertex3)
                    / max(length(vertex4 - vertex3), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex5 - vertex4, point - vertex4)
                    / max(length(vertex5 - vertex4), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex6 - vertex5, point - vertex5)
                    / max(length(vertex6 - vertex5), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex7 - vertex6, point - vertex6)
                    / max(length(vertex7 - vertex6), 0.0001)
    );
    edgeDistance = min(
            edgeDistance,
            -cross2(vertex0 - vertex7, point - vertex7)
                    / max(length(vertex0 - vertex7), 0.0001)
    );
    return smoothstep(
            0.0,
            max(fwidth(edgeDistance) * 1.45, 0.0012),
            edgeDistance
    );
}

float foundationShard(
        vec2 platformPoint,
        float angle,
        float displacement,
        float halfAngle,
        float outerRadius,
        float innerRadius,
        float seed
) {
    vec2 direction = vec2(cos(angle), sin(angle));
    vec2 localPoint = rotation(-angle)
            * (platformPoint - direction * displacement);
    float shardWidth = outerRadius * tan(halfAngle);
    float outerJitter = (hash(seed * 17.31 + 0.42) - 0.5) * 0.10;
    float innerJitter = (hash(seed * 23.77 + 1.16) - 0.5) * 0.035;
    float irregularOuterRadius = outerRadius + outerJitter;
    float innerPoint = innerRadius + innerJitter;
    vec2 vertex0 = vec2(
            innerPoint,
            -shardWidth * (0.56 + seed * 0.10)
    );
    vec2 vertex1 = vec2(
            innerPoint + 0.055 + seed * 0.035,
            -shardWidth * (0.30 + seed * 0.11)
    );
    vec2 vertex2 = vec2(
            irregularOuterRadius * (0.60 + seed * 0.10),
            -shardWidth * (0.96 - seed * 0.08)
    );
    vec2 vertex3 = vec2(
            irregularOuterRadius,
            -shardWidth * (0.48 + seed * 0.18)
    );
    vec2 vertex4 = vec2(
            irregularOuterRadius * (0.91 - seed * 0.07),
            shardWidth * (0.68 + seed * 0.18)
    );
    vec2 vertex5 = vec2(
            irregularOuterRadius * (0.52 + seed * 0.12),
            shardWidth * (1.00 - seed * 0.12)
    );
    vec2 vertex6 = vec2(
            innerPoint + 0.045 + seed * 0.030,
            shardWidth * (0.27 + seed * 0.13)
    );
    vec2 vertex7 = vec2(
            innerPoint,
            shardWidth * (0.60 + seed * 0.10)
    );
    return convexShardMask(
            localPoint,
            vertex0,
            vertex1,
            vertex2,
            vertex3,
            vertex4,
            vertex5,
            vertex6,
            vertex7
    );
}

float foundationShardEdge(
        vec2 platformPoint,
        float angle,
        float displacement,
        float halfAngle,
        float outerRadius,
        float innerRadius,
        float seed
) {
    vec2 direction = vec2(cos(angle), sin(angle));
    vec2 localPoint = rotation(-angle)
            * (platformPoint - direction * displacement);
    float shardWidth = outerRadius * tan(halfAngle);
    float outerJitter = (hash(seed * 17.31 + 0.42) - 0.5) * 0.10;
    float innerJitter = (hash(seed * 23.77 + 1.16) - 0.5) * 0.035;
    float irregularOuterRadius = outerRadius + outerJitter;
    float innerPoint = innerRadius + innerJitter;
    vec2 vertex0 = vec2(
            innerPoint,
            -shardWidth * (0.56 + seed * 0.10)
    );
    vec2 vertex1 = vec2(
            innerPoint + 0.055 + seed * 0.035,
            -shardWidth * (0.30 + seed * 0.11)
    );
    vec2 vertex2 = vec2(
            irregularOuterRadius * (0.60 + seed * 0.10),
            -shardWidth * (0.96 - seed * 0.08)
    );
    vec2 vertex3 = vec2(
            irregularOuterRadius,
            -shardWidth * (0.48 + seed * 0.18)
    );
    vec2 vertex4 = vec2(
            irregularOuterRadius * (0.91 - seed * 0.07),
            shardWidth * (0.68 + seed * 0.18)
    );
    vec2 vertex5 = vec2(
            irregularOuterRadius * (0.52 + seed * 0.12),
            shardWidth * (1.00 - seed * 0.12)
    );
    vec2 vertex6 = vec2(
            innerPoint + 0.045 + seed * 0.030,
            shardWidth * (0.27 + seed * 0.13)
    );
    vec2 vertex7 = vec2(
            innerPoint,
            shardWidth * (0.60 + seed * 0.10)
    );
    float edge = 0.0;
    edge = max(edge, segmentLine(localPoint, vertex0, vertex1, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex1, vertex2, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex2, vertex3, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex3, vertex4, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex4, vertex5, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex5, vertex6, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex6, vertex7, 0.009));
    edge = max(edge, segmentLine(localPoint, vertex7, vertex0, 0.009));
    return edge;
}

float shardSphereDistance(
        vec2 shardPoint,
        float angle,
        float displacement,
        float sphereRadius,
        float morphProgress,
        float seed,
        float time
) {
    vec2 direction = vec2(cos(angle), sin(angle));
    float spin = morphProgress * (0.45 + seed * 0.90);
    vec2 localPoint = rotation(-angle - spin)
            * (shardPoint - direction * displacement);
    float localRadius = length(localPoint);
    float localAngle = atan(localPoint.y, localPoint.x);
    float twist = sin(
            localAngle * (2.0 + seed * 3.0)
                    + time * (2.10 + seed * 1.20)
                    + seed * TAU
    ) * (
            0.008
            + (1.0 - morphProgress) * 0.016
    );
    float secondaryTwist = cos(
            localAngle * (3.0 + seed * 2.0)
                    - time * (1.25 + seed * 0.80)
                    + seed * 4.7
    ) * 0.005 * (1.0 - morphProgress * 0.45);
    float edgeWarp = (
            cloudNoise(vec2(
                    localPoint * 8.0 + vec2(seed * 3.4, time * 0.18)
            )) - 0.5
    ) * 0.010 * (1.0 - morphProgress * 0.55);
    float radialWarp = (twist + secondaryTwist + edgeWarp)
            * smoothstep(0.0, max(sphereRadius, 0.001), localRadius);
    return localRadius + radialWarp;
}

float goldenCoreFlow(vec2 localPoint, float time) {
    float radius = length(localPoint);
    float sphereMask = 1.0 - smoothstep(0.82, 1.0, radius);
    if (sphereMask <= 0.0) {
        return 0.0;
    }

    float surfaceDepth = sqrt(max(1.0 - radius * radius, 0.0));
    float latitude = asin(clamp(localPoint.y, -1.0, 1.0));
    float longitude = atan(localPoint.x, max(surfaceDepth, 0.001));
    float broadWarp = cloudNoise(vec2(
            longitude * 2.15 + time * 0.10,
            latitude * 3.60 - time * 0.16
    ));
    float detailWarp = cloudNoise(vec2(
            longitude * 7.0 - time * 0.24,
            latitude * 9.0 + time * 0.18
    ));
    float warp = (broadWarp - 0.5) * 2.8 + (detailWarp - 0.5) * 0.85;

    float streamA = sin(
            longitude * 4.0
            + latitude * 6.2
            - time * 1.45
            + warp
    );
    float streamB = sin(
            longitude * 7.0
            - latitude * 3.6
            - time * 2.05
            + detailWarp * 4.2
    );
    float streamC = sin(
            longitude * 11.0
            + latitude * 9.0
            - time * 2.85
            + broadWarp * 5.0
    );
    float broadStream = smoothstep(0.69, 0.94, 0.5 + 0.5 * streamA);
    float narrowStream = smoothstep(0.78, 0.985, 0.5 + 0.5 * streamB);
    float filament = smoothstep(0.84, 0.995, 0.5 + 0.5 * streamC);
    float frontLighting = 0.28 + 0.72 * smoothstep(0.04, 0.96, surfaceDepth);
    return sphereMask * frontLighting * (
            broadStream * 0.62
            + narrowStream * 0.32
            + filament * 0.18
    );
}

vec3 renderFoundationToGoldenCore(
        vec2 point,
        float progress,
        float time,
        out float alpha
) {
    float trembleProgress = smoothstep(
            0.0,
            FOUNDATION_TO_GOLDEN_CORE_TREMBLE_END,
            progress
    );
    float breakProgress = smoothstep(
            FOUNDATION_TO_GOLDEN_CORE_BREAK_START,
            FOUNDATION_TO_GOLDEN_CORE_BREAK_END,
            progress
    );
    float violentShakeEnvelope = 1.0
            - smoothstep(
                    FOUNDATION_TO_GOLDEN_CORE_VIOLENT_SHAKE_END,
                    FOUNDATION_TO_GOLDEN_CORE_SETTLE_END,
                    progress
            );
    float gatherProgress = smoothstep(
            FOUNDATION_TO_GOLDEN_CORE_GATHER_START,
            FOUNDATION_TO_GOLDEN_CORE_GATHER_END,
            progress
    );
    float gatherEase = easeInOut(gatherProgress);
    float preBreakTremble = trembleProgress * (1.0 - breakProgress);
    float rupturePulse = exp(-pow(
            (progress - FOUNDATION_TO_GOLDEN_CORE_BREAK_START) / 0.018,
            2.0
    ));
    float shakeStrength = preBreakTremble * (
            0.0015
            + 0.0055 * easeIn(preBreakTremble)
    ) + breakProgress * violentShakeEnvelope * (
            0.010
            + 0.018 * (0.70 + 0.30 * sin(time * 4.0))
    );
    vec2 shakeOffset = vec2(
            sin(time * 31.0 + 0.7) + sin(time * 53.0 + 2.2) * 0.42,
            cos(time * 37.0 + 1.4) + cos(time * 61.0 + 0.3) * 0.36
    ) * shakeStrength;
    float shakeAngle = (
            sin(time * 28.0 + 1.2) * 0.010
            + sin(time * 47.0 + 0.6) * 0.006
    ) * preBreakTremble;
    shakeAngle += (
            sin(time * 34.0 + 0.9) * 0.032
            + sin(time * 67.0 + 2.6) * 0.018
    ) * breakProgress * violentShakeEnvelope;
    vec2 animatedPoint = rotation(-shakeAngle) * (point - shakeOffset);
    float crackProgress = breakProgress;
    float shardOutProgress = smoothstep(
            FOUNDATION_TO_GOLDEN_CORE_BREAK_START,
            FOUNDATION_TO_GOLDEN_CORE_VIOLENT_SHAKE_END,
            progress
    );
    float shardVisibility = shardOutProgress
            * (1.0 - smoothstep(0.84, 0.96, progress));
    float platformVisibility = 1.0
            - smoothstep(
                    FOUNDATION_TO_GOLDEN_CORE_BREAK_START,
                    FOUNDATION_TO_GOLDEN_CORE_BREAK_END + 0.055,
                    progress
            );

    vec3 platformSideColor = mix(PrimaryColor, vec3(0.0), 0.28);
    vec3 platformFaceColor = mix(PrimaryColor, SecondaryColor, 0.28);
    vec3 platformEdgeColor = mix(SecondaryColor, AccentColor, 0.14);
    vec3 color = vec3(0.0);
    float colorAlpha = 0.0;

    vec2 platformCenter = vec2(0.0, 0.04);
    float platformScale = 0.78
            + preBreakTremble * 0.012
            + breakProgress * violentShakeEnvelope * 0.024
            + crackProgress * 0.020;
    vec2 platformPoint = (animatedPoint - platformCenter)
            / vec2(platformScale, platformScale * 0.46);
    float platformRadius = length(platformPoint);
    float platformFace = 1.0 - smoothstep(0.90, 1.0, platformRadius);
    float lowerRadius = length(
            (animatedPoint - platformCenter - vec2(0.0, 0.070))
                    / vec2(platformScale * 1.015, platformScale * 0.46)
    );
    float lowerFace = 1.0 - smoothstep(0.90, 1.0, lowerRadius);
    float lowerHalf = smoothstep(-0.12, 0.66, platformPoint.y);
    float platformSide = lowerFace
            * (1.0 - platformFace * 0.96)
            * lowerHalf;
    float outerEdge = softRing(platformRadius, 0.935, 0.018, 0.030);
    float middleEdge = softRing(platformRadius, 0.690, 0.012, 0.024);
    float innerEdge = softRing(platformRadius, 0.455, 0.010, 0.022);
    float platformArcs = platformArc(
            platformRadius,
            atan(platformPoint.y, platformPoint.x),
            0.82,
            0.010,
            time * 0.42,
            8.0
    ) + platformArc(
            platformRadius,
            atan(platformPoint.y, platformPoint.x),
            0.61,
            0.008,
            -time * 0.28 + 1.2,
            12.0
    );
    float surfaceSigils = platformSurfaceSigils(platformPoint, time);
    float crackMask = foundationFractureLines(platformPoint, time)
            * crackProgress;
    float platformLayer = platformVisibility
            * (0.94 + 0.06 * preBreakTremble);

    blendLayer(
            color,
            colorAlpha,
            platformSideColor,
            platformSide * platformLayer * 0.72
    );
    blendLayer(
            color,
            colorAlpha,
            platformFaceColor,
            platformFace * platformLayer * 0.44
    );
    blendLayer(
            color,
            colorAlpha,
            platformEdgeColor,
            (outerEdge * 0.84 + middleEdge * 0.42)
                    * platformLayer
                    * 0.78
    );
    blendLayer(
            color,
            colorAlpha,
            AccentColor,
            (innerEdge * 0.52 + platformArcs * 0.46
                    + surfaceSigils * 0.52)
                    * platformLayer
                    * (0.62 + crackProgress * 0.38)
    );
    blendLayer(
            color,
            colorAlpha,
            mix(AccentColor, vec3(1.0), 0.28),
            crackMask * platformLayer * 1.15
    );

    float fractureFlash = glow(platformRadius, 5.8)
            * rupturePulse
            * (0.72 + 0.28 * violentShakeEnvelope);
    color += AccentColor * fractureFlash * 0.72;
    colorAlpha = max(colorAlpha, fractureFlash * 0.52);

    vec2 shardPoint = (animatedPoint - platformCenter)
            / vec2(platformScale, platformScale * 0.46);
    float targetSphereRadius = 0.19 / max(platformScale, 0.001);
    for (int index = 0; index < 12; index++) {
        float indexValue = float(index);
        float seed = hash(indexValue * 4.37 + 2.1);
        float seedTwo = hash(indexValue * 7.13 + 9.4);
        float sectorAngle = TAU / 12.0;
        float baseAngle = indexValue * sectorAngle
                + sectorAngle * 0.5
                + sin(indexValue * 2.7 + 0.9) * 0.018;
        float halfAngle = sectorAngle * (0.435 + seed * 0.025);
        float outerRadius = 0.88 + seedTwo * 0.085;
        float innerRadius = 0.028 + seed * 0.024;
        float outwardDistance = mix(
                0.0,
                0.62 + seedTwo * 0.20,
                easeOut(shardOutProgress)
        ) + rupturePulse * (0.055 + seedTwo * 0.045);
        float displacement = mix(
                outwardDistance,
                0.018 + seed * 0.026,
                gatherEase
        );
        float shardAngle = baseAngle
                + sin(time * (0.72 + seed * 0.26) + indexValue * 1.9)
                * shardOutProgress
                * 0.045
                + gatherEase * (0.72 + seed * 0.85)
                + violentShakeEnvelope
                * sin(time * (5.6 + seed * 1.5) + indexValue * 2.4)
                * 0.18;
        float shardMask = foundationShard(
                shardPoint,
                shardAngle,
                displacement,
                halfAngle,
                outerRadius,
                innerRadius,
                seed
        );
        float shardMorph = smoothstep(0.08, 0.92, gatherEase);
        float sphereRadius = mix(
                0.024 + seed * 0.012,
                targetSphereRadius * (0.92 + seed * 0.12),
                gatherEase
        );
        float sphereDistance = shardSphereDistance(
                shardPoint,
                shardAngle,
                displacement,
                sphereRadius,
                shardMorph,
                seed,
                time
        );
        float sphereMask = 1.0 - smoothstep(
                sphereRadius * 0.66,
                sphereRadius * 0.98,
                sphereDistance
        );
        float morphedShardMask = mix(shardMask, sphereMask, shardMorph);
        float shardEdge = foundationShardEdge(
                shardPoint,
                shardAngle,
                displacement,
                halfAngle,
                outerRadius,
                innerRadius,
                seed
        );
        float sphereEdge = softRing(
                sphereDistance,
                sphereRadius * 0.88,
                0.008 + (1.0 - shardMorph) * 0.004,
                0.018
        ) * shardMorph;
        vec2 shardDirection = vec2(cos(shardAngle), sin(shardAngle));
        vec2 localShardPoint = rotation(-shardAngle)
                * (shardPoint - shardDirection * displacement);
        float shardWidth = outerRadius * tan(halfAngle);
        float shardGrooves = (
                segmentLine(
                        localShardPoint,
                        vec2(innerRadius + 0.075, -shardWidth * 0.18),
                        vec2(outerRadius * 0.72, shardWidth * 0.34),
                        0.0065
                ) + segmentLine(
                        localShardPoint,
                        vec2(innerRadius + 0.11, shardWidth * 0.28),
                        vec2(outerRadius * 0.58, -shardWidth * 0.40),
                        0.0055
                )
        ) * shardMask * (1.0 - shardMorph);
        vec3 shardColor = mix(
                platformFaceColor,
                platformSideColor,
                0.18 + seed * 0.24
        );
        shardColor = mix(
                shardColor,
                mix(vec3(0.98, 0.58, 0.075), vec3(1.0, 0.82, 0.28), 0.22),
                shardMorph * 0.82
        );
        float shardPulse = 0.80 + 0.20 * sin(
                time * (2.0 + seed * 1.0) + indexValue * 2.1
        );
        float activeShardVisibility = shardVisibility * shardPulse;
        blendLayer(
                color,
                colorAlpha,
                shardColor,
                morphedShardMask * activeShardVisibility * 0.92
        );
        blendLayer(
                color,
                colorAlpha,
                AccentColor,
                (
                        shardEdge * (1.0 - shardMorph) * 0.92
                        + sphereEdge * 1.12
                        + shardGrooves * (0.20 + crackProgress * 0.16)
                )
                        * activeShardVisibility
                        * 0.92
        );
    }

    float coreProgress = smoothstep(
            FOUNDATION_TO_GOLDEN_CORE_CORE_REVEAL_START,
            FOUNDATION_TO_GOLDEN_CORE_CORE_REVEAL_END,
            progress
    );
    float coreStable = smoothstep(0.78, 0.84, progress);
    float coreScale = mix(0.028, 0.19, easeOut(coreProgress));
    float coreDistance = length(animatedPoint - platformCenter);
    vec2 corePoint = (animatedPoint - platformCenter) / max(coreScale, 0.001);
    float coreRadius = length(corePoint);
    float coreMask = 1.0 - smoothstep(0.90, 1.0, coreRadius);
    float surfaceDepth = sqrt(max(1.0 - coreRadius * coreRadius, 0.0));
    float edgeLight = pow(1.0 - surfaceDepth, 2.25);
    float centerLight = 1.0 - smoothstep(
            0.0,
            1.18,
            length(corePoint - vec2(-0.22, -0.24))
    );
    float corePulse = 0.94 + 0.06 * sin(time * 2.35);
    float formationPulse = exp(-pow((progress - 0.80) / 0.055, 2.0));
    float flowReveal = smoothstep(
            FOUNDATION_TO_GOLDEN_CORE_FLOW_START,
            FOUNDATION_TO_GOLDEN_CORE_FLOW_END,
            progress
    );
    float flow = goldenCoreFlow(corePoint, time);
    float flowSpark = pow(clamp(flow * 1.45, 0.0, 1.0), 3.0)
            * (0.74 + 0.26 * sin(time * 4.1 + corePoint.x * 7.0));

    vec3 goldenBase = vec3(0.72, 0.25, 0.025);
    vec3 goldenBody = vec3(0.98, 0.58, 0.075);
    vec3 goldenLight = vec3(1.0, 0.82, 0.28);
    vec3 whiteGold = vec3(1.0, 0.96, 0.70);
    vec3 coreColor = mix(
            goldenBase,
            goldenBody,
            clamp(0.30 + centerLight * 0.52 + surfaceDepth * 0.18, 0.0, 1.0)
    );
    coreColor = mix(
            coreColor,
            goldenLight,
            edgeLight * 0.28 + centerLight * 0.22
    );
    coreColor *= corePulse;

    float coreHalo = exp(-pow(
            coreDistance / max(coreScale * 1.45, 0.001),
            2.0
    ))
            * coreProgress
            * (0.30 + coreStable * 0.32 + formationPulse * 0.34);
    float outerHalo = exp(-pow(
            coreDistance / max(coreScale * 3.8, 0.001),
            2.0
    ))
            * coreProgress
            * (0.20 + coreStable * 0.16 + formationPulse * 0.24);
    float coreContour = softRing(
            coreRadius,
            0.915,
            0.015,
            0.028
    ) * coreProgress;
    float pulseHalo = exp(-pow(
            coreDistance / max(coreScale * 4.4, 0.001),
            2.0
    ))
            * formationPulse
            * 0.42;
    float pulseFlash = exp(-pow(
            coreDistance / max(coreScale * 8.0, 0.001),
            2.0
    ))
            * formationPulse
            * 0.18;

    blendLayer(
            color,
            colorAlpha,
            coreColor,
            coreMask * coreProgress * 0.90
    );
    blendLayer(
            color,
            colorAlpha,
            goldenLight,
            coreContour * (0.14 + coreStable * 0.10)
    );
    color += goldenLight * coreHalo * 0.54;
    color += goldenLight * outerHalo * 0.38;
    color += goldenLight * pulseHalo * 0.30;
    color += whiteGold * pulseFlash;
    color += mix(goldenLight, whiteGold, 0.26)
            * flow
            * flowReveal
            * coreProgress
            * 0.86;
    color += whiteGold
            * flowSpark
            * flowReveal
            * coreProgress
            * 0.38;
    colorAlpha = max(
            colorAlpha,
            coreMask * coreProgress * 0.94
                    + coreHalo * 0.30
                    + outerHalo * 0.16
                    + pulseHalo * 0.20
                    + flow * flowReveal * 0.34
    );

    alpha = clamp(colorAlpha, 0.0, 1.0);
    return color;
}

float nascentSoulEllipse(vec2 point, vec2 center, vec2 scale) {
    float radius = length((point - center) / scale);
    return 1.0 - smoothstep(0.88, 1.0, radius);
}

float nascentSoulEllipseEdge(vec2 point, vec2 center, vec2 scale) {
    float radius = length((point - center) / scale);
    return softRing(radius, 0.90, 0.025, 0.035);
}

float nascentSoulOrientedEllipse(
        vec2 point,
        vec2 center,
        vec2 scale,
        float angle
) {
    return nascentSoulEllipse(
            rotation(-angle) * (point - center),
            vec2(0.0),
            scale
    );
}

float nascentSoulOrientedEllipseEdge(
        vec2 point,
        vec2 center,
        vec2 scale,
        float angle
) {
    return nascentSoulEllipseEdge(
            rotation(-angle) * (point - center),
            vec2(0.0),
            scale
    );
}

float nascentSoulPointedPetal(
        vec2 point,
        vec2 basePoint,
        vec2 tipPoint,
        float width
) {
    vec2 direction = tipPoint - basePoint;
    float lengthSquared = max(dot(direction, direction), 0.0001);
    float projection = clamp(
            dot(point - basePoint, direction) / lengthSquared,
            0.0,
            1.0
    );
    vec2 centerPoint = mix(basePoint, tipPoint, projection);
    float distanceToCenter = length(point - centerPoint);
    float pointedWidth = width * pow(
            max(sin(projection * PI), 0.0),
            0.62
    );
    float bodyMask = 1.0 - smoothstep(
            pointedWidth,
            pointedWidth + 0.028,
            distanceToCenter
    );
    float endMask = smoothstep(0.0, 0.08, projection)
            * (1.0 - smoothstep(0.90, 1.0, projection));
    return bodyMask * endMask;
}

float nascentSoulPointedPetalEdge(
        vec2 point,
        vec2 basePoint,
        vec2 tipPoint,
        float width
) {
    float outer = nascentSoulPointedPetal(
            point,
            basePoint,
            tipPoint,
            width
    );
    float inner = nascentSoulPointedPetal(
            point,
            mix(basePoint, tipPoint, 0.035),
            mix(basePoint, tipPoint, 0.965),
            width * 0.86
    );
    return max(outer - inner, 0.0);
}

float nascentSoulPointedPetalVein(
        vec2 point,
        vec2 basePoint,
        vec2 tipPoint,
        float width
) {
    vec2 direction = normalize(tipPoint - basePoint);
    vec2 perpendicular = vec2(-direction.y, direction.x);
    float centerVein = segmentLine(
            point,
            mix(basePoint, tipPoint, 0.12),
            mix(basePoint, tipPoint, 0.90),
            0.005
    );
    float leftVein = segmentLine(
            point,
            mix(basePoint, tipPoint, 0.24) + perpendicular * width * 0.10,
            mix(basePoint, tipPoint, 0.74) + perpendicular * width * 0.34,
            0.0035
    );
    float rightVein = segmentLine(
            point,
            mix(basePoint, tipPoint, 0.24) - perpendicular * width * 0.10,
            mix(basePoint, tipPoint, 0.74) - perpendicular * width * 0.34,
            0.0035
    );
    return max(centerVein, max(leftVein, rightVein));
}

float nascentSoulPetal(
        vec2 point,
        vec2 center,
        vec2 scale,
        float angle
) {
    vec2 localPoint = rotation(-angle) * (point - center);
    localPoint /= scale;
    float verticalMask = smoothstep(-1.08, -0.90, localPoint.y)
            * (1.0 - smoothstep(0.82, 1.05, localPoint.y));
    float width = mix(
            0.045,
            0.88,
            smoothstep(-0.98, 0.50, localPoint.y)
    );
    float sideMask = 1.0 - smoothstep(
            width,
            width + 0.08,
            abs(localPoint.x)
    );
    return clamp(verticalMask * sideMask, 0.0, 1.0);
}

float nascentSoulPetalEdge(
        vec2 point,
        vec2 center,
        vec2 scale,
        float angle
) {
    float outer = nascentSoulPetal(point, center, scale, angle);
    float inner = nascentSoulPetal(
            point,
            center,
            scale * 0.90,
            angle
    );
    return max(outer - inner, 0.0);
}

float nascentSoulPetalVein(
        vec2 point,
        vec2 center,
        vec2 scale,
        float angle
) {
    vec2 axis = vec2(sin(angle), -cos(angle));
    return segmentLine(
            point,
            center - axis * scale.y * 0.82,
            center + axis * scale.y * 0.58,
            0.006
    );
}

float nascentSoulHairStrand(
        vec2 point,
        vec2 headCenter,
        float strandIndex,
        float time
) {
    float normalizedIndex = strandIndex / 4.0;
    float startX = mix(-0.15, 0.15, normalizedIndex);
    float endX = startX + sin(time * 0.22 + strandIndex * 1.7) * 0.012;
    vec2 startPoint = headCenter + vec2(startX, -0.17);
    vec2 endPoint = headCenter + vec2(endX, -0.25 + abs(startX) * 0.14);
    return segmentLine(point, startPoint, endPoint, 0.009);
}

vec3 renderNascentSoulFigure(
        vec2 point,
        float progress,
        float time,
        float lotusReveal,
        float babyReveal,
        out float alpha
) {
    vec3 color = vec3(0.0);
    float colorAlpha = 0.0;

    float lotusScale = mix(0.08, 1.0, easeOutBack(lotusReveal));
    vec2 lotusCenter = vec2(0.0, 0.48);
    vec3 lotusBaseColor = vec3(0.66, 0.85, 0.83);
    vec3 lotusShadowColor = vec3(0.48, 0.70, 0.70);
    vec3 lotusLightColor = vec3(0.93, 1.0, 0.98);
    vec3 lotusEdgeColor = vec3(1.0, 0.98, 0.84);

    float lotusAura = exp(-pow(
            length((point - lotusCenter) / vec2(0.86, 0.57)),
            2.0
    )) * lotusReveal * (0.18 + 0.08 * sin(time * 2.2));
    color += lotusLightColor * lotusAura * 0.62;
    colorAlpha = max(colorAlpha, lotusAura * 0.42);

    float lotusBowl = nascentSoulEllipse(
            point,
            lotusCenter + vec2(0.0, 0.13 * lotusScale),
            vec2(0.67, 0.21) * lotusScale
    ) * lotusReveal;
    blendLayer(
            color,
            colorAlpha,
            lotusShadowColor,
            lotusBowl * 0.55
    );

    for (int index = 0; index < 13; index++) {
        float normalizedIndex = float(index) / 12.0;
        float theta = mix(0.12, PI - 0.12, normalizedIndex);
        float variation = hash(float(index) * 3.17 + 7.0);
        vec2 basePoint = lotusCenter
                + vec2(cos(theta) * 0.065, sin(theta) * 0.045)
                * lotusScale;
        vec2 tipPoint = lotusCenter
                + vec2(
                        cos(theta) * (0.60 + variation * 0.13),
                        sin(theta) * (0.40 + variation * 0.07)
                ) * lotusScale;
        float petalWidth = 0.13 + variation * 0.065;
        float petalMask = nascentSoulPointedPetal(
                point,
                basePoint,
                tipPoint,
                petalWidth
        ) * lotusReveal;
        float petalEdge = nascentSoulPointedPetalEdge(
                point,
                basePoint,
                tipPoint,
                petalWidth
        ) * lotusReveal;
        float petalVein = nascentSoulPointedPetalVein(
                point,
                basePoint,
                tipPoint,
                petalWidth
        ) * lotusReveal;
        vec3 petalColor = mix(
                lotusBaseColor,
                lotusLightColor,
                0.18 + normalizedIndex * 0.18
        );
        blendLayer(
                color,
                colorAlpha,
                petalColor,
                petalMask * 0.82
        );
        blendLayer(
                color,
                colorAlpha,
                lotusEdgeColor,
                petalEdge * 0.78 + petalVein * 0.22
        );
    }

    float backWispLeft = segmentLine(
            point,
            vec2(-0.54, 0.40),
            vec2(-0.78, 0.13),
            0.009
    ) + segmentLine(
            point,
            vec2(-0.78, 0.13),
            vec2(-0.68, -0.04),
            0.007
    );
    float backWispRight = segmentLine(
            point,
            vec2(0.54, 0.40),
            vec2(0.78, 0.13),
            0.009
    ) + segmentLine(
            point,
            vec2(0.78, 0.13),
            vec2(0.68, -0.04),
            0.007
    );
    blendLayer(
            color,
            colorAlpha,
            lotusLightColor,
            (backWispLeft + backWispRight) * lotusReveal * 0.32
    );

    float babyScale = mix(0.08, 1.0, easeOutBack(babyReveal));
    vec2 babyAnchor = vec2(0.0, 0.43);
    vec2 babyPoint = (point - babyAnchor) / babyScale + babyAnchor;
    vec2 headCenter = vec2(0.0, -0.28);

    vec3 skinColor = vec3(1.0, 0.70, 0.56);
    vec3 skinLightColor = vec3(1.0, 0.86, 0.74);
    vec3 skinShadowColor = vec3(0.82, 0.43, 0.32);
    vec3 hairColor = vec3(0.38, 0.20, 0.11);
    vec3 hairLightColor = vec3(0.67, 0.40, 0.20);
    vec3 robeColor = vec3(0.91, 0.96, 0.93);
    vec3 robeShadowColor = vec3(0.64, 0.78, 0.77);
    vec3 robeLightColor = vec3(1.0, 0.99, 0.91);
    vec3 sashColor = vec3(0.45, 0.82, 0.82);
    vec3 lineColor = vec3(0.40, 0.25, 0.17);
    vec3 babyLightColor = vec3(0.94, 1.0, 0.98);

    float leftLeg = nascentSoulOrientedEllipse(
            babyPoint,
            vec2(-0.19, 0.43),
            vec2(0.25, 0.105),
            -0.20
    );
    float rightLeg = nascentSoulOrientedEllipse(
            babyPoint,
            vec2(0.19, 0.43),
            vec2(0.25, 0.105),
            0.20
    );
    float leftKnee = nascentSoulEllipse(
            babyPoint,
            vec2(-0.28, 0.35),
            vec2(0.105, 0.085)
    );
    float rightKnee = nascentSoulEllipse(
            babyPoint,
            vec2(0.28, 0.35),
            vec2(0.105, 0.085)
    );
    blendLayer(
            color,
            colorAlpha,
            skinColor,
            max(leftLeg, rightLeg) * babyReveal * 0.88
    );
    blendLayer(
            color,
            colorAlpha,
            skinLightColor,
            max(leftKnee, rightKnee) * babyReveal * 0.78
    );

    float torso = nascentSoulEllipse(
            babyPoint,
            vec2(0.0, 0.12),
            vec2(0.30, 0.29)
    );
    float robeSkirt = nascentSoulEllipse(
            babyPoint,
            vec2(0.0, 0.38),
            vec2(0.45, 0.18)
    );
    float robeShoulders = nascentSoulEllipse(
            babyPoint,
            vec2(0.0, 0.03),
            vec2(0.39, 0.18)
    );
    float leftSleeve = segmentLine(
            babyPoint,
            vec2(-0.22, 0.01),
            vec2(-0.34, 0.29),
            0.105
    );
    float rightSleeve = segmentLine(
            babyPoint,
            vec2(0.22, 0.01),
            vec2(0.34, 0.29),
            0.105
    );
    float robeMask = max(
            max(torso, robeSkirt),
            max(robeShoulders, max(leftSleeve, rightSleeve))
    );
    blendLayer(
            color,
            colorAlpha,
            robeColor,
            robeMask * babyReveal * 0.86
    );
    blendLayer(
            color,
            colorAlpha,
            robeShadowColor,
            (robeSkirt * 0.38 + leftSleeve * 0.18 + rightSleeve * 0.18)
                    * babyReveal
    );

    float neck = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.0, 0.24),
            vec2(0.13, 0.095)
    );
    float leftEar = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(-0.30, 0.01),
            vec2(0.075, 0.105)
    );
    float rightEar = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.30, 0.01),
            vec2(0.075, 0.105)
    );
    float headMask = nascentSoulEllipse(
            babyPoint,
            headCenter,
            vec2(0.335, 0.315)
    );
    blendLayer(
            color,
            colorAlpha,
            skinShadowColor,
            neck * babyReveal * 0.54
    );
    blendLayer(
            color,
            colorAlpha,
            skinColor,
            (leftEar + rightEar) * babyReveal * 0.84
    );
    blendLayer(
            color,
            colorAlpha,
            skinLightColor,
            headMask * babyReveal * 0.94
    );
    float faceShadow = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.0, 0.08),
            vec2(0.30, 0.21)
    );
    blendLayer(
            color,
            colorAlpha,
            skinColor,
            faceShadow * babyReveal * 0.26
    );
    blendLayer(
            color,
            colorAlpha,
            skinLightColor,
            nascentSoulEllipse(
                    babyPoint,
                    headCenter + vec2(-0.10, -0.12),
                    vec2(0.16, 0.11)
            ) * babyReveal * 0.18
    );

    float leftEarInner = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(-0.30, 0.015),
            vec2(0.038, 0.068)
    );
    float rightEarInner = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.30, 0.015),
            vec2(0.038, 0.068)
    );
    blendLayer(
            color,
            colorAlpha,
            skinShadowColor,
            (leftEarInner + rightEarInner) * babyReveal * 0.42
    );

    float hairCap = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.0, -0.105),
            vec2(0.335, 0.205)
    ) * (1.0 - step(headCenter.y - 0.015, babyPoint.y));
    blendLayer(
            color,
            colorAlpha,
            hairColor,
            hairCap * babyReveal * 0.92
    );
    for (int index = 0; index < 9; index++) {
        float normalizedIndex = float(index) / 8.0;
        float strandX = mix(-0.27, 0.27, normalizedIndex);
        float strandCurve = (0.5 - normalizedIndex)
                * 0.050
                + sin(time * 0.20 + float(index) * 1.3) * 0.008;
        float strand = segmentLine(
                babyPoint,
                headCenter + vec2(strandX, -0.18),
                headCenter + vec2(strandX + strandCurve, -0.015),
                0.010
        );
        blendLayer(
                color,
                colorAlpha,
                hairLightColor,
                strand * babyReveal * 0.58
        );
    }
    float leftSideHair = segmentLine(
            babyPoint,
            headCenter + vec2(-0.30, -0.16),
            headCenter + vec2(-0.32, 0.06),
            0.009
    );
    float rightSideHair = segmentLine(
            babyPoint,
            headCenter + vec2(0.30, -0.16),
            headCenter + vec2(0.32, 0.06),
            0.009
    );
    blendLayer(
            color,
            colorAlpha,
            hairColor,
            (leftSideHair + rightSideHair) * babyReveal * 0.72
    );

    float leftEye = segmentLine(
            babyPoint,
            headCenter + vec2(-0.20, 0.015),
            headCenter + vec2(-0.075, 0.030),
            0.009
    );
    float rightEye = segmentLine(
            babyPoint,
            headCenter + vec2(0.075, 0.030),
            headCenter + vec2(0.20, 0.015),
            0.009
    );
    float leftBrow = segmentLine(
            babyPoint,
            headCenter + vec2(-0.19, -0.075),
            headCenter + vec2(-0.08, -0.060),
            0.005
    );
    float rightBrow = segmentLine(
            babyPoint,
            headCenter + vec2(0.08, -0.060),
            headCenter + vec2(0.19, -0.075),
            0.005
    );
    float noseLine = segmentLine(
            babyPoint,
            headCenter + vec2(0.0, 0.060),
            headCenter + vec2(-0.012, 0.097),
            0.005
    );
    float noseTip = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.012, 0.098),
            vec2(0.022, 0.015)
    );
    float mouthLeft = segmentLine(
            babyPoint,
            headCenter + vec2(-0.050, 0.135),
            headCenter + vec2(0.0, 0.148),
            0.006
    );
    float mouthRight = segmentLine(
            babyPoint,
            headCenter + vec2(0.0, 0.148),
            headCenter + vec2(0.050, 0.135),
            0.006
    );
    float leftBlush = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(-0.205, 0.105),
            vec2(0.060, 0.026)
    );
    float rightBlush = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.205, 0.105),
            vec2(0.060, 0.026)
    );
    blendLayer(
            color,
            colorAlpha,
            lineColor,
            (leftEye + rightEye + leftBrow + rightBrow
                    + noseLine + mouthLeft + mouthRight)
                    * babyReveal * 0.74
    );
    blendLayer(
            color,
            colorAlpha,
            skinShadowColor,
            noseTip * babyReveal * 0.36
    );
    blendLayer(
            color,
            colorAlpha,
            vec3(1.0, 0.40, 0.38),
            (leftBlush + rightBlush) * babyReveal * 0.22
    );

    float leftCollar = segmentLine(
            babyPoint,
            vec2(-0.19, -0.025),
            vec2(0.14, 0.25),
            0.030
    );
    float rightCollar = segmentLine(
            babyPoint,
            vec2(0.19, -0.025),
            vec2(-0.14, 0.25),
            0.030
    );
    float leftCollarLine = segmentLine(
            babyPoint,
            vec2(-0.19, -0.025),
            vec2(0.14, 0.25),
            0.006
    );
    float rightCollarLine = segmentLine(
            babyPoint,
            vec2(0.19, -0.025),
            vec2(-0.14, 0.25),
            0.006
    );
    float waistSash = segmentLine(
            babyPoint,
            vec2(-0.30, 0.26),
            vec2(0.30, 0.26),
            0.034
    );
    float waistSashEdge = segmentLine(
            babyPoint,
            vec2(-0.29, 0.285),
            vec2(0.29, 0.285),
            0.007
    );
    float leftFold = segmentLine(
            babyPoint,
            vec2(-0.24, 0.02),
            vec2(-0.12, 0.36),
            0.006
    );
    float rightFold = segmentLine(
            babyPoint,
            vec2(0.24, 0.02),
            vec2(0.12, 0.36),
            0.006
    );
    blendLayer(
            color,
            colorAlpha,
            sashColor,
            (leftCollar + rightCollar + waistSash) * babyReveal * 0.82
    );
    blendLayer(
            color,
            colorAlpha,
            robeLightColor,
            (leftCollarLine + rightCollarLine + waistSashEdge)
                    * babyReveal * 0.86
    );
    blendLayer(
            color,
            colorAlpha,
            robeShadowColor,
            (leftFold + rightFold) * babyReveal * 0.54
    );

    float leftHand = nascentSoulEllipse(
            babyPoint,
            vec2(-0.24, 0.325),
            vec2(0.080, 0.095)
    );
    float rightHand = nascentSoulEllipse(
            babyPoint,
            vec2(0.24, 0.325),
            vec2(0.080, 0.095)
    );
    blendLayer(
            color,
            colorAlpha,
            skinLightColor,
            (leftHand + rightHand) * babyReveal * 0.92
    );
    for (int index = 0; index < 3; index++) {
        float fingerOffset = (float(index) - 1.0) * 0.020;
        float leftFinger = segmentLine(
                babyPoint,
                vec2(-0.27 + fingerOffset, 0.33),
                vec2(-0.27 + fingerOffset, 0.375),
                0.004
        );
        float rightFinger = segmentLine(
                babyPoint,
                vec2(0.27 - fingerOffset, 0.33),
                vec2(0.27 - fingerOffset, 0.375),
                0.004
        );
        blendLayer(
                color,
                colorAlpha,
                skinShadowColor,
                (leftFinger + rightFinger) * babyReveal * 0.42
        );
    }

    float babyHalo = exp(-pow(
            length((point - vec2(0.0, 0.02)) / vec2(0.58, 0.72)),
            2.0
    )) * babyReveal * (
            0.18 + 0.18 * smoothstep(
                    GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_START,
                    GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_END,
                    progress
            )
    );
    float purityPulse = smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_END,
            progress
    ) * (0.78 + 0.22 * sin(time * 2.0));
    color += babyLightColor * babyHalo * (0.72 + purityPulse * 0.32);
    color += babyLightColor
            * exp(-pow(length(point - vec2(0.0, -0.16)) / 0.20, 2.0))
            * purityPulse
            * 0.11;
    colorAlpha = max(colorAlpha, babyHalo * 0.64);

    for (int index = 0; index < 9; index++) {
        float normalizedIndex = float(index) / 8.0;
        float theta = mix(0.22, PI - 0.22, normalizedIndex);
        float variation = hash(float(index) * 5.41 + 13.0);
        vec2 basePoint = lotusCenter
                + vec2(
                        cos(theta) * (0.035 + variation * 0.025),
                        0.10 + sin(theta) * 0.035
                ) * lotusScale;
        vec2 tipPoint = lotusCenter
                + vec2(
                        cos(theta) * (0.46 + variation * 0.12),
                        0.29 + sin(theta) * (0.28 + variation * 0.06)
                ) * lotusScale;
        float petalWidth = 0.15 + variation * 0.065;
        float petalMask = nascentSoulPointedPetal(
                point,
                basePoint,
                tipPoint,
                petalWidth
        ) * lotusReveal;
        float petalEdge = nascentSoulPointedPetalEdge(
                point,
                basePoint,
                tipPoint,
                petalWidth
        ) * lotusReveal;
        float petalVein = nascentSoulPointedPetalVein(
                point,
                basePoint,
                tipPoint,
                petalWidth
        ) * lotusReveal;
        blendLayer(
                color,
                colorAlpha,
                mix(lotusBaseColor, lotusLightColor, 0.26),
                petalMask * 0.88
        );
        blendLayer(
                color,
                colorAlpha,
                lotusEdgeColor,
                petalEdge * 0.84 + petalVein * 0.30
        );
    }

    float lotusCenterPetal = nascentSoulPointedPetal(
            point,
            lotusCenter + vec2(0.0, 0.08) * lotusScale,
            lotusCenter + vec2(0.0, 0.47) * lotusScale,
            0.21
    ) * lotusReveal;
    blendLayer(
            color,
            colorAlpha,
            lotusLightColor,
            lotusCenterPetal * 0.72
    );

    alpha = clamp(
            colorAlpha
                    + lotusReveal * 0.07
                    + babyReveal * 0.10,
            0.0,
            1.0
    );
    return color;
}

vec3 renderGoldenCoreToNascentSoul(
        vec2 point,
        float progress,
        float time,
        out float alpha
) {
    float trembleProgress = smoothstep(
            0.0,
            GOLDEN_CORE_TO_NASCENT_SOUL_TREMBLE_END,
            progress
    );
    float breakProgress = smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_END,
            progress
    );
    float violentShakeEnvelope = 1.0 - smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_VIOLENT_SHAKE_END,
            GOLDEN_CORE_TO_NASCENT_SOUL_SETTLE_END,
            progress
    );
    float shardOutProgress = smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_VIOLENT_SHAKE_END,
            progress
    );
    float shardVisibility = shardOutProgress
            * (1.0 - smoothstep(0.36, 0.50, progress));
    float coreVisibility = 1.0 - smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_END + 0.08,
            progress
    );

    vec2 coreShake = vec2(
            sin(time * 33.0 + 0.8) + sin(time * 61.0 + 2.1) * 0.42,
            cos(time * 39.0 + 1.4) + cos(time * 73.0 + 0.3) * 0.36
    ) * (
            trembleProgress * 0.0035
            + breakProgress * violentShakeEnvelope * 0.016
    );
    float coreAngle = (
            sin(time * 29.0 + 1.2) * 0.012
            + sin(time * 47.0 + 0.6) * 0.007
    ) * trembleProgress;
    coreAngle += (
            sin(time * 37.0 + 0.9) * 0.032
            + sin(time * 67.0 + 2.6) * 0.018
    ) * breakProgress * violentShakeEnvelope;

    vec2 coreCenter = vec2(0.0, 0.08);
    vec2 animatedPoint = rotation(-coreAngle)
            * (point - coreCenter - coreShake);
    vec2 corePoint = animatedPoint / 0.19;
    float coreRadius = length(corePoint);
    float coreMask = 1.0 - smoothstep(0.88, 1.0, coreRadius);
    float coreSurfaceDepth = sqrt(max(1.0 - coreRadius * coreRadius, 0.0));
    float coreHighlight = 1.0 - smoothstep(
            0.0,
            1.0,
            length(corePoint - vec2(-0.25, -0.28))
    );
    float coreFlow = goldenCoreFlow(corePoint, time * 0.72);
    vec3 goldenCoreColor = mix(
            vec3(0.75, 0.25, 0.025),
            vec3(1.0, 0.62, 0.10),
            clamp(0.30 + coreSurfaceDepth * 0.38 + coreHighlight * 0.32, 0.0, 1.0)
    );
    goldenCoreColor = mix(
            goldenCoreColor,
            vec3(1.0, 0.88, 0.40),
            coreFlow * 0.42
    );

    vec3 color = vec3(0.0);
    float colorAlpha = 0.0;
    float corePulse = 0.92 + 0.08 * sin(time * 2.6);
    blendLayer(
            color,
            colorAlpha,
            goldenCoreColor * corePulse,
            coreMask * coreVisibility * 0.94
    );
    float coreEdge = nascentSoulEllipseEdge(
            animatedPoint,
            vec2(0.0),
            vec2(0.19)
    ) * coreVisibility;
    blendLayer(
            color,
            colorAlpha,
            vec3(1.0, 0.88, 0.42),
            coreEdge * 0.72
    );

    float crackMask = foundationFractureLines(corePoint, time * 0.72)
            * breakProgress;
    float rupturePulse = exp(-pow(
            (progress - GOLDEN_CORE_TO_NASCENT_SOUL_BREAK_START) / 0.014,
            2.0
    ));
    blendLayer(
            color,
            colorAlpha,
            vec3(1.0, 0.95, 0.68),
            crackMask * coreVisibility * 1.18
                    + rupturePulse * 0.78
    );
    color += vec3(1.0, 0.86, 0.32)
            * rupturePulse
            * glow(length(animatedPoint), 4.8)
            * 0.58;

    for (int index = 0; index < 10; index++) {
        float indexValue = float(index);
        float seed = hash(indexValue * 4.37 + 18.1);
        float seedTwo = hash(indexValue * 7.13 + 21.4);
        float baseAngle = indexValue / 10.0 * TAU
                + 0.20
                + (seed - 0.5) * 0.14;
        float outwardDistance = mix(
                0.0,
                0.62 + seedTwo * 0.18,
                easeOut(shardOutProgress)
        ) + rupturePulse * (0.04 + seedTwo * 0.04);
        float displacement = outwardDistance;
        float shardAngle = baseAngle
                + shardOutProgress * (0.45 + seed * 0.75)
                + violentShakeEnvelope
                * sin(time * (5.0 + seed * 1.7) + indexValue * 2.2)
                * 0.16;
        float halfAngle = TAU / 10.0 * (0.42 + seed * 0.025);
        float outerRadius = 0.72 + seedTwo * 0.18;
        float innerRadius = 0.025 + seed * 0.024;
        float shardMask = foundationShard(
                corePoint,
                shardAngle,
                displacement,
                halfAngle,
                outerRadius,
                innerRadius,
                seed
        );
        float shardEdge = foundationShardEdge(
                corePoint,
                shardAngle,
                displacement,
                halfAngle,
                outerRadius,
                innerRadius,
                seed
        );
        vec3 shardColor = mix(
                vec3(0.70, 0.20, 0.02),
                vec3(1.0, 0.68, 0.16),
                0.28 + seed * 0.30
        );
        float shardPulse = 0.78 + 0.22 * sin(
                time * (2.2 + seed * 1.1) + indexValue * 2.4
        );
        blendLayer(
                color,
                colorAlpha,
                shardColor,
                shardMask * shardVisibility * shardPulse * 0.90
        );
        blendLayer(
                color,
                colorAlpha,
                vec3(1.0, 0.90, 0.48),
                shardEdge * shardVisibility * 0.98
        );
    }

    {
        float lotusReveal = smoothstep(
                GOLDEN_CORE_TO_NASCENT_SOUL_LOTUS_REVEAL_START,
                GOLDEN_CORE_TO_NASCENT_SOUL_LOTUS_REVEAL_END,
                progress
        );
        float babyReveal = smoothstep(
                GOLDEN_CORE_TO_NASCENT_SOUL_BABY_REVEAL_START,
                GOLDEN_CORE_TO_NASCENT_SOUL_BABY_REVEAL_END,
                progress
        );
        float figureAlpha;
        vec3 figureColor = renderNascentSoulFigure(
                point,
                progress,
                time,
                lotusReveal,
                babyReveal,
                figureAlpha
        );
        blendLayer(
                color,
                colorAlpha,
                figureColor,
                figureAlpha
        );
        alpha = clamp(
                colorAlpha
                        + shardVisibility * 0.08
                        + lotusReveal * 0.06
                        + babyReveal * 0.08,
                0.0,
                1.0
        );
        return color;
    }

    float lotusReveal = smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_LOTUS_REVEAL_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_LOTUS_REVEAL_END,
            progress
    );
    float lotusScale = mix(0.18, 1.0, easeOut(lotusReveal));
    vec2 lotusCenter = vec2(0.0, 0.45);
    vec3 lotusBaseColor = mix(
            SecondaryColor,
            vec3(0.78, 0.97, 0.92),
            0.54
    );
    vec3 lotusLightColor = mix(
            vec3(0.90, 1.0, 0.98),
            vec3(1.0),
            0.34
    );

    for (int index = 0; index < 9; index++) {
        float normalizedIndex = float(index) / 8.0;
        float petalAngle = mix(-1.14, 1.14, normalizedIndex);
        vec2 petalDirection = vec2(
                sin(petalAngle),
                -cos(petalAngle)
        );
        vec2 petalCenter = lotusCenter
                + petalDirection * (0.16 * lotusScale);
        vec2 petalScale = vec2(
                0.17 * lotusScale,
                0.33 * lotusScale
        ) * (0.90 + hash(float(index) + 4.0) * 0.14);
        float petalMask = nascentSoulPetal(
                point,
                petalCenter,
                petalScale,
                petalAngle
        ) * lotusReveal * 0.82;
        float petalEdge = nascentSoulPetalEdge(
                point,
                petalCenter,
                petalScale,
                petalAngle
        ) * lotusReveal;
        float petalVein = nascentSoulPetalVein(
                point,
                petalCenter,
                petalScale,
                petalAngle
        ) * lotusReveal;
        vec3 petalColor = mix(
                lotusBaseColor,
                lotusLightColor,
                0.24 + normalizedIndex * 0.20
        );
        blendLayer(
                color,
                colorAlpha,
                petalColor,
                petalMask * 0.74
        );
        blendLayer(
                color,
                colorAlpha,
                lotusLightColor,
                petalEdge * 0.84 + petalVein * 0.28
        );
    }

    float lotusHalo = exp(-pow(
            length(point - lotusCenter) / max(0.28 * lotusScale, 0.001),
            2.0
    )) * lotusReveal * (
            0.24 + 0.16 * sin(time * 2.1)
    );
    color += lotusLightColor * lotusHalo * 0.56;
    colorAlpha = max(colorAlpha, lotusHalo * 0.48);

    float babyReveal = smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_BABY_REVEAL_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_BABY_REVEAL_END,
            progress
    );
    float babyScale = mix(0.12, 1.0, easeOut(babyReveal));
    vec2 babyAnchor = vec2(0.0, 0.06);
    vec2 babyPoint = (point - babyAnchor) / babyScale + babyAnchor;
    vec2 headCenter = vec2(0.0, -0.29);
    float headMask = nascentSoulEllipse(
            babyPoint,
            headCenter,
            vec2(0.225, 0.235)
    );
    float leftEar = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(-0.205, 0.01),
            vec2(0.065, 0.085)
    );
    float rightEar = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.205, 0.01),
            vec2(0.065, 0.085)
    );
    float robeMask = max(
            nascentSoulEllipse(
                    babyPoint,
                    vec2(0.0, 0.13),
                    vec2(0.27, 0.32)
            ),
            nascentSoulEllipse(
                    babyPoint,
                    vec2(0.0, 0.34),
                    vec2(0.30, 0.17)
            )
    );
    float leftSleeve = segmentLine(
            babyPoint,
            vec2(-0.19, 0.02),
            vec2(-0.31, 0.22),
            0.070
    );
    float rightSleeve = segmentLine(
            babyPoint,
            vec2(0.19, 0.02),
            vec2(0.31, 0.22),
            0.070
    );
    float leftHand = nascentSoulEllipse(
            babyPoint,
            vec2(-0.31, 0.23),
            vec2(0.060, 0.067)
    );
    float rightHand = nascentSoulEllipse(
            babyPoint,
            vec2(0.31, 0.23),
            vec2(0.060, 0.067)
    );
    float leftFoot = nascentSoulEllipse(
            babyPoint,
            vec2(-0.13, 0.45),
            vec2(0.14, 0.095)
    );
    float rightFoot = nascentSoulEllipse(
            babyPoint,
            vec2(0.13, 0.45),
            vec2(0.14, 0.095)
    );
    vec3 robeColor = mix(
            vec3(0.82, 0.96, 0.93),
            vec3(0.98, 1.0, 0.98),
            0.48
    );
    vec3 skinColor = vec3(1.0, 0.73, 0.60);
    vec3 skinLightColor = vec3(1.0, 0.88, 0.78);
    vec3 hairColor = vec3(0.52, 0.36, 0.22);
    vec3 babyLightColor = vec3(0.96, 1.0, 0.99);

    float robeBody = robeMask + max(leftSleeve, rightSleeve) * 0.82;
    blendLayer(
            color,
            colorAlpha,
            robeColor,
            robeBody * babyReveal * 0.82
    );
    blendLayer(
            color,
            colorAlpha,
            skinColor,
            (leftFoot + rightFoot) * babyReveal * 0.78
    );
    blendLayer(
            color,
            colorAlpha,
            skinColor,
            (leftHand + rightHand) * babyReveal * 0.90
    );
    blendLayer(
            color,
            colorAlpha,
            skinLightColor,
            (headMask + leftEar + rightEar) * babyReveal * 0.92
    );

    float hairCap = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.0, -0.085),
            vec2(0.222, 0.145)
    ) * (1.0 - smoothstep(-0.34, -0.16, babyPoint.y));
    float hairStrands = 0.0;
    for (int index = 0; index < 5; index++) {
        hairStrands = max(
                hairStrands,
                nascentSoulHairStrand(
                        babyPoint,
                        headCenter,
                        float(index),
                        time
                )
        );
    }
    blendLayer(
            color,
            colorAlpha,
            hairColor,
            (hairCap * 0.82 + hairStrands * 0.64) * babyReveal
    );

    float leftEye = segmentLine(
            babyPoint,
            headCenter + vec2(-0.115, 0.025),
            headCenter + vec2(-0.035, 0.020),
            0.008
    );
    float rightEye = segmentLine(
            babyPoint,
            headCenter + vec2(0.035, 0.020),
            headCenter + vec2(0.115, 0.025),
            0.008
    );
    float nose = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.0, 0.070),
            vec2(0.017, 0.022)
    );
    float mouth = segmentLine(
            babyPoint,
            headCenter + vec2(-0.035, 0.112),
            headCenter + vec2(0.035, 0.112),
            0.006
    );
    float leftBlush = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(-0.135, 0.082),
            vec2(0.035, 0.018)
    );
    float rightBlush = nascentSoulEllipse(
            babyPoint,
            headCenter + vec2(0.135, 0.082),
            vec2(0.035, 0.018)
    );
    blendLayer(
            color,
            colorAlpha,
            mix(hairColor, vec3(0.14, 0.08, 0.05), 0.36),
            (leftEye + rightEye + mouth) * babyReveal * 0.68
    );
    blendLayer(
            color,
            colorAlpha,
            skinLightColor,
            nose * babyReveal * 0.72
    );
    blendLayer(
            color,
            colorAlpha,
            vec3(1.0, 0.42, 0.40),
            (leftBlush + rightBlush) * babyReveal * 0.24
    );

    float sashLeft = segmentLine(
            babyPoint,
            vec2(-0.17, -0.04),
            vec2(0.08, 0.26),
            0.008
    );
    float sashRight = segmentLine(
            babyPoint,
            vec2(0.17, -0.04),
            vec2(-0.08, 0.26),
            0.008
    );
    float robeHem = nascentSoulEllipseEdge(
            babyPoint,
            vec2(0.0, 0.34),
            vec2(0.30, 0.17)
    );
    blendLayer(
            color,
            colorAlpha,
            mix(SecondaryColor, lotusLightColor, 0.42),
            (sashLeft + sashRight + robeHem) * babyReveal * 0.66
    );

    float babyHalo = exp(-pow(
            length(point - vec2(0.0, -0.04)) / 0.48,
            2.0
    )) * babyReveal * (
            0.24 + 0.22 * smoothstep(
                    GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_START,
                    GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_END,
                    progress
            )
    );
    float purityPulse = smoothstep(
            GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_START,
            GOLDEN_CORE_TO_NASCENT_SOUL_PURITY_END,
            progress
    ) * (0.78 + 0.22 * sin(time * 2.0));
    color += babyLightColor * babyHalo * (0.68 + purityPulse * 0.42);
    color += babyLightColor
            * exp(-pow(length(point - vec2(0.0, -0.04)) / 0.18, 2.0))
            * purityPulse
            * 0.12;
    colorAlpha = max(colorAlpha, babyHalo * 0.62);

    for (int index = 0; index < 5; index++) {
        float normalizedIndex = float(index) / 4.0;
        float petalAngle = mix(-0.72, 0.72, normalizedIndex);
        vec2 petalDirection = vec2(
                sin(petalAngle),
                -cos(petalAngle)
        );
        vec2 petalCenter = lotusCenter
                + petalDirection * (0.18 * lotusScale)
                + vec2(0.0, 0.075 * lotusScale);
        vec2 petalScale = vec2(
                0.19 * lotusScale,
                0.27 * lotusScale
        );
        float petalMask = nascentSoulPetal(
                point,
                petalCenter,
                petalScale,
                petalAngle
        ) * lotusReveal * 0.88;
        float petalEdge = nascentSoulPetalEdge(
                point,
                petalCenter,
                petalScale,
                petalAngle
        ) * lotusReveal;
        blendLayer(
                color,
                colorAlpha,
                mix(lotusBaseColor, lotusLightColor, 0.36),
                petalMask * 0.78
        );
        blendLayer(
                color,
                colorAlpha,
                lotusLightColor,
                petalEdge * 0.90
        );
    }

    alpha = clamp(
            colorAlpha
                    + shardVisibility * 0.08
                    + lotusReveal * 0.06
                    + babyReveal * 0.08,
            0.0,
            1.0
    );
    return color;
}

void main() {
    float progress = clamp(Progress, 0.0, 1.0);
    float fadeIn = smoothstep(0.0, 0.055, progress);
    float fadeOut = 1.0 - smoothstep(0.86, 1.0, progress);
    float animationFade = fadeIn * fadeOut;

    vec2 safeScreenSize = max(ScreenSize, vec2(1.0));
    vec2 point = animationUv * 2.0 - 1.0;
    float shakeStrength = clamp(ShakeStrength, 0.0, 1.0);
    vec2 shakePixels = vec2(
            sin(Time * 63.0 + 0.8) + sin(Time * 97.0 + 2.1) * 0.42,
            cos(Time * 71.0 + 1.4) + cos(Time * 113.0 + 0.3) * 0.36
    ) * (7.0 + 9.0 * shakeStrength) * shakeStrength;
    point -= shakePixels / vec2(safeScreenSize.x, safeScreenSize.y);
    float shakeAngle = (
            sin(Time * 59.0 + 1.2) * 0.004
            + sin(Time * 89.0 + 0.6) * 0.0025
    ) * shakeStrength;
    point = rotation(shakeAngle) * point;
    point.x *= safeScreenSize.x / safeScreenSize.y;

    float alpha;
    vec3 color;
    if (AnimationType < 0.5) {
        color = renderMortalToQi(point, progress, Time, alpha);
    } else if (AnimationType < 1.5) {
        color = renderQiToFoundation(point, progress, Time, alpha);
    } else if (AnimationType < 2.5) {
        color = renderFoundationToGoldenCore(point, progress, Time, alpha);
    } else {
        color = renderGoldenCoreToNascentSoul(point, progress, Time, alpha);
    }

    color = mixLight(color, 0.08 + 0.10 * smoothstep(0.34, 0.82, progress));
    alpha *= vertexColor.a * animationFade;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 1.0));
}
