#version 150

uniform sampler2D Sampler0;
uniform sampler2D Texture0;
uniform sampler2D ColorTexture0;
uniform sampler2D MainTarget0;

uniform vec2 screenSize;
uniform float time;

in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main()
{
    float angle0 = texCoord0.x * 2.0 * 3.14159265 + time * 0.5 * 1.5;
    vec2 texture0UV = vec2(fract(angle0 / (2.0 * 3.14159265)), texCoord0.y);

    vec4 texture0Color = texture(Texture0, texture0UV);
    float texture0Brightness = dot(texture0Color.rgb, vec3(0.299, 0.587, 0.114));

    vec2 distort = vec2(
    noise(texCoord0 * 3.0 + time * 0.2),
    noise(texCoord0 * 3.0 + time * 0.3 + 100.0)
    ) * 0.1 - 0.05;

    distort += (texture0Brightness - 0.5) * 0.1;

    float angle = texCoord0.x * 2.0 * 3.14159265 + time * 0.5;
    vec2 rotatedUV = vec2(fract(angle / (2.0 * 3.14159265)), texCoord0.y) + distort;

    vec4 texColor = texture(Sampler0, rotatedUV);
    float brightness = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));

    float finalAlpha = brightness * texColor.a;

    vec2 screenUV = gl_FragCoord.xy / screenSize;
    vec2 distortedScreenUV = screenUV + (texColor.rg - 0.5) * 0.1;
    vec4 mainTarget = texture(MainTarget0, distortedScreenUV);

    if (finalAlpha == 0.0) {
        fragColor = vec4(0.0);
    } else {
        vec3 gradientColor = texture(ColorTexture0, vec2(1.0 - brightness, 0.5)).rgb;
        fragColor = mix(mainTarget, vec4(gradientColor, 1.0), finalAlpha);
    }
}