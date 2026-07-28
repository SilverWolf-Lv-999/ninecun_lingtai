#version 150

in vec4 vertexColor;
in vec2 scrollUv;

uniform vec2 PanelSize;
uniform float SlatCount;
uniform float ViewAngleDegrees;
uniform float UnrollProgress;
uniform float Opacity;

out vec4 fragColor;

const float ROLLER_WIDTH = 24.0;
const float ROLLER_OVERLAP = 3.0;

float hash(float value) {
    return fract(sin(value * 127.1) * 43758.5453);
}

float roundedBoxDistance(vec2 point, vec2 halfSize, float radius) {
    vec2 distanceToEdge = abs(point) - halfSize + vec2(radius);
    return min(max(distanceToEdge.x, distanceToEdge.y), 0.0)
            + length(max(distanceToEdge, vec2(0.0)))
            - radius;
}

float fillMask(float signedDistance, float softness) {
    return 1.0 - smoothstep(-softness, softness, signedDistance);
}

float lineBand(float value, float center, float halfWidth, float softness) {
    return 1.0 - smoothstep(halfWidth, halfWidth + softness, abs(value - center));
}

void main() {
    float opacity = clamp(Opacity, 0.0, 1.0) * vertexColor.a;
    if (opacity <= 0.001) {
        discard;
    }

    vec2 screenPoint = scrollUv * PanelSize;
    float centerX = PanelSize.x * 0.5;
    float tilt = cos(radians(clamp(ViewAngleDegrees, 70.0, 89.0)));
    float topPerspectiveScale = 1.0 - tilt * 0.28;
    float perspectiveScale = mix(topPerspectiveScale, 1.0, scrollUv.y);
    vec2 point = vec2(
            centerX + (screenPoint.x - centerX) / perspectiveScale,
            screenPoint.y
    );
    float openPaperWidth = max(1.0, PanelSize.x - ROLLER_WIDTH * 2.0);
    float paperHalfWidth = openPaperWidth * 0.5 * clamp(UnrollProgress, 0.0, 1.0);
    float fullPaperLeft = centerX - openPaperWidth * 0.5;
    float paperLeft = centerX - paperHalfWidth;
    float paperRight = centerX + paperHalfWidth;

    float slatWidth = openPaperWidth / max(1.0, SlatCount);
    float slatCoordinate = (point.x - fullPaperLeft) / slatWidth;
    float slatIndex = floor(slatCoordinate);
    float slatFraction = fract(slatCoordinate);
    float topVariation = (hash(slatIndex + 2.3) - 0.5) * 2.2;
    float bottomVariation = (hash(slatIndex + 9.7) - 0.5) * 2.2;
    float paperTop = 7.0 + topVariation;
    float paperBottom = PanelSize.y - 8.0 + bottomVariation;
    float paperHorizontalMask = smoothstep(paperLeft - 0.8, paperLeft + 0.8, point.x)
            * (1.0 - smoothstep(paperRight - 0.8, paperRight + 0.8, point.x))
            * step(0.001, UnrollProgress);
    float paperVerticalMask = smoothstep(paperTop - 0.8, paperTop + 0.8, point.y)
            * (1.0 - smoothstep(paperBottom - 0.8, paperBottom + 0.8, point.y));
    float paperMask = paperHorizontalMask * paperVerticalMask;

    float verticalPosition = clamp((point.y - paperTop) / max(1.0, paperBottom - paperTop), 0.0, 1.0);
    float edgeLight = smoothstep(0.0, 0.12, verticalPosition)
            * smoothstep(0.0, 0.12, 1.0 - verticalPosition);
    float curvedSlat = 0.72 + 0.28 * sin(slatFraction * 3.14159265);
    float seam = 1.0 - smoothstep(0.025, 0.1, min(slatFraction, 1.0 - slatFraction));
    float firstNode = 0.34 + (hash(slatIndex + 17.0) - 0.5) * 0.035;
    float secondNode = 0.69 + (hash(slatIndex + 31.0) - 0.5) * 0.035;
    float nodeBand = max(
            lineBand(verticalPosition, firstNode, 0.006, 0.012),
            lineBand(verticalPosition, secondNode, 0.006, 0.012)
    );
    vec3 paperColor = mix(vec3(0.80, 0.70, 0.51), vec3(0.975, 0.945, 0.83), edgeLight);
    paperColor *= curvedSlat;
    paperColor = mix(paperColor, vec3(0.55, 0.40, 0.23), seam * 0.34);
    paperColor = mix(paperColor, vec3(0.72, 0.57, 0.35), nodeBand * 0.23);
    float nearPaperEdge = lineBand(verticalPosition, 0.985, 0.01, 0.018);
    paperColor = mix(paperColor, vec3(0.58, 0.41, 0.22), nearPaperEdge * tilt * 1.8);

    float rollerHalfWidth = ROLLER_WIDTH * 0.5;
    float leftRollerCenter = centerX - paperHalfWidth - rollerHalfWidth + ROLLER_OVERLAP;
    float rightRollerCenter = centerX + paperHalfWidth + rollerHalfWidth - ROLLER_OVERLAP;
    vec2 rollerHalfSize = vec2(rollerHalfWidth - 2.0, PanelSize.y * 0.5 - 6.0);
    float rollerRadius = 2.4;
    float rollerCenterY = PanelSize.y * 0.5 - 1.0;
    float leftDistance = roundedBoxDistance(
            point - vec2(leftRollerCenter, rollerCenterY),
            rollerHalfSize,
            rollerRadius
    );
    float rightDistance = roundedBoxDistance(
            point - vec2(rightRollerCenter, rollerCenterY),
            rollerHalfSize,
            rollerRadius
    );
    float leftMask = fillMask(leftDistance, 0.85);
    float rightMask = fillMask(rightDistance, 0.85);
    float rollerMask = max(leftMask, rightMask);
    bool useLeftRoller = leftMask >= rightMask;
    float rollerCenter = useLeftRoller ? leftRollerCenter : rightRollerCenter;
    float rollerDistance = useLeftRoller ? leftDistance : rightDistance;
    float localRollerX = clamp((point.x - rollerCenter) / rollerHalfWidth, -1.0, 1.0);
    float rollerCurve = sqrt(max(0.0, 1.0 - localRollerX * localRollerX));
    float rollerVertical = point.y / PanelSize.y;
    vec3 rollerColor = mix(vec3(0.55, 0.39, 0.22), vec3(0.965, 0.91, 0.75), rollerCurve);
    float rolledLayerLines = max(
            lineBand(abs(localRollerX), 0.52, 0.035, 0.045),
            lineBand(abs(localRollerX), 0.79, 0.025, 0.04)
    );
    rollerColor = mix(rollerColor, vec3(0.64, 0.47, 0.27), rolledLayerLines * 0.28);
    float rollerBands = max(
            lineBand(rollerVertical, 0.225, 0.012, 0.012),
            lineBand(rollerVertical, 0.775, 0.012, 0.012)
    );
    rollerColor = mix(rollerColor, vec3(0.49, 0.33, 0.17), rollerBands * 0.62);
    float outline = fillMask(rollerDistance, 0.9)
            - fillMask(rollerDistance + 1.5, 0.9);
    rollerColor = mix(rollerColor, vec3(0.38, 0.25, 0.13), clamp(outline, 0.0, 1.0));

    float nearCapCenterY = rollerCenterY + rollerHalfSize.y;
    float capHalfHeight = max(1.2, rollerHalfWidth * tilt);
    float nearCapRadius = length(vec2(
            (point.x - rollerCenter) / (rollerHalfWidth - 2.4),
            (point.y - nearCapCenterY) / capHalfHeight
    ));
    float nearCapMask = 1.0 - smoothstep(0.94, 1.04, nearCapRadius);
    float nearCapRing = lineBand(nearCapRadius, 0.72, 0.055, 0.055);
    vec3 nearCapColor = mix(vec3(0.58, 0.41, 0.22), vec3(0.88, 0.78, 0.58), nearCapRing);
    rollerColor = mix(rollerColor, nearCapColor, nearCapMask);
    rollerMask = max(rollerMask, nearCapMask);

    float shapeMask = max(paperMask, rollerMask);
    if (shapeMask <= 0.002) {
        discard;
    }
    vec3 finalColor = mix(paperColor, rollerColor, rollerMask);
    fragColor = vec4(finalColor * vertexColor.rgb, shapeMask * opacity);
}
