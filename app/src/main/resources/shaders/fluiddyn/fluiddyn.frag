#version 330 core

in vec2 vLocalPos;
in float vProp;
out vec4 FragColor;

uniform float uLow;
uniform float uHigh;

vec3 blue  = vec3(0.0, 0.2, 1.0);
vec3 white = vec3(1.0, 1.0, 1.0);
vec3 red   = vec3(1.0, 0.0, 0.5);

void main() {
    float r = 0.5;
    float d = length(vLocalPos);

    if (d-r>0) {
        discard;
    }
    
    float t = (vProp-uLow)/(uHigh-uLow);
    t = clamp(t, 0.0, 1.0);
    FragColor = vec4(mix(blue, red, t), 1.0);
}
