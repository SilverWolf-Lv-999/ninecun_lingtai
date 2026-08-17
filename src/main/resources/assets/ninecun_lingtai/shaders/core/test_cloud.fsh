#version 150

// Adapted for the Minecraft rendering pipeline from the volumetric-cloud
// approach referenced by the user: https://www.shadertoy.com/view/ll3SWl

in vec4 vertexColor;
in vec2 cloudUv;

uniform float CloudTime;
uniform float CloudSeed;

out vec4 fragColor;

const int STEP_COUNT = 32;
const vec3 LIGHT_DIRECTION = normalize(vec3(-0.45, 0.62, -0.58));

float hash(vec3 point) {
    return fract(sin(dot(point, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

float noise(vec3 point) {
    vec3 cell = floor(point);
    vec3 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);

    float lowerFrontLeft = hash(cell);
    float lowerFrontRight = hash(cell + vec3(1.0, 0.0, 0.0));
    float lowerBackLeft = hash(cell + vec3(0.0, 0.0, 1.0));
    float lowerBackRight = hash(cell + vec3(1.0, 0.0, 1.0));
    float upperFrontLeft = hash(cell + vec3(0.0, 1.0, 0.0));
    float upperFrontRight = hash(cell + vec3(1.0, 1.0, 0.0));
    float upperBackLeft = hash(cell + vec3(0.0, 1.0, 1.0));
    float upperBackRight = hash(cell + vec3(1.0, 1.0, 1.0));

    float lowerFront = mix(lowerFrontLeft, lowerFrontRight, local.x);
    float lowerBack = mix(lowerBackLeft, lowerBackRight, local.x);
    float upperFront = mix(upperFrontLeft, upperFrontRight, local.x);
    float upperBack = mix(upperBackLeft, upperBackRight, local.x);
    return mix(mix(lowerFront, lowerBack, local.z), mix(upperFront, upperBack, local.z), local.y);
}

float fractalNoise(vec3 point) {
    float value = 0.0;
    float amplitude = 0.5;
    mat3 rotation = mat3(
            0.00, 0.80, 0.60,
            -0.80, 0.36, -0.48,
            -0.60, -0.48, 0.64
    );
    for (int octave = 0; octave < 5; octave++) {
        value += noise(point) * amplitude;
        point = rotation * point * 2.03 + vec3(3.1, -1.7, 2.4);
        amplitude *= 0.5;
    }
    return value;
}

float cloudDensity(vec3 point) {
    vec3 wind = vec3(CloudTime * 0.052, CloudTime * 0.012, CloudTime * 0.031);
    float radialMask = 1.0 - smoothstep(0.43, 1.04, length(point));
    float broadShape = fractalNoise(point * 1.24 + wind + CloudSeed * 19.0);
    float detailShape = fractalNoise(point * 3.05 - wind * 1.7 + CloudSeed * 43.0);
    float shape = smoothstep(0.40, 0.70, broadShape * 0.72 + detailShape * 0.28);
    return shape * radialMask;
}

bool intersectUnitSphere(vec3 rayOrigin, vec3 rayDirection, out float nearDistance, out float farDistance) {
    float projectedDistance = dot(rayOrigin, rayDirection);
    float closestDistanceSquared = dot(rayOrigin, rayOrigin) - projectedDistance * projectedDistance;
    float halfChordSquared = 1.0 - closestDistanceSquared;
    if (halfChordSquared <= 0.0) {
        return false;
    }
    float halfChord = sqrt(halfChordSquared);
    nearDistance = -projectedDistance - halfChord;
    farDistance = -projectedDistance + halfChord;
    return farDistance > 0.0;
}

void main() {
    vec2 cloudPoint = cloudUv * 2.0 - 1.0;
    vec3 rayOrigin = vec3(cloudPoint * 0.10, -1.48);
    vec3 rayDirection = normalize(vec3(cloudPoint * 0.18, 1.0));
    float nearDistance;
    float farDistance;
    if (!intersectUnitSphere(rayOrigin, rayDirection, nearDistance, farDistance)) {
        discard;
    }

    nearDistance = max(nearDistance, 0.0);
    float stepLength = (farDistance - nearDistance) / float(STEP_COUNT);
    float jitter = hash(vec3(cloudPoint, CloudSeed)) * stepLength;
    float transmittance = 1.0;
    vec3 accumulatedColor = vec3(0.0);

    for (int step = 0; step < STEP_COUNT; step++) {
        float distanceAlongRay = nearDistance + jitter + (float(step) + 0.5) * stepLength;
        vec3 samplePoint = rayOrigin + rayDirection * distanceAlongRay;
        float density = cloudDensity(samplePoint);
        if (density <= 0.001) {
            continue;
        }

        float illumination = 0.40 + 0.60 * max(dot(normalize(samplePoint), LIGHT_DIRECTION), 0.0);
        vec3 shadowColor = vec3(0.20, 0.28, 0.38);
        vec3 sunlitColor = vec3(0.90, 0.94, 0.98);
        vec3 sampleColor = mix(shadowColor, sunlitColor, illumination);
        float sampleAlpha = 1.0 - exp(-density * stepLength * 2.35);
        accumulatedColor += transmittance * sampleColor * sampleAlpha;
        transmittance *= 1.0 - sampleAlpha;
        if (transmittance < 0.015) {
            break;
        }
    }

    float alpha = (1.0 - transmittance) * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    fragColor = vec4(accumulatedColor / alpha * vertexColor.rgb, alpha);
}
