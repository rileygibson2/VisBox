#version 330 core

uniform sampler2D uScreenTex;

in vec2 vUV;
out vec4 FragColor;

void main() {
    vec4 glow = texture(uScreenTex, vUV);
    FragColor = glow;
}