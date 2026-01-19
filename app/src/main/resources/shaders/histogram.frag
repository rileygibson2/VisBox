#version 330 core

in vec2 vUV;
out vec4 FragColor;

uniform sampler2D uHistTex;
uniform int uNumBands;
uniform int uHistoryLen;
uniform int uWriteRow;    // index of NEXT row to be written
uniform float uGamma;
uniform float uIntensity;

// Simple value -> color ramp
vec3 valueToColor(float v)
{
    v = clamp(v, 0.0, 1.0);
    v = pow(v, uGamma);

    vec3 colors[6] = vec3[](
        vec3(0.0, 0.0, 0.0),
        vec3(0.0, 0.0, 0.35),
        vec3(0.8, 0.0, 0.8),
        vec3(1.0, 0.0, 0.0),
        vec3(1.0, 1.0, 0.0),
        vec3(1.0)
    );

    int N = 6; // array length

    float x = v * float(N - 1);
    int i = int(floor(x));
    float t = fract(x);

    vec3 color = mix(colors[i], colors[i+1], t);
    return color;
}

void main()
{
    float numBands   = float(uNumBands);
    float historyLen = float(uHistoryLen);
    float writeRow   = float(uWriteRow);

    float bandIndex = vUV.x * (numBands - 1.0);
    float texX = (bandIndex + 0.5) / numBands;

    float t = 1-vUV.y; // 0..1
    float age01    = 1.0 - t;
    float ageRows  = age01 * (historyLen - 1.0);

    float newest = writeRow - 1.0;
    if (newest < 0.0) newest += historyLen;

    float rowIndex = newest - ageRows;
    rowIndex = mod(rowIndex + historyLen, historyLen);

    float texY = (rowIndex + 0.5) / historyLen;

    float value = texture(uHistTex, vec2(texX, texY)).r;

    vec3 color = valueToColor(value) * uIntensity;

    // simple grayscale
    color = clamp(color, 0.0, 1.0);
    FragColor = vec4(color, 1.0);
}

