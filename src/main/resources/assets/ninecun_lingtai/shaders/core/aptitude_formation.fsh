#version 150

in vec4 vertexColor;
in vec2 formationUv;

uniform float FormationTime;

out vec4 fragColor;

float lineMask(float value, float width) {
    return 1.0 - smoothstep(width, width + 0.008, abs(value));
}

float ringMask(float radius, float target, float width) {
    return lineMask(radius - target, width);
}

void main() {
    vec2 point = formationUv * 2.0 - 1.0;
    float radius = length(point);
    if (radius > 1.0) {
        discard;
    }

    float angle = atan(point.y, point.x);
    float edgeFade = 1.0 - smoothstep(0.88, 1.0, radius);
    float discAlpha = 1.0 - smoothstep(0.76, 1.0, radius);
    float centerShade = 1.0 - smoothstep(0.0, 0.82, radius);
    vec3 color = mix(vec3(0.018, 0.026, 0.042), vec3(0.045, 0.065, 0.086), centerShade);

    float outerRing = ringMask(radius, 0.925, 0.006);
    float middleRing = ringMask(radius, 0.735, 0.004);
    float innerRing = ringMask(radius, 0.43, 0.003);
    float rotatingDash = ringMask(radius, 0.835, 0.008)
            * smoothstep(0.48, 0.58, sin(angle * 24.0 - FormationTime * 0.22));
    float counterDash = ringMask(radius, 0.56, 0.006)
            * smoothstep(0.5, 0.62, sin(angle * 15.0 + FormationTime * 0.16));
    float spokeDistance = abs(sin(angle * 10.0));
    float shortSpokes = (1.0 - smoothstep(0.0, 0.025, spokeDistance))
            * smoothstep(0.48, 0.58, radius)
            * (1.0 - smoothstep(0.69, 0.78, radius));
    float runeBand = ringMask(radius, 0.66, 0.018)
            * smoothstep(0.55, 0.78, sin(angle * 40.0 + sin(angle * 5.0) * 2.0));

    vec3 bronze = vec3(0.63, 0.49, 0.25);
    vec3 blueGray = vec3(0.26, 0.39, 0.52);
    color += bronze * (outerRing * 0.72 + rotatingDash * 0.34 + runeBand * 0.2);
    color += blueGray * (middleRing * 0.44 + innerRing * 0.26 + counterDash * 0.28 + shortSpokes * 0.24);

    float vignette = mix(0.88, 0.38, smoothstep(0.48, 1.0, radius));
    float alpha = vertexColor.a * edgeFade * vignette * (0.78 + discAlpha * 0.22);
    fragColor = vec4(color, alpha);
}
