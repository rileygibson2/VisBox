#version 330 core

in vec2 vCellUV;   // 0..1 inside the cell
in vec3 vHSB;      // your H,S,B from instance data

out vec4 FragColor;

uniform float uCornerRadius;
uniform float uEdgeSoftness;


vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp( abs(mod(c.x * 6.0 + vec3(0.0,4.0,2.0),
                              6.0) - 3.0) - 1.0,
                      0.0, 1.0 );
    rgb = rgb * rgb * (3.0 - 2.0 * rgb); // smooth
    return c.z * mix(vec3(1.0), rgb, c.y);
}

float roundedRectSDF(vec2 p, vec2 halfSize, float r) {
    // shrink the core by radius
    vec2 q = abs(p) - (halfSize - vec2(r));

    // outside corner region
    float outside = length(max(q, 0.0)) - r;

    // inside core region
    float inside = min(max(q.x, q.y), 0.0);

    return outside + inside;
}

void main() {
    // Map vCellUV [0,1] to local coords centered at 0
    // full cell is roughly [-0.5, 0.5] in both axes
    vec2 p = vCellUV - 0.5;

    float margin = 0.08;
    vec2 halfSize = vec2(0.5 - margin);

    float r = uCornerRadius; // in same units as halfSize, e.g. 0.18
    float d = roundedRectSDF(p, halfSize, r);

    // Anti-aliased edge based on SDF gradient
    float w = max(uEdgeSoftness, fwidth(d));
    float alpha = 1.0 - smoothstep(0.0, w, d);

    // Fully outside → discard
    if (alpha <= 0.0) {
        discard;
    }

    // Your original color from HSB
    vec3 rgb = hsb2rgb(vHSB);

    FragColor = vec4(rgb, alpha);
}
