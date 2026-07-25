#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 BlurDirection;
uniform float BlurScale;

out vec4 fragColor;

void main() {
    vec2 offset = oneTexel * BlurDirection * BlurScale;
    vec4 blurredColor = texture(DiffuseSampler, texCoord) * 0.087546;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 1.0) * 0.085813;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 1.0) * 0.085813;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 2.0) * 0.080813;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 2.0) * 0.080813;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 3.0) * 0.073128;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 3.0) * 0.073128;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 4.0) * 0.063566;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 4.0) * 0.063566;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 5.0) * 0.053098;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 5.0) * 0.053098;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 6.0) * 0.042616;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 6.0) * 0.042616;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 7.0) * 0.032855;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 7.0) * 0.032855;
    blurredColor += texture(DiffuseSampler, texCoord + offset * 8.0) * 0.024338;
    blurredColor += texture(DiffuseSampler, texCoord - offset * 8.0) * 0.024338;
    fragColor = blurredColor;
}
