shader_type spatial;
render_mode cull_back, diffuse_burley;

uniform float damage : hint_range(0.0, 1.0) = 0.5;
uniform vec4 plaster_color : source_color = vec4(0.68, 0.70, 0.64, 1.0);
uniform vec4 stain_color : source_color = vec4(0.38, 0.34, 0.29, 1.0);
uniform vec4 chip_color : source_color = vec4(0.22, 0.19, 0.17, 1.0);

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void fragment() {
    // Симуляция низкого разрешения текстурных координат (PS1 horror vibe)
    vec2 uv_ps1 = floor(UV * 128.0) / 128.0;
    
    // Микрошум от координат
    float micro_noise = noise(uv_ps1 * 64.0) * 0.15;
    
    // Крупные пятна (потеки, грязь)
    float large_stains = fbm(uv_ps1 * 4.0);
    
    // Сколы и облупившаяся краска (высококонтрастный шум)
    float chips_base = noise(uv_ps1 * 18.0);
    float chips_pattern = smoothstep(0.35, 0.65, chips_base);
    
    // Маски повреждения, управляемые uniform damage
    float stain_mask = smoothstep(0.55 - damage * 0.45, 0.85 - damage * 0.45, large_stains);
    float chip_mask = smoothstep(0.65 - damage * 0.55, 0.95 - damage * 0.55, chips_pattern);
    
    // Композиция цвета штукатурки
    vec3 final_color = plaster_color.rgb;
    final_color = mix(final_color, stain_color.rgb, stain_mask);
    final_color = mix(final_color, chip_color.rgb, chip_mask * damage);
    
    // Наложение микрошума на цвет для шероховатости
    final_color -= micro_noise * (0.2 + damage * 0.6);
    
    // Шероховатая поверхность: повреждения и шум влияют на ROUGHNESS
    float final_roughness = 0.75 + (micro_noise * 2.0) + (stain_mask * 0.15) + (chip_mask * 0.25);
    final_roughness = clamp(final_roughness, 0.0, 1.0);
    
    ALBEDO = final_color;
    ROUGHNESS = final_roughness;
}