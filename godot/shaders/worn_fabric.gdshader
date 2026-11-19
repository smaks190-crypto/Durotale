shader_type spatial;
render_mode cull_back, diffuse_burley;
uniform vec4 fabric_tint : source_color = vec4(0.14, 0.14, 0.15, 1.0);
uniform float scuff_amount : hint_range(0.0, 1.0) = 0.45;
varying vec3 model_pos;
float hash31(vec3 p) {
    return fract(sin(dot(p, vec3(17.3, 41.7, 113.9))) * 43758.5453);
}
void vertex() {
    model_pos = VERTEX;
}
void fragment() {
    // Soft fabric texture with coarse broad tonal patches and tiny weave.
    vec3 weave_coords = floor(model_pos * 94.0);
    vec3 abrasion_coords = floor(model_pos * 13.0);
    float weave = hash31(weave_coords);
    float scuff = hash31(abrasion_coords);
    float vertical_thread = 0.94 + 0.06 * sin(model_pos.x * 435.0);
    vec3 color = fabric_tint.rgb * (0.72 + 0.28 * scuff + 0.10 * weave) * vertical_thread;
    color = mix(color, color * 0.73, scuff_amount * smoothstep(0.69,0.94,scuff));
    ALBEDO = color;
    ROUGHNESS = 0.94;
}
