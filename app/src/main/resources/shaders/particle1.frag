#version 330 core

uniform float uCoreRadius;
uniform float uCoreSoftness;
uniform float uHaloPower;
uniform int uPass;

in vec2 vLocalPos;
in vec3 vHSB;
in float vBright;
out vec4 FragColor;

vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(
        abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0,
        0.0, 1.0
    );
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

void main() {
    float d = length(vLocalPos);
    float r = d * 2.0;
    if (r > 1.0) {
        discard;
    }

    vec3 baseColor = hsb2rgb(vHSB);

    float core = 1.0 - smoothstep(uCoreRadius, uCoreRadius + uCoreSoftness, r);
    float halo = 1.0 - smoothstep(uCoreRadius, 1.0, r);

    halo = pow(halo, uHaloPower);
    float coreIntensity = core;
    float haloIntensity = halo * 0.7;

    float brightness = vHSB.z *vBright;

    vec3 finalColor;
    float finalAlpha;

    if (uPass == 1) {
        float glow = haloIntensity;
        finalColor = baseColor * glow * brightness;
        finalAlpha = clamp(halo * 0.8, 0.0, 1.0);
    } else if (uPass == 2) {
        float c = coreIntensity;
        finalColor = baseColor * c * brightness;
        finalAlpha = clamp(core * 1.0, 0.0, 1.0);
    }

    FragColor = vec4(finalColor, finalAlpha);
}