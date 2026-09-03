#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float TransitionProgress;
uniform float TransitionTime;
uniform float InkStrength;

out vec4 fragColor;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float lowerLeft = hash21(cell);
    float lowerRight = hash21(cell + vec2(1.0, 0.0));
    float upperLeft = hash21(cell + vec2(0.0, 1.0));
    float upperRight = hash21(cell + vec2(1.0, 1.0));
    return mix(
            mix(lowerLeft, lowerRight, local.x),
            mix(upperLeft, upperRight, local.x),
            local.y
    );
}

float inkNoise(vec2 point) {
    float result = 0.0;
    float amplitude = 0.55;
    for (int octave = 0; octave < 4; octave++) {
        result += valueNoise(point) * amplitude;
        point = point * 2.03 + vec2(17.1, -11.7);
        amplitude *= 0.5;
    }
    return result;
}

void main() {
    float progress = clamp(TransitionProgress, 0.0, 1.0);
    float centerDistance = abs(progress * 2.0 - 1.0);
    float veil = 1.0 - smoothstep(0.02, 0.98, centerDistance);
    float whiteout = smoothstep(0.06, 0.62, veil);
    float timeWave = TransitionTime * 0.055;

    vec2 centered = texCoord * 2.0 - 1.0;
    centered.x *= 1.7777778;
    float radial = length(centered);
    vec2 warped = centered;
    warped += vec2(
            sin(centered.y * 7.0 + timeWave * 3.0),
            cos(centered.x * 8.0 - timeWave * 2.0)
    ) * veil * 0.012;
    warped.x /= 1.7777778;
    vec2 sampleUv = clamp(warped * 0.5 + 0.5, 0.002, 0.998);

    vec3 original = texture(DiffuseSampler, sampleUv).rgb;
    float luminance = dot(original, vec3(0.299, 0.587, 0.114));
    vec3 inkWashScene = mix(original, vec3(luminance), veil * 0.72);
    inkWashScene = mix(
            inkWashScene,
            inkWashScene * vec3(1.04, 0.99, 0.91),
            veil * 0.36
    );

    vec2 paperPoint = centered * 2.3 + vec2(timeWave, -timeWave * 0.7);
    float paperGrain = inkNoise(paperPoint * 1.7);
    float spreadingInk = inkNoise(centered * 3.2 + vec2(-timeWave * 2.0, timeWave));
    float inkEdge = smoothstep(
            0.48,
            0.93,
            spreadingInk + (1.0 - centerDistance) * 0.62
    );
    inkEdge *= veil * clamp(InkStrength, 0.0, 1.0);

    vec3 paperWhite = vec3(0.975, 0.965, 0.925);
    vec3 inkBlack = vec3(0.035, 0.042, 0.040);
    vec3 color = mix(inkWashScene, paperWhite, whiteout * 0.92);
    color = mix(color, inkBlack, inkEdge * (0.24 + paperGrain * 0.22));
    color *= 1.0 - smoothstep(0.56, 1.34, radial) * veil * 0.18;

    fragColor = vec4(color, 1.0);
}
