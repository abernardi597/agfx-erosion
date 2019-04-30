#version 400 core

uniform mat4 u_mvp;

layout(location=0) in vec3 v_position;
layout(location=1) in vec3 v_normal;

out vec3 f_normal;
out vec3 f_center;

void main() {
    gl_Position = u_mvp * vec4(v_position,1);
    gl_PointSize = 8;
    f_normal = normalize(u_mvp * vec4(v_normal, 0)).xyz;
    f_center = gl_Position.xyz;
}
