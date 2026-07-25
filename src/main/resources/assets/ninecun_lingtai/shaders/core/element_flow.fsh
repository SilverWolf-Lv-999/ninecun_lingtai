#version 150

in vec4 vertexColor;
in vec2 flowUv;

uniform float FlowTime;
uniform float FlowStyle;
uniform float FlowCutoff;
uniform float FlowDirection;

out vec4 fragColor;

float randomValue(float value) {
    return fract(sin(value * 91.3458) * 47453.5453);
}

void main() {
    float pathProgress = clamp(flowUv.x, 0.0, 1.0);
    float directedProgress = FlowDirection > 0.0 ? pathProgress : 1.0 - pathProgress;
    float phase = directedProgress * 8.0 - FlowTime * 2.6;
    float centerOffset = 0.0;
    float pulse = 0.45 + 0.55 * pow(0.5 + 0.5 * sin(phase * 6.2831853), 5.0);
    float textureStrength = 1.0;

    if (FlowStyle > 0.5 && FlowStyle < 1.5) {
        centerOffset = sin(pathProgress * 20.0 - FlowTime * 1.8) * 0.045;
        float branchVein = abs(sin(pathProgress * 31.0 + flowUv.y * 8.0));
        textureStrength = 0.62 + 0.38 * smoothstep(0.32, 0.95, branchVein);
    } else if (FlowStyle > 1.5 && FlowStyle < 2.5) {
        centerOffset = sin(pathProgress * 17.0 - FlowTime * 4.2) * 0.105;
        pulse = 0.38 + 0.62 * pow(0.5 + 0.5 * sin(phase * 5.1), 3.0);
    } else if (FlowStyle > 2.5 && FlowStyle < 3.5) {
        float flameNoise = randomValue(floor(pathProgress * 34.0) + floor(FlowTime * 15.0));
        centerOffset = (flameNoise - 0.5) * 0.16 * pathProgress;
        pulse = 0.28 + 0.72 * pow(0.5 + 0.5 * sin(phase * 7.5 + flameNoise * 4.0), 8.0);
        textureStrength = 0.72 + flameNoise * 0.28;
    } else if (FlowStyle > 3.5 && FlowStyle < 4.5) {
        float earthPulse = smoothstep(0.08, 0.5, fract(phase * 0.72));
        pulse = 0.48 + earthPulse * 0.52;
        textureStrength = 0.72 + 0.28 * step(0.45, fract(pathProgress * 18.0));
    } else if (FlowStyle < 0.5) {
        pulse = 0.32 + 0.68 * pow(0.5 + 0.5 * sin(phase * 8.5), 12.0);
        textureStrength = 0.82 + 0.18 * cos(pathProgress * 37.0);
    }

    float centerDistance = abs(flowUv.y - 0.5 - centerOffset);
    float body = 1.0 - smoothstep(0.17, 0.5, centerDistance);
    float core = 1.0 - smoothstep(0.0, 0.17, centerDistance);
    float cutoffMask = 1.0 - smoothstep(
            max(0.0, FlowCutoff - 0.045),
            min(1.0, FlowCutoff + 0.012),
            pathProgress
    );
    float endpointFade = smoothstep(0.0, 0.035, pathProgress)
            * (1.0 - smoothstep(0.965, 1.0, pathProgress));
    float alpha = vertexColor.a * body * textureStrength * (0.3 + pulse * 0.7) * cutoffMask * endpointFade;

    vec3 flowColor = mix(vertexColor.rgb, vec3(1.0), core * (0.28 + pulse * 0.24));
    if (FlowStyle > 4.5) {
        float yinYangStripe = step(0.5, fract(phase * 0.34 + flowUv.y * 0.55));
        flowColor = mix(vec3(0.035, 0.04, 0.055), vec3(0.95, 0.94, 0.9), yinYangStripe);
        alpha *= 0.82 + 0.18 * pulse;
    }

    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(flowColor, alpha);
}
