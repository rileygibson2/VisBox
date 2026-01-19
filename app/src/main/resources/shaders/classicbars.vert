#version 330 core

layout(location = 0) in vec2 aQuadPos;
layout(location = 1) in float aPosX;
layout(location = 2) in float aPosY;
layout(location = 3) in vec3 aHSB;

uniform int uNumBands;
uniform int uNumBlocks;
    
out vec2 vCellUV;
out vec3 vHSB;

void main() {
    float numBands = float(uNumBands);
    float numBlocks = float(uNumBlocks);

    float cellW = 1.0/numBands;
    float cellH = 1.0/numBlocks;
    float cx = (aPosX+0.5)*cellW;
    float cy = (aPosY+0.5)*cellH;

    vec2 cellSize = vec2(1.0/numBands, 1.0/numBlocks);
    vec2 cellSizeR = vec2(0.9/numBands, 0.9/numBlocks);


    vec2 world = vec2(
        cx+aQuadPos.x*cellW,
        cy+aQuadPos.y*cellH
    );

    vec2 ndc = world*2.0-1.0;
    gl_Position = vec4(ndc, 0.0, 1.0);

    vCellUV = aQuadPos+0.5;
    vHSB = aHSB;
}