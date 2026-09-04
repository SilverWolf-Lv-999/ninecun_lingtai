#version 150

in vec4 vertexColor;
in vec2 overlayUv;

uniform vec2 PanelSize;
uniform float Time;
uniform float SelectedRow;
uniform float ReleaseReady;
uniform float PageIndex;
uniform float PageCount;

out vec4 fragColor;

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float roundedBox(vec2 point, vec2 halfSize, float radius) {
    vec2 q = abs(point) - halfSize + vec2(radius);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - radius;
}

float lineBand(float value, float center, float halfWidth, float softness) {
    return 1.0 - smoothstep(halfWidth, halfWidth + softness, abs(value - center));
}

void main() {
    vec2 pixel = overlayUv * PanelSize;
    vec2 centered = pixel - PanelSize * 0.5;
    vec2 panelHalfSize = PanelSize * vec2(0.5, 0.5) - vec2(12.0, 10.0);
    float panelDistance = roundedBox(centered, panelHalfSize, 10.0);
    float panelMask = 1.0 - smoothstep(-1.2, 1.2, panelDistance);
    if (panelMask <= 0.002) {
        discard;
    }

    vec2 paperPoint = pixel * 0.035 + vec2(Time * 0.008, -Time * 0.004);
    float grain = noise(paperPoint) * 0.10 + noise(paperPoint * 3.2) * 0.035;
    vec3 paper = vec3(0.90, 0.865, 0.76) + grain;
    float warmShade = smoothstep(0.0, 1.0, overlayUv.y) * 0.035;
    paper *= vec3(1.0, 0.985 - warmShade, 0.94 - warmShade);

    float border = 1.0 - smoothstep(0.0, 2.0, abs(panelDistance + 1.0));
    vec3 ink = vec3(0.105, 0.09, 0.075);
    vec3 borderColor = mix(vec3(0.27, 0.20, 0.14), ink, 0.35 + 0.12 * sin(Time * 0.35));
    vec3 color = mix(paper, borderColor, border * 0.78);

    float contentTop = 58.0;
    float contentBottom = PanelSize.y - 42.0;
    float rowHeight = max(1.0, (contentBottom - contentTop) / 12.0);
    float row = floor((pixel.y - contentTop) / rowHeight);
    float rowInside = fract((pixel.y - contentTop) / rowHeight);
    float divider = lineBand(rowInside, 0.985, 0.008, 0.018);
    color = mix(color, vec3(0.34, 0.27, 0.19), divider * 0.34);

    float selectedMask = step(0.0, SelectedRow + 0.01)
            * (1.0 - step(SelectedRow + 1.0, row))
            * (1.0 - step(row, SelectedRow - 0.001));
    float selectedInk = selectedMask * (1.0 - smoothstep(0.08, 0.92, rowInside));
    color = mix(color, vec3(0.72, 0.57, 0.36), selectedInk * 0.20);

    // A restrained vermilion brush mark indicates that the selected row can
    // accept the single right-click release gesture. It remains shader-only.
    float releaseEdge = selectedMask * ReleaseReady * (
            lineBand(rowInside, 0.08, 0.012, 0.022)
            + lineBand(rowInside, 0.92, 0.012, 0.022)
    );
    color = mix(color, vec3(0.50, 0.12, 0.08), releaseEdge * 0.62);

    vec2 sealPoint = centered - vec2(-PanelSize.x * 0.34, -PanelSize.y * 0.34);
    float sealRadius = min(PanelSize.x, PanelSize.y) * 0.105;
    float sealDistance = abs(length(sealPoint) - sealRadius);
    float sealRing = 1.0 - smoothstep(1.0, 2.6, sealDistance);
    float sealWave = 0.5 + 0.5 * sin(Time * 0.8 + length(sealPoint) * 0.12);
    color = mix(color, vec3(0.42, 0.10, 0.08), sealRing * (0.35 + 0.20 * sealWave));

    float brushNoise = noise(pixel * 0.015 + vec2(PageIndex * 0.4, PageCount * 0.17));
    float brushStroke = smoothstep(0.70, 0.94, brushNoise) * 0.08;
    color = mix(color, ink, brushStroke);

    fragColor = vec4(color * vertexColor.rgb, panelMask * vertexColor.a * 0.97);
}
