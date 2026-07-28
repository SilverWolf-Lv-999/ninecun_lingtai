#version 150

in vec4 vertexColor;
in vec2 cloudUv;

uniform float CloudTime;
uniform float CloudProgress;
uniform float Darkness;
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

void main() {
    vec2 safeSize = max(ScreenSize, vec2(1.0));
    vec2 point = cloudUv * 2.0 - 1.0;
    point.x *= safeSize.x / safeSize.y;
    float time = CloudTime * 0.045;
    float broadNoise = noise(point * 2.1 + vec2(time, -time * 0.8));
    float detailNoise = noise(point * 7.0 - vec2(time * 1.7, time));
    float cloud = smoothstep(0.24, 0.76, broadNoise * 0.72 + detailNoise * 0.28);
    float pulse = 0.94 + 0.06 * sin(CloudTime * 0.7 + detailNoise * 5.0);
    vec3 cloudColor = mix(vec3(0.045, 0.065, 0.12), vec3(0.008, 0.012, 0.028), cloud);
    float alpha = Darkness * vertexColor.a * (0.38 + cloud * 0.44) * pulse;
    fragColor = vec4(cloudColor, clamp(alpha, 0.0, 0.82));
}
