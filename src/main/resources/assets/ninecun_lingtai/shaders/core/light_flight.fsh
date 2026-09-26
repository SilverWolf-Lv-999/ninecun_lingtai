#version 150

in vec4 vertexColor;
in vec2 lightFlightUv;

uniform float Time;
uniform float Mode;
uniform float Boost;
uniform float TailStrength;
uniform float ImpactProgress;
uniform float ImpactLayer;

out vec4 fragColor;

void main() {
    vec3 baseColor = vertexColor.rgb;
    float alpha;
    float core;
    float energy;

    if (Mode < 0.5) {
        vec2 centered = lightFlightUv * 2.0 - 1.0;
        float radius = length(centered);
        float body = 1.0 - smoothstep(0.35, 1.0, radius);
        float halo = 1.0 - smoothstep(0.18, 0.88, radius);
        float pulse = 0.9 + 0.1 * sin(Time * 5.4 + centered.x * 6.0 + centered.y * 8.0);
        core = 1.0 - smoothstep(0.0, 0.31, radius);
        energy = body * 0.72 + halo * 0.44;
        alpha = vertexColor.a * energy * pulse;
        baseColor = mix(baseColor, vec3(1.0), core * 0.9 + Boost * 0.16);
    } else if (Mode < 1.5) {
        float progress = clamp(lightFlightUv.x, 0.0, 1.0);
        float radial = abs(lightFlightUv.y * 2.0 - 1.0);
        float wave = sin(progress * 18.0 - Time * 7.0) * 0.08
                + sin(progress * 43.0 + Time * 4.0) * 0.035;
        float tailFade = 1.0 - smoothstep(0.72, 1.0, progress);
        float body = 1.0 - smoothstep(0.48, 0.98, radial);
        float coreShift = 0.5 + wave;
        float coreDistance = abs(lightFlightUv.y - coreShift);
        core = (1.0 - smoothstep(0.02, 0.25, coreDistance)) * tailFade;
        energy = max(body * 0.72, core * 0.86) * tailFade;
        alpha = vertexColor.a * TailStrength * energy;
        baseColor = mix(baseColor, vec3(0.76, 1.0, 0.96), core * 0.62 + Boost * 0.18);
        baseColor *= 0.9 + 0.1 * sin(progress * 36.0 - Time * 5.0);
    } else {
        float progress = clamp(ImpactProgress, 0.0, 1.0);
        float longitude = lightFlightUv.x * 6.2831853;
        float latitude = (lightFlightUv.y - 0.5) * 3.1415926;
        float flow = 0.5 + 0.5 * sin(
                longitude * 5.0
                        + sin(latitude * 4.0 + Time * 2.4) * 2.1
                        - Time * 6.0
        );
        float turbulence = 0.5 + 0.5 * sin(
                longitude * 13.0
                        - latitude * 7.0
                        + sin(longitude * 3.0 + Time * 3.0)
        );
        float wisps = smoothstep(0.36, 0.92, flow * 0.72 + turbulence * 0.42);
        float polarFade = 0.42 + 0.58 * smoothstep(0.0, 0.72, cos(latitude));
        float fade = 1.0 - smoothstep(0.55, 1.0, progress);
        float shell = wisps * polarFade;
        float flash = (1.0 - smoothstep(0.0, 0.3, progress)) * (0.6 + 0.4 * turbulence);
        float layerEnergy = ImpactLayer < 0.5
                ? shell * 1.65 + flash * 0.9
                : shell * 0.85 + flash * 2.8;
        alpha = vertexColor.a * layerEnergy * fade;
        baseColor = mix(
                baseColor,
                vec3(0.88, 1.0, 0.97),
                ImpactLayer < 0.5 ? 0.72 + wisps * 0.22 : 0.88 + flash * 0.12
        );
    }

    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(baseColor, alpha);
}
