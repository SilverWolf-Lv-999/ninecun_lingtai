#version 150

in vec4 vertexColor;
in vec2 particleUv;

uniform float Time;
uniform float ElementStyle;

out vec4 fragColor;

float softShape(float distanceValue, float innerEdge, float outerEdge) {
    return 1.0 - smoothstep(innerEdge, outerEdge, distanceValue);
}

void main() {
    vec2 point = particleUv * 2.0 - 1.0;
    float radius = length(point);
    float halo = softShape(radius, 0.28, 1.0);
    halo *= halo;
    float body = 0.0;
    float core = 0.0;
    vec3 energyColor = vertexColor.rgb;

    if (ElementStyle < 0.5) {
        float diamond = abs(point.x) + abs(point.y);
        body = softShape(diamond, 0.34, 0.62);
        float horizontalRay = softShape(abs(point.y), 0.025, 0.09)
                * softShape(abs(point.x), 0.42, 0.98);
        float verticalRay = softShape(abs(point.x), 0.025, 0.09)
                * softShape(abs(point.y), 0.42, 0.98);
        body = max(body, max(horizontalRay, verticalRay) * 0.72);
        core = softShape(diamond, 0.08, 0.3);
        energyColor = mix(energyColor, vec3(1.0, 0.98, 0.82), core * 0.82);
    } else if (ElementStyle < 1.5) {
        vec2 leafPoint = vec2(point.x * 0.76, point.y * 1.32);
        float leafDistance = length(leafPoint) + abs(leafPoint.x) * 0.34;
        body = softShape(leafDistance, 0.38, 0.7);
        float vein = softShape(abs(point.x + point.y * 0.16), 0.018, 0.065)
                * softShape(abs(point.y), 0.32, 0.82);
        core = body * vein;
        energyColor = mix(energyColor, vec3(0.78, 1.0, 0.58), core * 0.64);
    } else if (ElementStyle < 2.5) {
        vec2 dropPoint = point;
        dropPoint.y += 0.12;
        float roundedDrop = length(vec2(dropPoint.x * 1.08, dropPoint.y * 0.9));
        float pointedTop = abs(dropPoint.x) + max(0.0, dropPoint.y) * 0.72;
        body = max(
                softShape(roundedDrop, 0.4, 0.68),
                softShape(pointedTop, 0.18, 0.48) * smoothstep(-0.05, 0.7, dropPoint.y)
        );
        float ripple = softShape(abs(radius - 0.52), 0.015, 0.07) * 0.45;
        body = max(body, ripple);
        core = softShape(roundedDrop, 0.08, 0.32);
        energyColor = mix(energyColor, vec3(0.78, 0.94, 1.0), core * 0.8);
    } else if (ElementStyle < 3.5) {
        vec2 flamePoint = point;
        flamePoint.x += sin(flamePoint.y * 5.4 + Time * 7.0) * (flamePoint.y + 1.0) * 0.055;
        float flameWidth = mix(0.5, 0.07, clamp((flamePoint.y + 0.75) / 1.6, 0.0, 1.0));
        float flameBody = softShape(abs(flamePoint.x), flameWidth, flameWidth + 0.14);
        float flameHeight = smoothstep(-0.96, -0.6, flamePoint.y)
                * (1.0 - smoothstep(0.58, 0.96, flamePoint.y));
        body = flameBody * flameHeight;
        core = softShape(abs(flamePoint.x), flameWidth * 0.24, flameWidth * 0.52)
                * flameHeight;
        energyColor = mix(energyColor, vec3(1.0, 0.9, 0.28), core * 0.88);
    } else {
        vec2 rockPoint = point;
        float hexagon = max(abs(rockPoint.y), abs(rockPoint.x) * 0.866 + abs(rockPoint.y) * 0.5);
        body = softShape(hexagon, 0.38, 0.62);
        float fault = softShape(abs(rockPoint.x * 0.72 + rockPoint.y * 0.34), 0.02, 0.075)
                * body;
        core = softShape(hexagon, 0.08, 0.3);
        energyColor = mix(energyColor, vec3(1.0, 0.74, 0.34), core * 0.56 + fault * 0.25);
    }

    float flicker = 0.84 + 0.16 * sin(Time * 5.0 + particleUv.x * 9.0 + particleUv.y * 13.0);
    float alpha = vertexColor.a * (body * 0.9 + halo * 0.34) * flicker;
    energyColor = mix(energyColor, vec3(1.0), core * 0.28 + halo * 0.12);
    if (alpha <= 0.003) {
        discard;
    }
    fragColor = vec4(energyColor, alpha);
}
