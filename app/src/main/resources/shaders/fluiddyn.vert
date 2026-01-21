#version 330 core

layout(location = 0) in vec2 aQuadPos;
layout(location = 1) in vec2 aCenter;
layout(location = 2) in float aSize;

uniform mat4 uProj;
    
out vec2 vLocalPos;

void main() {
    vec2 worldPos = aCenter + aQuadPos * aSize;
    gl_Position = uProj * vec4(worldPos, 0.0, 1.0);
    vLocalPos = aQuadPos;
}