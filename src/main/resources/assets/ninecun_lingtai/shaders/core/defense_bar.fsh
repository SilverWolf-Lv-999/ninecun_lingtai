#version 150

in vec4 vertexColor;
in vec2 barUv;

uniform float Time;
uniform float Coverage;
uniform float ElementMode;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;

out vec4 fragColor;

float hash(vec2 value) {
    return fract(sin(dot(value, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 value) {
    vec2 base = floor(value);
    vec2 fraction = fract(value);
    fraction = fraction * fraction * (3.0 - 2.0 * fraction);
    float a = hash(base);
    float b = hash(base + vec2(1.0, 0.0));
    float c = hash(base + vec2(0.0, 1.0));
    float d = hash(base + vec2(1.0, 1.0));
    return mix(mix(a, b, fraction.x), mix(c, d, fraction.x), fraction.y);
}

float softBand(float value, float width, float softness) {
    return 1.0 - smoothstep(width, width + softness, abs(value));
}

void main() {
    float coverage = clamp(Coverage, 0.0, 1.0);
    if (coverage <= 0.001) {
        discard;
    }

    vec2 uv = barUv;
    float fillStart = 1.0 - coverage;
    float fillMask = coverage >= 0.999
            ? 1.0
            : smoothstep(fillStart - 0.012, fillStart + 0.012, uv.y);
    float edgeMask = smoothstep(0.0, 0.12, uv.x)
            * smoothstep(0.0, 0.12, 1.0 - uv.x)
            * smoothstep(0.0, 0.02, uv.y)
            * smoothstep(0.0, 0.02, 1.0 - uv.y);
    float side = uv.x * 2.0 - 1.0;
    vec3 color = PrimaryColor;
    float pattern = 0.72;
    float glow = 0.0;

    if (ElementMode < 0.5) {
        float facet = 1.0 - smoothstep(0.0, 0.13, abs(fract(uv.y * 7.0 + uv.x * 1.8 - Time * 0.36) * 2.0 - 1.0));
        float ridge = softBand(side + sin(uv.y * 13.0 + Time * 0.7) * 0.18, 0.045, 0.055);
        color = mix(vec3(0.58, 0.58, 0.55), PrimaryColor, 0.64);
        color = mix(color, SecondaryColor, facet * 0.58 + ridge * 0.18);
        pattern = 0.66 + facet * 0.32 + ridge * 0.18;
        glow = facet * 0.34;
    } else if (ElementMode < 1.5) {
        float trunkCenter = sin(uv.y * 8.0 + Time * 0.7) * 0.12;
        float trunk = softBand(side - trunkCenter, 0.11, 0.1);
        float branchPhase = fract(uv.y * 5.2 - Time * 0.16);
        float branchSide = step(0.5, fract(floor(uv.y * 5.2) * 0.37)) * 2.0 - 1.0;
        float branchCenter = trunkCenter + branchSide * (branchPhase * 0.7 - 0.05);
        float branch = softBand(side - branchCenter, 0.045, 0.065)
                * smoothstep(0.12, 0.42, branchPhase)
                * (1.0 - smoothstep(0.58, 0.92, branchPhase));
        float leaf = 1.0 - smoothstep(0.16, 0.32, length(vec2(side - branchCenter, branchPhase - 0.55)));
        pattern = max(0.5, max(trunk, max(branch, leaf * 0.8)));
        color = mix(vec3(0.18, 0.32, 0.16), PrimaryColor, trunk * 0.7 + 0.2);
        color = mix(color, SecondaryColor, max(branch, leaf) * 0.46);
        glow = leaf * 0.18;
    } else if (ElementMode < 2.5) {
        float waveA = sin(uv.y * 19.0 - Time * 3.4 + uv.x * 4.8) * 0.18;
        float waveB = sin(uv.y * 28.0 - Time * 4.6 - uv.x * 3.0) * 0.08;
        float stream = softBand(side - waveA, 0.12, 0.12);
        float ripple = softBand(fract(uv.y * 9.0 - Time * 1.3 + waveB) - 0.5, 0.06, 0.16);
        color = mix(PrimaryColor, SecondaryColor, stream * 0.42 + ripple * 0.28);
        pattern = 0.62 + stream * 0.28 + ripple * 0.22;
        glow = stream * 0.18 + ripple * 0.12;
    } else if (ElementMode < 3.5) {
        float flameNoise = noise(vec2(uv.x * 4.0, uv.y * 10.0 - Time * 2.8));
        float flameCenter = sin(uv.y * 14.0 - Time * 4.2) * 0.18 + (flameNoise - 0.5) * 0.22;
        float flameTaper = pow(max(0.0, 1.0 - uv.y), 0.85);
        float flame = 1.0 - smoothstep(0.28 + flameTaper * 0.12, 0.66, abs(side - flameCenter));
        float ember = noise(vec2(uv.x * 14.0, uv.y * 24.0 - Time * 9.0));
        color = mix(PrimaryColor, SecondaryColor, flame * 0.58 + ember * 0.16);
        pattern = 0.58 + flame * 0.4 + ember * 0.16;
        glow = flame * 0.34 + ember * 0.12;
    } else if (ElementMode < 4.5) {
        float layerValue = floor(uv.y * 11.0 + noise(vec2(Time * 0.08, uv.x * 3.0)) * 0.6);
        float layerBlend = hash(vec2(layerValue, 3.0));
        float seam = softBand(fract(uv.y * 11.0 + layerBlend * 0.2) - 0.5, 0.025, 0.035);
        float crack = softBand(side + sin(uv.y * 17.0 + layerBlend * 6.0) * 0.28, 0.022, 0.05)
                * step(0.62, layerBlend);
        color = mix(PrimaryColor, SecondaryColor, layerBlend * 0.42 + seam * 0.22);
        pattern = 0.8 - seam * 0.18 - crack * 0.16;
        glow = seam * 0.04;
    } else if (ElementMode < 5.5) {
        float divider = 0.5 + sin(uv.y * 7.0 + Time * 0.82) * 0.16
                + sin(uv.y * 17.0 - Time * 0.48) * 0.04;
        float lightSide = smoothstep(divider - 0.05, divider + 0.05, uv.x);
        float weave = softBand(uv.x - divider, 0.055, 0.08);
        color = mix(SecondaryColor, PrimaryColor, lightSide);
        color = mix(color, vec3(0.62, 0.66, 0.76), weave * 0.24);
        pattern = 0.78 + weave * 0.24;
        glow = weave * 0.2;
    } else {
        float fogA = noise(uv * vec2(4.0, 11.0) + vec2(Time * 0.18, -Time * 0.42));
        float fogB = noise(uv * vec2(9.0, 19.0) + vec2(-Time * 0.3, Time * 0.21));
        float fog = fogA * 0.62 + fogB * 0.38;
        float wisp = softBand(side + sin(uv.y * 10.0 + Time * 1.4 + fog * 3.0) * 0.32, 0.18, 0.18);
        color = mix(SecondaryColor, PrimaryColor, fog);
        color = mix(color, vec3(0.92, 0.72, 1.0), wisp * 0.36);
        pattern = 0.54 + fog * 0.32 + wisp * 0.24;
        glow = wisp * 0.28 + fog * 0.12;
    }

    float boundary = coverage > 0.02 && coverage < 0.98
            ? 1.0 - smoothstep(0.0, 0.022, abs(uv.y - fillStart))
            : 0.0;
    color = mix(color, vec3(1.0), min(0.72, glow + boundary * 0.48));
    float alpha = vertexColor.a * fillMask * edgeMask * clamp(pattern + boundary * 0.36, 0.0, 1.25);
    if (alpha <= 0.003) {
        discard;
    }
    fragColor = vec4(color, alpha);
}
