#version 420

layout (location = 0) in vec2 pos;
// first 2 are velocity vector, last 2 are acceleration vector
layout (location = 1) in vec4 vel_acc;
// amplitude, frequency, and phase of sinusoidal motion along global x axis
layout (location = 2) in vec3 sinusoid_x;
// amplitude, frequency, and phase of sinusoidal motion along global y axis
layout (location = 3) in vec3 sinusoid_y;
// elements are direction, angular velocity, and angular acceleration
layout (location = 4) in vec3 angle_data;
// circle the initial position at the given angle, last 2 are that angle's delta-t and acceleration
layout (location = 5) in vec3 radial_data;
// elements are scale, growth rate, and growth acceleration
layout (location = 6) in vec3 size_data;
layout (location = 7) in vec4 color_start;
layout (location = 8) in vec4 color_end;
layout (location = 9) in float lifetime;

uniform mat4 projection;
uniform float time;

const vec2 vert_locs[4] = vec2[] (
  vec2(0., 0.),
  vec2(1., 0.),
  vec2(0., 1.),
  vec2(1., 1.)
);

out vec2 texCoord;
out vec4 color;

mat2 rot_mat(float angle) {
  return mat2(cos(angle), sin(angle), -sin(angle), cos(angle));
}

void main() {
  vec2 vert_pos = vert_locs[gl_VertexID];
  vec2 new_pos = pos + time*vel_acc.xy + 0.5f*time*time*vel_acc.zw;
  float new_angle = angle_data.x + time*angle_data.y + 0.5f*time*time*angle_data.z;
  float radial_angle = radial_data.x + time*radial_data.y + 0.5*time*time*radial_data.z;
  float new_size = size_data.x + time*size_data.y + 0.5f*time*time*size_data.z;

  new_pos += vec2(sinusoid_x.x * sin(sinusoid_x.y * time + sinusoid_x.z), sinusoid_y.x * sin(sinusoid_y.y * time + sinusoid_y.z));
  // so that new_pos = pos at t = 0
  new_pos -= vec2(sinusoid_x.x * sin(sinusoid_x.z), sinusoid_y.x * sin(sinusoid_y.z));
  new_pos = rot_mat(radial_angle) * (new_pos - pos) + pos;

  vec2 vec = rot_mat(new_angle) * vec2(new_size*vert_pos.x - new_size/2.f, new_size*vert_pos.y - new_size/2.f);
  gl_Position = projection * vec4(vec.x + new_pos.x, vec.y + new_pos.y, 1.f, 1.f);

  texCoord = vert_pos;
  color = time > lifetime ? vec4(0., 0., 0., 0.) : clamp(mix(color_start, color_end, time / lifetime), 0., 1.);
}