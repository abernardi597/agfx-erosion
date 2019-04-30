#version 400 core

const vec3 k_light_pos = vec3(2, 2, 0);
const vec3 k_light_color = vec3(1, 1, 1);
const float k_light_power = 1;
const vec3 k_specular = vec3(1, 1, 1);
const vec3 k_diffuse = vec3(0.5, 0.5, 0.5);
const vec3 k_ambient = vec3(0.1, 0.1, 0.1);
const float k_shiny = 64;

in vec3 f_normal;
in vec3 f_center;

vec3 projectOnto(vec3 point, vec3 planar, vec3 normal) {
    return point - normal * dot(planar - point, normal);
}

void main() {
    if (length(f_normal) == 0) discard;
    vec3 center = f_center;
    vec3 normal = normalize(f_normal);
    vec3 r = projectOnto(vec3(2 * gl_PointCoord.x - 1, 1 - 2*gl_PointCoord.y, 0), center, normal);
    if (dot(r,r) < 1) {
        vec3 toView = normalize(-gl_FragCoord.xyz);
        vec3 fromLight = normalize(k_light_pos - gl_FragCoord.xyz);
        vec3 h = normalize(fromLight + toView);

        float lambertian = max(dot(fromLight, normal), 0);
        float specular = lambertian > 0? pow(max(dot(h, normal), 0), k_shiny) : 0;

        vec3 color = k_ambient + (k_diffuse * lambertian + k_specular * specular) * k_light_color * k_light_power / length(fromLight);

        gl_FragColor = vec4(color, 1);
//        gl_FragDepth = r.z;
    } else discard;
}
