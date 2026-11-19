shader_type spatial;
render_mode cull_back, diffuse_burley;

// World-aligned plaster, dark hospital dado, mildew and peeled paint.
// Stable across reality swaps and independent from BoxMesh UV stretching.
uniform float damage : hint_range(0.0, 1.0) = 0.5;
uniform vec4 plaster_color : source_color = vec4(0.68, 0.70, 0.64, 1.0);
uniform vec4 stain_color : source_color = vec4(0.38, 0.34, 0.29, 1.0);
uniform vec4 chip_color : source_color = vec4(0.22, 0.19, 0.17, 1.0);
uniform vec4 lower_color : source_color = vec4(0.145, 0.17, 0.17, 1.0);
varying vec3 wp;
varying vec3 wn;

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f*f*(3.0-2.0*f);
    return mix(mix(hash21(i),hash21(i+vec2(1.0,0.0)),u.x),
               mix(hash21(i+vec2(0.0,1.0)),hash21(i+vec2(1.0,1.0)),u.x),u.y);
}

void vertex() {
    wp = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
    wn = normalize((MODEL_NORMAL_MATRIX * NORMAL));
}

void fragment() {
    vec2 uv = abs(wn.x) > abs(wn.z) ? wp.zy : wp.xy;
    // Pin low-frequency decay to meters of world geometry.
    float discol = noise21(uv*0.85);
    float blotches = noise21(uv*2.7 + 9.1);
    float flake = noise21(uv*9.5 + noise21(uv*1.6)*4.0);
    float speckle = hash21(floor(uv * 67.0));
    float wet_streak = noise21(vec2(uv.x*3.0, uv.y*0.25)+3.7);
    float drip = smoothstep(0.70, 0.88, wet_streak) * (0.65 + damage);
    float peeling = smoothstep(0.56 - damage*0.23, 0.72, flake) *
                    smoothstep(0.40, 0.67, blotches);
    float damp = smoothstep(0.52 - damage*0.18, 0.79, discol);
    float strip = 1.0 - smoothstep(1.36, 1.43, wp.y);
    float separator = 1.0 - smoothstep(0.008, 0.027, abs(wp.y - 1.43));
    vec3 upper = plaster_color.rgb;
    vec3 bottom = lower_color.rgb;
    vec3 color = mix(upper, bottom, strip);
    color = mix(color, stain_color.rgb, damp * (0.19 + 0.53*damage));
    color = mix(color, chip_color.rgb, peeling * (0.08 + 0.78*damage));
    color *= 1.0 - drip * (0.09 + 0.17 * damage);
    color *= 0.87 + 0.16 * speckle;
    color = mix(color, vec3(0.08, 0.095, 0.09), separator * 0.73);
    float ground_dirt = 1.0 - smoothstep(0.15,0.85, wp.y);
    color = mix(color, stain_color.rgb * 0.42, ground_dirt * (0.18 + 0.34 * damage));
    ALBEDO = color;
    ROUGHNESS = clamp(0.86 + damp*0.1 - drip*0.1, 0.55, 1.0);
}
