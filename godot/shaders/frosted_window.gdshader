shader_type spatial;
render_mode unshaded, cull_back;
// Dead trees outside a milk-glass ward window. Readable without HDR or SSR.
uniform float light_strength : hint_range(0.0, 2.0) = 0.75;
float hash21(vec2 p) {
    return fract(sin(dot(p,vec2(103.7,311.9)))*43758.5453123);
}
float noise21(vec2 p) {
    vec2 i=floor(p), f=fract(p), u=f*f*(3.0-2.0*f);
    return mix(mix(hash21(i),hash21(i+vec2(1.0,0.0)),u.x),
               mix(hash21(i+vec2(0.0,1.0)),hash21(i+vec2(1.0,1.0)),u.x),u.y);
}
void fragment() {
    vec2 p = UV;
    float frost = noise21(p*46.0);
    float haze = noise21(p*8.0);
    float horizon = smoothstep(0.34,0.57,p.y+0.04*haze);
    float trees = 0.0;
    for (int i=0; i<6; i++) {
        float x = float(i)*0.175 + 0.07;
        float trunk = 1.0 - smoothstep(0.012,0.025,abs(p.x-x));
        trees = max(trees, trunk*(0.25 + 0.75*horizon));
        float branch_y = 0.28+0.12*float(i%3);
        float diagonal = abs(p.y - (branch_y + 1.7*abs(p.x-x)));
        trees = max(trees,(1.0 - smoothstep(0.008,0.020,diagonal))*
                         (1.0-smoothstep(0.0,0.16,abs(p.x-x))) * horizon);
    }
    vec3 overcast = mix(vec3(0.19,0.22,0.23), vec3(0.36,0.38,0.38), p.y);
    overcast *= 0.78 + 0.25*frost;
    overcast = mix(overcast,vec3(0.066,0.074,0.073),trees*0.73);
    overcast *= 0.88+0.15*haze;
    ALBEDO = overcast * light_strength;
}
