#version 420

in vec2 texCoord;
in vec4 color;
out vec4 fragColor;

layout (binding = 0) uniform sampler2D tex;

void main() {
    fragColor = texture(tex, texCoord) * color;
}