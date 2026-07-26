#version 150

in vec4 vertexColor;
in vec2 cloudUv;

uniform float CloudTime;
uniform float CloudProgress;
uniform vec2 ScreenSize;

out vec4 fragColor;

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float lowerLeft = hash(cell);
    float lowerRight = hash(cell + vec2(1.0, 0.0));
    float upperLeft = hash(cell + vec2(0.0, 1.0));
    float upperRight = hash(cell + vec2(1.0, 1.0));
    return mix(mix(lowerLeft, lowerRight, local.x), mix(upperLeft, upperRight, local.x), local.y);
}

float cloudNoise(vec2 point) {
    float value = 0.0;
    float amplitude = 0.54;
    mat2 rotation = mat2(0.82, -0.57, 0.57, 0.82);
    for (int octave = 0; octave < 5; octave++) {
        value += noise(point) * amplitude;
        point = rotation * point * 2.03 + vec2(3.1, 1.7);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 safeSize = max(ScreenSize, vec2(1.0));
    float aspect = safeSize.x / safeSize.y;
    vec2 point = cloudUv * 2.0 - 1.0;
    point.x *= aspect;

    float time = CloudTime * 0.035;
    float broadNoise = cloudNoise(point * 1.45 + vec2(time, -time * 0.7));
    float detailNoise = cloudNoise(point * 3.6 - vec2(time * 1.6, time));
    float edgeNoise = (broadNoise - 0.5) * 0.34 + (detailNoise - 0.5) * 0.12;
    float maximumRadius = sqrt(aspect * aspect + 1.0);
    float revealRadius = mix(0.035, maximumRadius + 0.34, smoothstep(0.0, 1.0, CloudProgress));
    float distanceFromCenter = length(point) + edgeNoise * (0.4 + CloudProgress * 0.6);
    float revealMask = 1.0 - smoothstep(revealRadius - 0.2, revealRadius + 0.12, distanceFromCenter);

    float centerGlow = 1.0 - smoothstep(0.0, maximumRadius, length(point));
    float vapor = smoothstep(0.18, 0.9, broadNoise * 0.72 + detailNoise * 0.38);
    vec3 warmMilk = vec3(0.955, 0.94, 0.86);
    vec3 coolMist = vec3(0.82, 0.88, 0.84);
    vec3 cloudColor = mix(coolMist, warmMilk, 0.46 + centerGlow * 0.34 + vapor * 0.2);
    float alpha = revealMask * (0.79 + vapor * 0.17 + centerGlow * 0.05) * vertexColor.a;
    fragColor = vec4(cloudColor * vertexColor.rgb, clamp(alpha, 0.0, 0.97));
}
