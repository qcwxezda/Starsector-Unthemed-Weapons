#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D tex;
uniform vec4 tintColor;

void main() {
    fragColor = texture(tex, texCoord) * tintColor;
}