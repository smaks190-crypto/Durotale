extends Node3D
class_name DurkaAtmosphere

# Decorative geometry only: the hospital owns all navigation and collisions.
# Deliberately low polygon count and a shared, almost monochrome material palette.
var clean_only: Array[Node3D] = []
var ruined_only: Array[Node3D] = []
var warped_only: Array[Node3D] = []
var iron: StandardMaterial3D
var rusty: StandardMaterial3D
var enamel: StandardMaterial3D
var charcoal: StandardMaterial3D
var chalk: StandardMaterial3D
var red: StandardMaterial3D
var glass: StandardMaterial3D
var wet: StandardMaterial3D

func material(color: Color, roughness: float = 0.87, metallic: float = 0.0) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = color
    m.roughness = roughness
    m.metallic = metallic
    m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
    return m

func part(label: String, mesh: Mesh, at: Vector3, mat: Material, parent: Node3D = self) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    n.name = label
    n.mesh = mesh
    n.position = at
    n.material_override = mat
    parent.add_child(n)
    return n

func box(label: String, at: Vector3, size: Vector3, mat: Material, parent: Node3D = self) -> MeshInstance3D:
    var mesh := BoxMesh.new()
    mesh.size = size
    return part(label, mesh, at, mat, parent)

func bar(label: String, a: Vector3, b: Vector3, radius: float, mat: Material, parent: Node3D = self) -> MeshInstance3D:
    var delta := b - a
    var mesh := CylinderMesh.new()
    mesh.top_radius = radius
    mesh.bottom_radius = radius
    mesh.height = maxf(delta.length(), 0.01)
    mesh.radial_segments = 7
    var item := part(label,mesh,(a + b)*0.5,mat,parent)
    if delta.length_squared() > 0.0001:
        item.quaternion = Quaternion(Vector3.UP,delta.normalized())
    return item

func ring(label: String, at: Vector3, radius: float, mat: Material, parent: Node3D = self) -> void:
    for i in 8:
        var a := float(i) * TAU / 8.0
        var b := float(i+1) * TAU / 8.0
        bar(label,at + Vector3(cos(a),0,sin(a))*radius,at + Vector3(cos(b),0,sin(b))*radius,0.019,mat,parent)

func category(label: String) -> Node3D:
    var n := Node3D.new()
    n.name = label
    add_child(n)
    return n

func _ready() -> void:
    iron = material(Color(0.25,0.255,0.26),0.57,0.65)
    rusty = material(Color(0.22,0.18,0.16),0.92,0.4)
    enamel = material(Color(0.65,0.65,0.62),0.75,0.3)
    charcoal = material(Color(0.075,0.078,0.082),0.96)
    chalk = material(Color(0.56,0.54,0.50),1.0)
    red = material(Color(0.29,0.043,0.048),0.97)
    glass = material(Color(0.23,0.25,0.27),0.22,0.20)
    wet = material(Color(0.20,0.22,0.24,0.25),0.16,0.22)
    wet.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    build_bed()
    build_radiator()
    build_medical_equipment()
    build_corridor()
    build_degradation()
    build_fluorescents()
    set_reality(0)

func build_bed() -> void:
    var b := category("IronBedDetail")
    for x in [1.10,3.0]:
        for z in [-2.9,-0.3]:
            bar("BedPost",Vector3(x,0.42,z),Vector3(x,1.45,z),0.055,iron,b)
            var knob := SphereMesh.new()
            knob.radius = 0.085
            knob.height = 0.17
            knob.radial_segments = 6
            knob.rings = 3
            part("BedPostCap",knob,Vector3(x,1.47,z),enamel,b)
    for z in [-2.9,-0.3]:
        for y in [0.88,1.34]:
            bar("BedRail",Vector3(1.1,y,z),Vector3(3.0,y,z),0.035,iron,b)
        for i in 7:
            var x := 1.22 + float(i)*0.27
            bar("BedRailSpindle",Vector3(x,0.88,z),Vector3(x,1.34,z),0.019,iron,b)
    for x in [1.13,2.97]:
        bar("FrameEdge",Vector3(x,0.75,-2.9),Vector3(x,0.75,-0.3),0.033,iron,b)
    var small := category("BedRustAndSeams")
    ruined_only.append(small)
    for i in 24:
        var x := 1.25 + float(i % 6)*0.29
        var z := -2.75 + float(i / 6)*0.74
        box("MattressStain",Vector3(x,0.881,z),Vector3(0.17,0.012,0.21),rusty,small)

func build_radiator() -> void:
    var p := category("SteamPipes")
    for i in 7:
        var z := -1.98 + float(i)*0.28
        bar("Tube",Vector3(-5.50,0.37,z),Vector3(-5.50,1.43,z),0.052,iron,p)
        bar("TubeConnector",Vector3(-5.5,1.4,z),Vector3(-5.4,1.4,z+0.11),0.032,iron,p)
    bar("HeatingFeed",Vector3(-5.57,0.39,-2.25),Vector3(-5.57,0.39,0.04),0.036,iron,p)
    var lost := category("BrokenPipeSections")
    ruined_only.append(lost)
    for i in 4:
        var z := -1.85 + float(i)*0.49
        bar("MissingPipeFragment",Vector3(-5.46,0.52,z),Vector3(-5.46,0.8 + float(i%2)*0.24,z),0.042,rusty,lost)
    var impossible := category("ImpossiblePipeOutline")
    warped_only.append(impossible)
    bar("CeilingPipeShadow",Vector3(-5.3,3.07,-1),Vector3(-0.7,3.07,-1.2),0.07,red,impossible)
    for i in 3:
        bar("CeilingPipeSpoke",Vector3(-5.0 + float(i),3.01,-1.1),Vector3(-4.9 + float(i),2.9,-1.0),0.03,red,impossible)

func build_medical_equipment() -> void:
    var iv := category("IVStand")
    bar("StandPole",Vector3(4.15,0.12,-1.0),Vector3(4.15,2.52,-1.0),0.033,iron,iv)
    bar("StandArm",Vector3(3.88,2.5,-1),Vector3(4.44,2.5,-1),0.025,iron,iv)
    for dz in [-0.23,0.23]:
        bar("StandFeet",Vector3(4.15,0.12,-1+dz),Vector3(4.15,0.12,-1-dz),0.05,iron,iv)
    var bag := box("CloudyMedicineBag",Vector3(4.36,2.12,-1),Vector3(0.21,0.49,0.09),enamel,iv)
    clean_only.append(bag)
    bar("IVTube",Vector3(4.36,1.9,-1),Vector3(4.35,1.2,-1.15),0.008,iron,iv)
    var tray := category("OldNurseTray")
    box("TrayRim",Vector3(3.9,1.0,-2.2),Vector3(0.54,0.028,0.35),iron,tray)
    box("GreyEnamelCup",Vector3(3.80,1.09,-2.15),Vector3(0.15,0.15,0.15),enamel,tray)
    ring("CupHandle",Vector3(3.91,1.11,-2.15),0.07,iron,tray)

func build_corridor() -> void:
    var cl := category("CorridorScenery")
    # The original gurney body exists in the world. Here we give it a readable frame.
    for z in [17.07,18.13]:
        for x in [0.59,1.45]:
            bar("TrolleySupport",Vector3(x,0.21,z),Vector3(x,0.87,z),0.035,iron,cl)
            ring("GurneyWheel",Vector3(x,0.22,z),0.10,rusty,cl)
    for z in [17.1,18.1]:
        bar("GurneyCrossbar",Vector3(0.57,0.65,z),Vector3(1.5,0.65,z),0.027,iron,cl)
    bar("GurneyHandrail",Vector3(0.52,1.12,17.0),Vector3(0.52,1.12,18.2),0.04,iron,cl)
    var sheets := category("DirtyDocuments")
    for i in 6:
        var z := 7.7 + float(i)*3.35
        var x := -1.87 if i%2 == 0 else 1.87
        var doc := box("PatientRecord",Vector3(x,1.63,z),Vector3(0.025,0.42,0.28),chalk,sheets)
        doc.rotation.x = 0.035 * float(i%3-1)
        box("RecordPhoto",Vector3(x + (-0.014 if i%2 else 0.014),1.73,z+0.055),Vector3(0.032,0.16,0.105),charcoal,sheets)
    var scraps := category("CorridorDebris")
    ruined_only.append(scraps)
    for i in 20:
        var z := 8.0 + float((i*17)%187)/10.0
        var x := -1.65 + float((i*7)%31)/10.0
        if absf(z-20.0)<1.7 and x>0.7:
            continue
        var chip := box("FallenPlaster",Vector3(x,0.022,z),Vector3(0.08+float(i%4)*0.045,0.015,0.10+float(i%3)*0.035),chalk,scraps)
        chip.rotation.y = float(i)*1.2

func build_degradation() -> void:
    var worn := category("DestroyedPaintAndPaper")
    ruined_only.append(worn)
    for i in 23:
        var x := -5.83
        var y := 0.45 + float(i%7)*0.34
        var z := -4.8 + float((i*11)%95)*0.1
        var flake := box("PeelingPlaster",Vector3(x,y,z),Vector3(0.025,0.14+float(i%3)*0.04,0.19+float(i%5)*0.05),chalk,worn)
        flake.rotation.x = float(i)*0.06
    var decay := category("DistortedWires")
    warped_only.append(decay)
    for i in 12:
        var z := 6.5 + float(i)*1.66
        bar("WarpedWiring",Vector3(-1.75,3.02,z),Vector3(-1.2+float(i%3)*0.2,2.8,z+0.4),0.021,red,decay)

func build_fluorescents() -> void:
    var lights := category("CeilingLightFixtures")
    for z in [0.0,10.8,15.5,22.0,25.5]:
        var x := 0.0
        box("FluorescentHousing",Vector3(x,3.02,z),Vector3(0.68,0.065,0.27),iron,lights)
        for off in [-0.21,0.21]:
            box("FluorescentTube",Vector3(x+off,2.94,z),Vector3(0.07,0.055,0.30),enamel,lights)

func set_reality(level: int, mixed: bool = false) -> void:
    for p in clean_only:
        if is_instance_valid(p):
            p.visible = (level == 0) or mixed
    for p in ruined_only:
        if is_instance_valid(p):
            p.visible = (level != 0) or mixed
    for p in warped_only:
        if is_instance_valid(p):
            p.visible = (level == 2) or mixed
