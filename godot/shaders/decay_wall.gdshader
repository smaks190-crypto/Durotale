shader_type spatial;
render_mode cull_back, diffuse_burley;

uniform float damage : hint_range(0.0, 1.0) = 0.5;
uniform vec4 plaster_color : source_color = vec4(0.68, 0.70, 0.64, 1.0);
uniform vec4 stain_color : source_color = vec4(0.38, 0.34, 0.29, 1.0);
uniform vec4 chip_color : source_color = vec4(0.22, 0.19, 0.17, 1.0);

varying vec3 wall_world;
varying vec3 wall_normal;

float hash21(vec2 v) {
    return fract(sin(dot(v, vec2(127.1, 311.7))) * 43758.5453123);
}

float value_noise(vec2 v) {
    vec2 id = floor(v);
    vec2 f = fract(v);
    vec2 smooth_f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(id), hash21(id + vec2(1.0, 0.0)), smooth_f.x),
        mix(hash21(id + vec2(0.0, 1.0)), hash21(id + vec2(1.0, 1.0)), smooth_f.x),
        smooth_f.y
    );
}

float fractal_noise(vec2 v) {
    float n = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; ++i) {
        n += amplitude * value_noise(v);
        v *= 2.07;
        amplitude *= 0.5;
    }
    return n;
}

void vertex() {
    wall_world = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
    wall_normal = NORMAL;
}

void fragment() {
    // Wall pattern is measured in world metres, not stretched independently
    // across every wall box's UV. This keeps rooms and corridors consistent.
    vec2 p = abs(wall_normal.x) > abs(wall_normal.z)
        ? vec2(wall_world.z, wall_world.y)
        : vec2(wall_world.x, wall_world.y);
    float rough = fractal_noise(p * 2.8 + vec2(11.3, 3.2));
    float grain = value_noise(p * 34.0) - 0.5;
    float peeled = fractal_noise(p * 4.8 + vec2(23.0, 6.0));
    float cracks = value_noise(p * 13.0 + 18.1);

    // Old institutional paint: dark desaturated green to 1.43m, worn
    // ivory plaster above. The borderline is chipped, never ruler-straight.
    float boundary = 1.43 + (rough - 0.42) * 0.085;
    float upper = smoothstep(boundary - 0.035, boundary + 0.035, wall_world.y);
    vec3 institutional_green = vec3(0.22, 0.27, 0.26);
    vec3 base = mix(institutional_green, plaster_color.rgb, upper);
    float baseboard = 1.0 - smoothstep(0.12, 0.18, wall_world.y);
    base = mix(base, vec3(0.11, 0.13, 0.13), baseboard * 0.83);

    float stains = smoothstep(0.51 - damage * 0.16, 0.78 - damage * 0.11, rough);
    float peeling = smoothstep(0.69 - damage * 0.23, 0.83 - damage * 0.17, peeled);
    float hairlines = smoothstep(0.76, 0.85, cracks) * (0.14 + damage * 0.39);
    float darkened = clamp(stains * (0.12 + damage * 0.40) + peeling * (0.12 + damage * 0.63), 0.0, 0.91);
    base = mix(base, stain_color.rgb * 0.62, darkened);
    base = mix(base, chip_color.rgb * 0.73, peeling * damage * 0.66);
    base -= vec3(hairlines * 0.08);
    base += grain * 0.092;

    ALBEDO = clamp(base, vec3(0.022), vec3(0.94));
    ROUGHNESS = clamp(0.88 + rough * 0.1 + peeling * 0.08, 0.72, 1.0);
    METALLIC = 0.0;
}
