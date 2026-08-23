#version 150

in vec4 vertexColor;
in vec2 qiUv;

uniform float QiTime;
uniform float QiProgress;
uniform float QiAngle;

out vec4 fragColor;

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    return mix(
        mix(hash(cell), hash(cell + vec2(1.0, 0.0)), local.x),
        mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), local.x),
        local.y
    );
}

void main() {
    float progress = qiUv.y;
    float side = abs(qiUv.x * 2.0 - 1.0);
    float edge = 1.0 - smoothstep(0.38, 1.0, side);
    float core = 1.0 - smoothstep(0.03, 0.48, side);
    float reveal = 1.0 - smoothstep(QiProgress - 0.22, QiProgress + 0.015, progress);
    float head = smoothstep(QiProgress - 0.18, QiProgress, progress);
    float grain = noise(vec2(progress * 15.0, side * 5.0 + QiTime * 2.5));
    float anglePulse = 0.88 + 0.12 * cos(radians(QiAngle) * 2.0 + QiTime * 5.0);
    float fade = smoothstep(0.0, 0.09, progress) * (1.0 - smoothstep(0.91, 1.0, progress));

    vec3 azure = vec3(0.20, 0.70, 1.0);
    vec3 white = vec3(0.92, 0.99, 1.0);
    vec3 color = mix(azure, white, core * 0.84 + head * 0.16);
    float alpha = vertexColor.a * fade * reveal * (edge * 0.82 + core * 0.72) * anglePulse;
    alpha *= 0.84 + grain * 0.20;
    if (alpha <= 0.003) {
        discard;
    }
    fragColor = vec4(color * (1.0 + core * 0.42), alpha);
}
