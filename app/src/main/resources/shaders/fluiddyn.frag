#version 330 core

in vec2 vLocalPos;
out vec4 FragColor;

void main() {

    float r = 0.5;
    float d = length(vLocalPos);

    if (d-r>0) {
        discard;
    }
    
    float t = clamp(d/0.9, 0.0, 1.0);
    //vec3 col = mix(vec3(0.0, 0.5, 1.0), vec3(0.0, 0.2, 0.5), t);
    vec3 col = vec3(0, 0.5, 1.0);
    float a = step(d, r);

    FragColor = vec4(col, a);
}
