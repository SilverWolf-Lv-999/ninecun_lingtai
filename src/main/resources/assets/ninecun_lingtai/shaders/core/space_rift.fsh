#version 150
in vec4 vertexColor;
in vec2 riftUv;
uniform float RiftTime;
uniform float RiftProgress;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p); f = f*f*(3.0-2.0*f);
    return mix(mix(hash(i), hash(i+vec2(1,0)), f.x), mix(hash(i+vec2(0,1)), hash(i+vec2(1,1)), f.x), f.y);
}
void main() {
    vec2 p = riftUv * 2.0 - 1.0;
    float edge = 1.0 - smoothstep(0.76, 1.0, abs(p.x));
    float crack = 1.0 - smoothstep(0.02, 0.28, abs(p.x + 0.10*sin(p.y*7.0 + RiftTime*2.0) * noise(p*5.0)));
    float reveal = smoothstep(0.0, 0.12, RiftProgress) * (1.0 - smoothstep(0.88, 1.0, RiftProgress));
    float alpha = (edge * 0.7 + crack) * reveal * vertexColor.a;
    if (alpha < 0.01) discard;
    vec3 color = mix(vec3(0.05, 0.08, 0.14), vec3(0.55, 0.9, 1.0), edge) + vec3(0.35, 0.55, 1.0) * crack;
    fragColor = vec4(color, alpha);
}
