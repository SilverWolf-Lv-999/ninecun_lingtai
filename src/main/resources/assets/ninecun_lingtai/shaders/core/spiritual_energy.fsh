#version 150

in vec4 vertexColor;
in vec2 particleUv;

uniform float Time;

out vec4 fragColor;

void main() {
    vec2 point = particleUv * 2.0 - 1.0;
    float radius = length(point);
    float core = 1.0 - smoothstep(0.0, 0.24, radius);
    float body = 1.0 - smoothstep(0.18, 0.48, radius);
    float halo = 1.0 - smoothstep(0.3, 1.0, radius);
    halo *= halo;

    float pulse = 0.92 + 0.08 * sin(Time * 3.2 + particleUv.x * 7.0 + particleUv.y * 11.0);
    float alpha = vertexColor.a * (body * 0.78 + halo * 0.3) * pulse;
    if (alpha <= 0.003) {
        discard;
    }

    vec3 glowColor = mix(vertexColor.rgb, vec3(1.0), core * 0.72 + body * 0.18);
    fragColor = vec4(glowColor, alpha);
}
