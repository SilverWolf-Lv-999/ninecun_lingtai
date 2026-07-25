#version 150

in vec4 vertexColor;
in vec2 particleUv;

uniform float Time;
uniform float ElementStyle;

out vec4 fragColor;

float softShape(float distanceValue, float innerEdge, float outerEdge) {
    return 1.0 - smoothstep(innerEdge, outerEdge, distanceValue);
}

float flameLobe(
        vec2 point,
        vec2 basePoint,
        vec2 tipPoint,
        float maximumWidth,
        float edgeSoftness
) {
    vec2 axis = tipPoint - basePoint;
    float axisLength = length(axis);
    vec2 direction = axis / axisLength;
    vec2 normal = vec2(-direction.y, direction.x);
    vec2 relativePoint = point - basePoint;
    float progress = dot(relativePoint, direction) / axisLength;
    float clampedProgress = clamp(progress, 0.0, 1.0);
    float widthEnvelope = pow(max(sin(clampedProgress * 3.14159265), 0.0), 1.05);
    float halfWidth = maximumWidth * widthEnvelope;
    float sideDistance = abs(dot(relativePoint, normal));
    float heightMask = smoothstep(-0.015, 0.045, progress)
            * (1.0 - smoothstep(0.99, 1.0, progress));
    return softShape(sideDistance, halfWidth, halfWidth + edgeSoftness) * heightMask;
}

void main() {
    vec2 point = particleUv * 2.0 - 1.0;
    float radius = length(point);
    float halo = softShape(radius, 0.28, 1.0);
    halo *= halo;
    float haloStrength = 0.34;
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
        float mainSway = sin(Time * 7.0) * 0.055;
        float sideSway = sin(Time * 8.6 + 1.7) * 0.04;
        float mainFlame = flameLobe(
                point,
                vec2(0.0, 0.94),
                vec2(0.1 + mainSway, -0.98),
                0.45,
                0.055
        );
        float leftTongue = flameLobe(
                point,
                vec2(-0.02, 0.72),
                vec2(-0.5 + sideSway, -0.18),
                0.19,
                0.05
        );
        body = max(mainFlame, leftTongue);

        float innerFlame = flameLobe(
                point,
                vec2(0.02, 0.78),
                vec2(-0.12 - mainSway * 0.45, -0.48),
                0.2,
                0.04
        );
        float innerTongue = flameLobe(
                point,
                vec2(0.08, 0.72),
                vec2(0.28 + sideSway * 0.35, 0.02),
                0.1,
                0.035
        );
        core = max(innerFlame, innerTongue) * body;

        vec2 flameHaloPoint = vec2(point.x * 1.24, (point.y + 0.08) * 0.82);
        halo = softShape(length(flameHaloPoint), 0.22, 0.92);
        halo *= halo;
        haloStrength = 0.16;
        energyColor = mix(energyColor, vec3(1.0, 0.46, 0.06), body * 0.58);
        energyColor = mix(energyColor, vec3(1.0, 0.92, 0.3), core * 0.92);
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
    float alpha = vertexColor.a * (body * 0.9 + halo * haloStrength) * flicker;
    energyColor = mix(energyColor, vec3(1.0), core * 0.28 + halo * 0.12);
    if (alpha <= 0.003) {
        discard;
    }
    fragColor = vec4(energyColor, alpha);
}
