#version 330 core

layout (location = 0) in vec2 pos;

uniform mat4 projection;
uniform vec2 offset;
uniform float scale;
uniform float angle;

out vec2 texCoord;

void main() {
    vec2 vec = mat2(cos(angle), sin(angle), -sin(angle), cos(angle)) * vec2(scale*pos.x - scale/2.f, scale*pos.y - scale/2.f);
    gl_Position = projection * vec4(vec.x + offset.x, vec.y + offset.y, 1.f, 1.f);
    texCoord = pos;
}