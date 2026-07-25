#version 150

in vec4 vertexColor;
in vec2 flowUv;

uniform float FlowTime;
uniform float FlowStyle;
uniform float FlowMode;
uniform float FlowSeed;
uniform float FlowCutoff;
uniform float FlowDirection;

out vec4 fragColor;

float randomValue(float value) {
    return fract(sin(value * 91.3458 + FlowSeed * 47.17) * 47453.5453);
}

float softBand(float distanceValue, float width, float softness) {
    return 1.0 - smoothstep(width, width + softness, abs(distanceValue));
}

void main() {
    float pathProgress = clamp(flowUv.x, 0.0, 1.0);
    float directedProgress = FlowDirection > 0.0 ? pathProgress : 1.0 - pathProgress;
    float side = flowUv.y * 2.0 - 1.0;
    float packetPhase = directedProgress * 9.0 - FlowTime * 2.4 + FlowSeed * 3.7;
    float body = 0.0;
    float core = 0.0;
    float textureStrength = 1.0;
    vec3 flowColor = vertexColor.rgb;

    if (FlowStyle < 0.5) {
        float slash = sin(pathProgress * 18.0 + FlowSeed * 8.0) * 0.08;
        float dash = smoothstep(0.08, 0.32, fract(packetPhase * 0.31));
        body = softBand(side - slash, 0.1, 0.12) * dash;
        core = softBand(side - slash, 0.025, 0.055) * dash;
        float spark = softBand(fract(packetPhase) - 0.5, 0.08, 0.08)
                * softBand(side + slash * 2.0, 0.36, 0.16);
        body = max(body, spark * 0.72);
        flowColor = mix(flowColor, vec3(1.0, 0.96, 0.76), core * 0.82 + spark * 0.46);
    } else if (FlowStyle < 1.5) {
        float vineCenter = sin(pathProgress * 13.0 - FlowTime * 1.35 + FlowSeed * 5.0) * 0.16;
        float vine = softBand(side - vineCenter, 0.075, 0.08);
        float leafCell = fract(pathProgress * 8.0 - FlowTime * 0.62 + FlowSeed);
        float leafSide = mod(floor(pathProgress * 8.0 + FlowSeed * 5.0), 2.0) * 2.0 - 1.0;
        vec2 leafPoint = vec2((leafCell - 0.52) * 4.4, side - vineCenter - leafSide * 0.38);
        float leaf = 1.0 - smoothstep(0.28, 0.47, length(leafPoint));
        body = max(vine, leaf * 0.92);
        core = vine * softBand(side - vineCenter, 0.02, 0.035);
        textureStrength = 0.68 + 0.32 * sin(pathProgress * 38.0 + FlowTime * 0.8);
        flowColor = mix(flowColor, vec3(0.72, 0.96, 0.53), leaf * 0.42 + core * 0.32);
    } else if (FlowStyle < 2.5) {
        float waveA = sin(pathProgress * 20.0 - FlowTime * 3.1 + FlowSeed * 4.0) * 0.19;
        float waveB = sin(pathProgress * 16.0 - FlowTime * 2.4 + FlowSeed * 7.0) * 0.12;
        float streamA = softBand(side - waveA, 0.115, 0.12);
        float streamB = softBand(side + 0.34 - waveB, 0.055, 0.08);
        float dropletCell = fract(packetPhase * 0.55);
        vec2 dropletPoint = vec2((dropletCell - 0.5) * 3.8, side - waveA * 0.6 + 0.28);
        float droplet = 1.0 - smoothstep(0.16, 0.32, length(dropletPoint));
        body = max(streamA, max(streamB * 0.7, droplet));
        core = softBand(side - waveA, 0.025, 0.055);
        textureStrength = 0.72 + 0.28 * sin(pathProgress * 31.0 - FlowTime * 4.0);
        flowColor = mix(flowColor, vec3(0.75, 0.94, 1.0), core * 0.62 + droplet * 0.34);
    } else if (FlowStyle < 3.5) {
        float flameCell = fract(pathProgress * 10.0 - FlowTime * 1.45 + FlowSeed);
        float flameNoise = randomValue(floor(pathProgress * 27.0) + floor(FlowTime * 12.0));
        float flameCenter = (flameNoise - 0.5) * 0.34 + sin(pathProgress * 17.0 - FlowTime * 3.4) * 0.11;
        float flameWidth = mix(0.44, 0.08, flameCell);
        float flame = softBand(side - flameCenter * flameCell, flameWidth, 0.11);
        float flicker = 0.42 + 0.58 * smoothstep(0.08, 0.72, sin(packetPhase * 4.6 + flameNoise * 5.0));
        body = flame * flicker;
        core = softBand(side - flameCenter * flameCell, flameWidth * 0.28, 0.06) * flicker;
        textureStrength = 0.62 + flameNoise * 0.38;
        flowColor = mix(flowColor, vec3(1.0, 0.9, 0.4), core * 0.82);
    } else if (FlowStyle < 4.5) {
        float rockCell = fract(pathProgress * 9.0 - FlowTime * 0.48 + FlowSeed);
        float rockIndex = floor(pathProgress * 9.0 - FlowTime * 0.48 + FlowSeed);
        float rockOffset = (randomValue(rockIndex) - 0.5) * 0.42;
        vec2 rockPoint = vec2((rockCell - 0.5) * 2.9, side - rockOffset);
        float rock = 1.0 - smoothstep(0.44, 0.57, max(abs(rockPoint.x), abs(rockPoint.y)));
        float dust = softBand(side + rockOffset * 0.35, 0.18, 0.25)
                * smoothstep(0.28, 0.72, sin(packetPhase * 2.2));
        body = max(rock, dust * 0.34);
        core = rock * (1.0 - smoothstep(0.14, 0.35, length(rockPoint)));
        flowColor = mix(flowColor, vec3(0.91, 0.69, 0.39), core * 0.42);
    } else {
        float beadCell = fract(packetPhase * 0.72);
        vec2 beadPoint = vec2((beadCell - 0.5) * 3.7, side);
        float bead = 1.0 - smoothstep(0.19, 0.34, length(beadPoint));
        float thread = softBand(side, 0.025, 0.05) * 0.18;
        body = max(bead, thread);
        core = bead * (1.0 - smoothstep(0.0, 0.17, length(beadPoint)));
        float alternating = step(0.5, fract(floor(packetPhase * 0.72) * 0.5));
        vec3 darkBalance = vec3(0.19, 0.21, 0.25);
        vec3 lightBalance = FlowMode > 1.5 && FlowMode < 2.5
                ? vec3(1.0, 0.9, 0.64)
                : vec3(0.9, 0.92, 0.96);
        flowColor = mix(darkBalance, lightBalance, alternating);
        flowColor = mix(flowColor, vec3(1.0), core * 0.36);
    }

    if (FlowMode > 0.5 && FlowMode < 1.5) {
        float breakup = smoothstep(0.66, 0.98, directedProgress);
        float fragments = smoothstep(0.12, 0.74, sin(packetPhase * 6.0 + side * 9.0));
        body *= mix(1.0, fragments, breakup * 0.7);
        textureStrength *= 1.0 - breakup * 0.28;
    }

    float cutoffMask = 1.0 - smoothstep(
            max(0.0, FlowCutoff - 0.025),
            min(1.0, FlowCutoff + 0.008),
            pathProgress
    );
    float endpointFade = smoothstep(0.0, 0.025, pathProgress)
            * (1.0 - smoothstep(0.975, 1.0, pathProgress));
    float movingEnergy = 0.66 + 0.34 * pow(0.5 + 0.5 * sin(packetPhase * 6.2831853), 3.0);
    float alpha = vertexColor.a * body * textureStrength * movingEnergy * cutoffMask * endpointFade;
    flowColor = mix(flowColor, vec3(1.0), core * 0.22);

    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(flowColor, alpha);
}
