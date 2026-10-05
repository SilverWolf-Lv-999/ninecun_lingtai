#version 150

in vec4 vertexColor;
in vec2 lightFlightUv;
in float vertexDistance;

uniform sampler2D Sampler0;
uniform float Time;
uniform float Mode;
uniform float Boost;
uniform float TailLength;
uniform float ImpactProgress;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec4 h = fract(sin(vec4(dot(cell, vec2(127.1, 311.7)),
                              dot(cell + vec2(1.0, 0.0), vec2(127.1, 311.7)),
                              dot(cell + vec2(0.0, 1.0), vec2(127.1, 311.7)),
                              dot(cell + vec2(1.0, 1.0), vec2(127.1, 311.7)))) * 43758.5453);
    return mix(mix(h.x, h.y, f.x), mix(h.z, h.w, f.x), f.y);
}

float flow(vec2 uv) {
    float phase = fract(uv.x);
    float a = texture(Sampler0, vec2(phase, uv.y)).r;
    float b = texture(Sampler0, vec2(fract(phase + 0.5), uv.y)).r;
    float blend = smoothstep(0.0, 0.22, phase) * (1.0 - smoothstep(0.78, 1.0, phase));
    return mix(b, a, blend);
}

void main() {
    vec2 uv = lightFlightUv;
    vec3 tint = vertexColor.rgb;
    float alpha = 0.0;
    vec3 light = tint;
    if (Mode < 0.5) {
        vec2 p = uv * 2.0 - 1.0;
        float r2 = dot(p, p);
        float core = exp(-r2 * 76.0);
        float inner = exp(-r2 * 23.0);
        float halo = exp(-r2 * 4.7) * (1.0 - smoothstep(0.66, 1.0, sqrt(r2)));
        halo *= 0.84 + noise(p * 4.5 - vec2(Time * 0.9, Time * 0.35)) * 0.32;
        float pulse = 0.97 + 0.03 * sin(Time * 8.0);
        alpha = (1.0 - exp(-(core * 2.8 + inner * 0.95 + halo * 0.45))) * pulse;
        light = mix(tint * vec3(0.72, 0.96, 0.75), vec3(0.96, 1.0, 0.88),
                    smoothstep(0.05, 0.72, core + inner * 0.4));
    } else if (Mode < 1.5) {
        float u = clamp(uv.x, 0.0, 1.0);
        float crosswise = uv.y * 2.0 - 1.0;
        float distance = u * TailLength;
        float warp = sin(distance * 0.8 - Time * 1.2) * 0.045;
        float silk = flow(vec2(distance * 0.19 - Time * 0.43, uv.y + warp));
        float fine = flow(vec2(distance * 0.31 - Time * 0.61 + 0.37, 1.0 - uv.y - warp));
        float edge = 1.0 - smoothstep(0.64, 1.0, abs(crosswise));
        float body = exp(-crosswise * crosswise * 3.8);
        float fade = pow(1.0 - u, 0.3) * (1.0 - smoothstep(0.83, 1.0, u));
        alpha = (body * 0.38 + silk * 0.94 + fine * 0.38) * edge * fade;
        light = mix(tint * vec3(0.62, 0.98, 0.84), vec3(0.79, 1.0, 0.88), silk * 0.65);
    } else if (Mode < 2.5) {
        vec2 p = uv * 2.0 - 1.0;
        float r = length(p);
        float angle = atan(p.y, p.x);
        vec2 direction = vec2(cos(angle), sin(angle));
        float n = noise(p * 4.5 + vec2(0.0, ImpactProgress * 2.4));
        float detail = noise(p * 14.0 - vec2(ImpactProgress * 0.7, ImpactProgress * 3.6));
        float folds = noise(direction * 6.5 + vec2(2.7, ImpactProgress * 1.4));
        float shell = 0.48 + folds * 0.29 + (n - 0.5) * 0.15
                    + sin(angle * 19.0 + folds * 7.0 - ImpactProgress * 2.0) * 0.025;
        float signedDistance = r - shell;
        float thickness = max(0.004, fwidth(signedDistance) * 0.75);
        float rim = exp(-pow(signedDistance / thickness, 2.0));
        float breaks = smoothstep(0.19 + ImpactProgress * 0.25, 0.4 + ImpactProgress * 0.25,
                                  noise(direction * 10.0 + vec2(0.0, ImpactProgress * 2.2)));
        float veil = exp(-pow(signedDistance / 0.13, 2.0)) * (0.1 + n * 0.28);
        float ribs = pow(0.5 + 0.5 * sin(angle * 13.0 + r * 12.0 + n * 8.0), 10.0);
        float interior = ribs * smoothstep(0.16, 0.48, r) * (1.0 - smoothstep(shell - 0.1, shell, r));
        float dissolve = smoothstep(ImpactProgress * 1.2 - 0.15, ImpactProgress * 1.2 + 0.2,
                                    n * 0.65 + detail * 0.35);
        float edge = 1.0 - smoothstep(0.85, 1.0, r);
        alpha = (rim * breaks * 1.65 + veil + interior * 0.15) * dissolve * edge;
        light = mix(tint * vec3(0.52, 0.9, 0.59), vec3(0.91, 1.0, 0.88), rim * 0.96);
    } else if (Mode < 3.5) {
        float u = clamp(uv.x, 0.0, 1.0);
        float v = abs(uv.y * 2.0 - 1.0);
        float distance = u * TailLength;
        float strand = exp(-v * v * 5.5) * (1.0 - smoothstep(0.7, 1.0, v));
        float packets = 0.66 + 0.22 * sin(distance * 1.4 - Time * 3.5)
                             + 0.12 * sin(distance * 3.1 - Time * 4.7);
        float fade = pow(1.0 - u, 0.6) * (1.0 - smoothstep(0.78, 1.0, u));
        alpha = strand * packets * fade * (0.78 + Boost * 0.15);
        light = mix(tint, vec3(0.86, 1.0, 0.87), 0.58);
    } else if (Mode < 4.5) {
        vec2 p = uv * 2.0 - 1.0;
        alpha = exp(-dot(p, p) * 4.5) * (1.0 - smoothstep(0.65, 1.0, length(p)));
        light = vec3(0.8, 1.0, 0.88);
    } else if (Mode < 5.5) {
        vec2 drift = vec2(ImpactProgress * 0.12, ImpactProgress * 0.27);
        float turbulence = texture(Sampler0, uv * 0.7 + drift).g;
        vec2 distorted = uv + vec2(turbulence - 0.5, 0.0) * 0.3;
        float smoke = texture(Sampler0, clamp(distorted, 0.0, 1.0)).b;
        float edge = 1.0 - smoothstep(0.48, 1.0, length(uv * 2.0 - 1.0));
        float dissolve = 1.0 - smoothstep(0.48, 1.0, ImpactProgress);
        alpha = smoke * edge * dissolve * (0.75 + turbulence * 0.65);
        light = tint * vec3(0.48, 0.87, 0.5);
    } else {
        float angle = uv.y * 6.2831853;
        vec2 around = vec2(cos(angle), sin(angle));
        float current = noise(around * 3.2 + vec2(uv.x * 7.0 - Time * 2.6, uv.x * 2.0));
        float detail = noise(around * 6.0 + vec2(uv.x * 13.0 - Time * 4.1, 1.7));
        float silk = flow(vec2(uv.x * 0.8 - Time * 0.72,
                              0.5 + 0.43 * sin(angle + uv.x * 2.2 + current * 0.35)));
        float folds = pow(0.5 + 0.5 * sin(angle * 3.0 + uv.x * 8.0 - Time * 2.0 + current * 3.0), 7.0);
        float broken = smoothstep(0.2, 0.68, current * 0.7 + detail * 0.3);
        float ends = smoothstep(0.0, 0.16, uv.x) * (1.0 - smoothstep(0.74, 1.0, uv.x));
        alpha = (silk * 0.28 + folds * broken * 0.55) * ends;
        light = mix(tint * vec3(0.58, 0.94, 0.74), vec3(0.78, 1.0, 0.87), folds * broken);
    }
    float fog = 1.0 - smoothstep(FogStart, max(FogStart + 0.001, FogEnd), vertexDistance);
    alpha = clamp(alpha * vertexColor.a * fog, 0.0, 1.0);
    if (alpha < 0.002) {
        discard;
    }
    fragColor = vec4(light, alpha);
}
