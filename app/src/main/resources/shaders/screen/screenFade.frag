#version 330 core

out vec4 FragColor;

uniform float uScreenFade;

void main() {
    FragColor = vec4(0.0, 0.0, 0.0, uScreenFade);
}
