shader_type spatial;
render_mode cull_back, diffuse_burley, specular_schlick_ggx;

// World-space tiles: no stretching when the same material is used on a large room.
uniform float damage : hint_range(0.0, 1.0) = 0.25;
uniform float wetness : hint_range(0.0, 1.0) = 0.55;
varying vec3 local_world;

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), u.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x), u.y);
}

void vertex() {
    local_world = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
}

void fragment() {
    vec2 uv = local_world.xz / 0.58;
    vec2 tile_id = floor(uv);
    vec2 within = fract(uv);
    float checker = mod(tile_id.x + tile_id.y, 2.0);
    float tile_tint = hash21(tile_id * 3.1);
    float fine = noise21(floor(local_world.xz * 46.0) / 3.8);
    float pooled = noise21(local_world.xz * 0.65 + 19.3);
    float scratches = noise21(local_world.xz * 8.0);
    float edge = min(min(within.x, 1.0 - within.x), min(within.y, 1.0 - within.y));
    float grout = 1.0 - smoothstep(0.009, 0.027, edge);
    vec3 light_tile = vec3(0.39, 0.405, 0.395);
    vec3 dark_tile = vec3(0.082, 0.092, 0.094);
    vec3 color = mix(light_tile, dark_tile, checker);
    color *= 0.81 + 0.21 * tile_tint;
    color *= 0.88 + fine * 0.19;
    float grime = smoothstep(0.37 - damage * 0.14, 0.81, pooled);
    color = mix(color, vec3(0.055, 0.058, 0.053), grime * (0.18 + damage * 0.60));
    color *= 1.0 - 0.21 * smoothstep(0.66, 0.86, scratches) * damage;
    color = mix(color, vec3(0.024, 0.029, 0.028), grout * 0.70);
    float puddle = smoothstep(0.50, 0.76, pooled) * wetness;
    ALBEDO = color;
    ROUGHNESS = mix(0.76, 0.17, puddle);
    METALLIC = 0.0;
    SPECULAR = 0.45;
}
