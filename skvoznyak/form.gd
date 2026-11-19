extends Node3D
# Faceted closed rings define clothing, anatomy and furniture in metres.
func surface(parent:Node3D,mesh:Mesh,at:Vector3,material:Material)->MeshInstance3D:
    var v:=MeshInstance3D.new()
    v.mesh=mesh
    v.material_override=material
    v.position=at
    parent.add_child(v)
    return v

func box(parent:Node3D,at:Vector3,size:Vector3,m:Material,solid:=false)->MeshInstance3D:
    var mesh:=BoxMesh.new()
    mesh.size=size
    var obj:=surface(parent,mesh,at,m)
    if solid:
        var body:=StaticBody3D.new()
        var cs:=CollisionShape3D.new()
        var shape:=BoxShape3D.new()
        shape.size=size
        cs.shape=shape
        body.add_child(cs)
        obj.add_child(body)
    return obj

func bar(parent:Node3D,a:Vector3,b:Vector3,r:float,m:Material,r2:float=-1)->MeshInstance3D:
    var tube:=CylinderMesh.new()
    tube.radial_segments=8
    tube.rings=1
    tube.bottom_radius=r
    tube.top_radius=r if r2<0 else r2
    tube.height=a.distance_to(b)
    var obj:=surface(parent,tube,(a+b)*.5,m)
    obj.quaternion=Quaternion(Vector3.UP,(b-a).normalized())
    return obj

func rings(parent:Node3D,at:Vector3,rows:Array,m:Material,sides:=10)->MeshInstance3D:
    var st:=SurfaceTool.new()
    st.begin(Mesh.PRIMITIVE_TRIANGLES)
    for j in range(rows.size()-1):
        var low:Vector4=rows[j]
        var high:Vector4=rows[j+1]
        for i in sides:
            var u:=TAU*i/sides
            var v:=TAU*(i+1)/sides
            var a:=Vector3(cos(u)*low.y,low.x,sin(u)*low.z+low.w)
            var b:=Vector3(cos(v)*low.y,low.x,sin(v)*low.z+low.w)
            var c:=Vector3(cos(u)*high.y,high.x,sin(u)*high.z+high.w)
            var d:=Vector3(cos(v)*high.y,high.x,sin(v)*high.z+high.w)
            for tri in [[a,b,c],[b,d,c]]:
                var norm:Vector3=(tri[2]-tri[0]).cross(tri[1]-tri[0]).normalized()
                for pt in tri:
                    st.set_normal(norm)
                    st.add_vertex(pt)
    return surface(parent,st.commit(),at,m)

func pigment(rgb:String,k:=0)->ShaderMaterial:
    var mat:=ShaderMaterial.new()
    mat.shader=preload("res://surface.gdshader")
    mat.set_shader_parameter("pigment",Color(rgb))
    mat.set_shader_parameter("kind",float(k))
    var maps:=["fabric","wall","tile","iron"]
    mat.set_shader_parameter("scan",preload("res://materials.gd").get_map(maps[k]))
    return mat

func figure(parent:Node3D,nurse:bool)->Node3D:
    var rig:=Node3D.new()
    parent.add_child(rig)
    var coat:=pigment("918d80" if nurse else "423e3a")
    var seam:=pigment("726f65" if nurse else "282725")
    var face:=pigment("a49a8a")
    var hair:=pigment("393332")
    var legs:=pigment("44413e")
    rings(rig,Vector3.ZERO,[Vector4(.61,.27,.17,.01),Vector4(.85,.24,.17,.02),Vector4(1.12,.24,.16,.02),Vector4(1.41,.31,.19,.015),Vector4(1.48,.23,.15,.02),Vector4(1.52,.10,.10,0)],coat)
    rings(rig,Vector3.ZERO,[Vector4(1.39,.28,.18,.02),Vector4(1.48,.20,.16,.045),Vector4(1.52,.12,.13,.04)],seam)
    rings(rig,Vector3.ZERO,[Vector4(1.45,.083,.085,0),Vector4(1.57,.085,.09,0),Vector4(1.61,.087,.08,0)],face)
    rings(rig,Vector3.ZERO,[Vector4(1.52,.05,.06,-.045),Vector4(1.58,.103,.122,-.026),Vector4(1.69,.137,.144,0),Vector4(1.79,.128,.13,.006),Vector4(1.84,.05,.07,.01)],face)
    rings(rig,Vector3.ZERO,[Vector4(1.59,.112,.105,.048),Vector4(1.68,.143,.14,.027),Vector4(1.78,.14,.155,.012),Vector4(1.85,.093,.12,.012),Vector4(1.88,.002,.002,0)],hair,11)
    for side in [-1.,1.]:
        var ear:=Vector3(side*.131,1.665,.008)
        rings(rig,ear,[Vector4(-.039,.012,.021,0),Vector4(.008,.021,.036,0),Vector4(.049,.009,.02,0)],face,6)
        var lock:=bar(rig,Vector3(side*.123,1.75,.026),Vector3(side*.103,1.61,.061),.034,hair,.008)
        lock.rotation.z+=side*.07
    for i in 7:
        var ang:=.15+i*.46
        var a:=Vector3(.128*cos(ang),1.78,.144*sin(ang)+.012)
        var b:=Vector3(.108*cos(ang+.11),1.60+.018*sin(i*2.4),.117*sin(ang+.11)+.043)
        bar(rig,a,b,.036,hair,.009)
    box(rig,Vector3(0,1.29,.175),Vector3(.009,.32,.014),seam)
    if nurse:
        rings(rig,Vector3.ZERO,[Vector4(1.75,.143,.153,.018),Vector4(1.84,.15,.13,.026),Vector4(1.86,.003,.003,0)],coat)
    for side in [-1.,1.]:
        var s:=float(side)
        var elbow:=Vector3(s*.34,1.08,.015)
        var wrist:=Vector3(s*(.18 if nurse else .31),1.06 if nurse else .92,-.24)
        bar(rig,Vector3(s*.245,1.39,.01),elbow,.14,coat,.099)
        bar(rig,elbow,wrist,.111,coat,.073)
        bar(rig,wrist,wrist+Vector3(-s*.014,-.071,-.058),.067,face,.045)
        bar(rig,Vector3(s*.124,.78,0),Vector3(s*.14,.43,-.01),.124,legs,.10)
        bar(rig,Vector3(s*.14,.45,-.01),Vector3(s*.14,.095,.026),.097,legs,.078)
        box(rig,Vector3(s*.14,.065,-.05),Vector3(.16,.12,.29),seam)
    if nurse: box(rig,Vector3(0,1.08,-.22),Vector3(.42,.088,.28),pigment("c0b9aa"))
    return rig
