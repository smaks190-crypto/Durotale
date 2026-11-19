shader_type spatial;
uniform vec4 pigment : source_color;
uniform sampler2D scan : repeat_enable, filter_linear_mipmap;
uniform float kind;
uniform float wear;
varying vec3 p;
varying vec3 n;
void vertex(){p=(MODEL_MATRIX*vec4(VERTEX,1.)).xyz;n=normalize(mat3(MODEL_MATRIX)*NORMAL);}
float hash(vec2 v){return fract(sin(dot(v,vec2(72.137,17.713)))*43758.5453);}
float noise2(vec2 v){vec2 i=floor(v),f=fract(v);f=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+1.),f.x),f.y);}
void fragment(){
vec2 uv=abs(n.y)>.6?p.xz:(abs(n.x)>.65?p.zy:p.xy);
uv=floor(uv*115.)/115.;float grit=hash(uv*115.);float a=noise2(uv*1.8),b=noise2(uv*8.3),c=noise2(uv*28.),m=a*.42+b*.32+c*.19+grit*.07;
vec3 col=pigment.rgb*(.72+.5*m+.16*grit);
if(kind==1.){col*=p.y<1.27?.40:1.;float spots=smoothstep(.51,.55,m+wear*.15);col=mix(col,vec3(.06,.054,.049)*(.3+m),spots*wear); col*=1.-wear*.47*smoothstep(.5,.8,noise2(uv*vec2(17.,1.3)));col*=.67+.33*smoothstep(0.,.42,p.y);}
if(kind==2.){vec2 tile=uv*2.;float light=mod(floor(tile.x)+floor(tile.y),2.);col=mix(vec3(.055),vec3(.31),light)*(.44+m);vec2 e=min(fract(tile),1.-fract(tile));col*=.54+.46*smoothstep(0.,.024,min(e.x,e.y));col=mix(col,vec3(.09,.083,.078),smoothstep(.50,.73,m)*(.45+.5*wear));SPECULAR=.8;}
if(kind==3.){col=mix(col,vec3(.10,.065,.043),wear*smoothstep(.45,.62,m));}
float scanned=texture(scan,uv*vec2(.94,.51)).r;col*=mix(.58,1.36,scanned);
if(kind==1.){col=mix(col,vec3(.11,.101,.09)*(.5+scanned),wear*smoothstep(.42,.65,scanned)*smoothstep(.48,.6,a));}
ROUGHNESS=kind==2.?mix(.17,.74,b):.87;ALBEDO=col;
}
