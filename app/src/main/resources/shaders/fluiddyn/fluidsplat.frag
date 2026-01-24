// density_splat.frag
#version 330 core

in vec2 vCenter;

uniform float uSmoothingRadius;
uniform float uMass;

layout(location = 0) out float outDensity;

float poly6Kernel(float r, float h) {
    if (r >= h) return 0.0;
    float hh = h * h;
    float x = hh - r * r;
    // 3D Poly6 normalization (fine for visualization)
    float c = 315.0 / (64.0 * 3.141592653589793 * pow(h, 9.0));
    return c * x * x * x;
}

void main() {
    // Window-space fragment coords (in pixels)
    vec2 fragPos = gl_FragCoord.xy;

    vec2 d = fragPos - vCenter;
    float r = length(d);

    float h = uSmoothingRadius;

    if (r >= h) {
        discard; // outside support of kernel
    }

    float w = poly6Kernel(r, h);
    float densityContribution = uMass * w;

    outDensity = densityContribution;
}