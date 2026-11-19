extends Node3D
class_name DurkaVisualDressing

# Art-direction-only layer. All interaction anchors and collision geometry
# remain in DurkaWorld. Decorative meshes have no physics.
var only_clean: Array[Node3D] = []
var only_ruined: Array[Node3D] = []
var only_distorted: Array[Node3D] = []
var plaster: StandardMaterial3D
var grime: StandardMaterial3D
var dark_iron: StandardMaterial3D
var chipped_enamel: StandardMaterial3D
var paper: StandardMaterial3D
var blood_dark: StandardMaterial3D
var pale_glow: StandardMaterial3D
var amber_glow: StandardMaterial3D
var glass: StandardMaterial3D

func finish(color: Color, roughness: float = 0.83, metal: float = 0.0, emission_energy: float = 0.0) -> StandardMaterial3D:
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = roughness
    material.metallic = metal
    if emission_energy > 0.0:
        material.emission_enabled = true
        material.emission = color
        material.emission_energy_multiplier = emission_energy
    return material

func detail(label: String, center: Vector3, extent: Vector3, material: Material, parent: Node3D = self) -> MeshInstance3D:
    var mesh := BoxMesh.new()
    mesh.size = extent
    var object := MeshInstance3D.new()
    object.name = label
    object.mesh = mesh
    object.material_override = material
    object.position = center
    parent.add_child(object)
    return object

func pillar(label: String, a: Vector3, b: Vector3, radius: float, material: Material, parent: Node3D = self) -> MeshInstance3D:
    var mesh := CylinderMesh.new()
    mesh.top_radius = radius
    mesh.bottom_radius = radius
    mesh.height = maxf(a.distance_to(b), 0.01)
    mesh.radial_segments = 8
    var object := MeshInstance3D.new()
    object.name = label
    object.mesh = mesh
    object.material_override = material
    object.position = (a + b) * 0.5
    if a.distance_squared_to(b) > 0.0001:
        object.quaternion = Quaternion(Vector3.UP, (b-a).normalized())
    parent.add_child(object)
    return object

func group(label: String) -> Node3D:
    var node := Node3D.new()
    node.name = label
    add_child(node)
    return node

func _ready() -> void:
    plaster = finish(Color(0.58,0.58,0.53),0.98)
    grime = finish(Color(0.15,0.17,0.16),0.97)
    dark_iron = finish(Color(0.19,0.21,0.21),0.61,0.50)
    chipped_enamel = finish(Color(0.53,0.56,0.54),0.67,0.14)
    paper = finish(Color(0.61,0.59,0.51),0.99)
    blood_dark = finish(Color(0.31,0.055,0.065),0.96)
    pale_glow = finish(Color(0.48,0.56,0.62),0.42,0.0,0.19)
    amber_glow = finish(Color(0.91,0.79,0.58),0.40,0.0,0.55)
    glass = finish(Color(0.25,0.32,0.35),0.20,0.18)
    build_architecture()
    build_window()
    build_room_detail()
    build_floor_wetness()
    build_corridor_detail()
    build_lamps()
    build_reality_debris()
    build_sidelight()

func build_architecture() -> void:
    # Painted wall borders and industrial skirting give the room scale.
    for side in [-1.0, 1.0]:
        var x: float = side * 5.795
        detail("WardPictureRail",Vector3(x,1.44,0),Vector3(0.06,0.065,10.8),dark_iron)
        detail("WardSkirting",Vector3(x,0.17,0),Vector3(0.09,0.23,11.7),grime)
        for z in [-4.65, 4.5]:
            detail("CornerWallStrip",Vector3(x,1.46,z),Vector3(0.08,2.76,0.11),grime)
    for x in [-5.6,5.6]:
        detail("WardCeilingCornice",Vector3(x,3.02,0),Vector3(0.15,0.095,11.7),chipped_enamel)
    for z in [-5.76,5.74]:
        for x in [-4.8,4.8]:
            detail("WallEdgeMoulding",Vector3(x,1.37,z),Vector3(0.06,2.70,0.07),grime)
    # Door with a complete frame, readable as an architectural element.
    for z in [5.77]:
        for x in [-0.35,2.53]:
            detail("DoorJamb",Vector3(x,1.25,z),Vector3(0.13,2.5,0.16),dark_iron)
        detail("DoorLintel",Vector3(1.09,2.55,z),Vector3(3.0,0.16,0.16),dark_iron)
    detail("PaintedRoomNumberPlate",Vector3(0.05,2.19,5.61),Vector3(0.38,0.21,0.03),chipped_enamel)
    detail("NumberPlateDarkInset",Vector3(0.05,2.19,5.58),Vector3(0.24,0.12,0.025),grime)

func build_window() -> void:
    # The shell has no wall opening: a recess lit from within fakes the
    # cold exterior beyond the actual grille and preserves collisions.
    detail("WindowPaleDaylight",Vector3(0,1.95,-5.73),Vector3(2.58,1.42,0.018),pale_glow)
    for x in [-1.37,1.37]:
        detail("WindowChippedFrame",Vector3(x,1.98,-5.57),Vector3(0.14,1.69,0.2),dark_iron)
    for y in [1.10,2.78]:
        detail("WindowChippedSill",Vector3(0,y,-5.56),Vector3(3.0,0.11,0.24),dark_iron)
    var window_reflection := finish(Color(0.59,0.67,0.68,0.11),0.13)
    window_reflection.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    detail("WindowBottomPuddle",Vector3(0,0.016,-4.2),Vector3(2.2,0.005,1.75),window_reflection)
    for i in 6:
        var x := -1.19 + float(i)*0.48
        detail("WindowSliverReflection",Vector3(x,0.019,-2.5),Vector3(0.035,0.003,2.6),window_reflection)

func build_room_detail() -> void:
    var props := group("VisiblePeriodObjects")
    # Deliberate silhouettes on the periphery: avoid covering the bed and
    # interaction markers or placing collision blockers.
    detail("IronWardCupboard",Vector3(5.35,1.08,2.1),Vector3(0.71,2.10,1.3),dark_iron,props)
    detail("CupboardEnamelDoor",Vector3(4.98,1.08,2.1),Vector3(0.025,1.89,1.12),chipped_enamel,props)
    detail("CupboardDoorLine",Vector3(4.96,1.05,2.1),Vector3(0.026,1.75,0.022),grime,props)
    detail("CupboardLatch",Vector3(4.90,1.16,1.84),Vector3(0.056,0.12,0.025),dark_iron,props)
    for x in [-4.4,-3.8]:
        pillar("OldStoolLeg",Vector3(x,0.10,3.1),Vector3(x,0.53,3.1),0.035,dark_iron,props)
    detail("OldStoolSeat",Vector3(-4.1,0.57,3.1),Vector3(0.90,0.085,0.56),chipped_enamel,props)
    detail("WallRecordBoard",Vector3(-5.77,1.86,-3.8),Vector3(0.055,0.85,0.62),dark_iron,props)
    detail("WallRecordSheet",Vector3(-5.73,1.86,-3.8),Vector3(0.020,0.72,0.51),paper,props)
    for i in 5:
        detail("PatientRecordLine",Vector3(-5.715,2.08-float(i)*0.10,-3.79),Vector3(0.008,0.012,0.40),grime,props)
    var linens := group("ExtraCleanLinen")
    only_clean.append(linens)
    detail("FoldedClothOnChair",Vector3(-0.6,0.57,-2.1),Vector3(0.48,0.13,0.42),paper,linens)
    var decay := group("WardDecayClutter")
    only_ruined.append(decay)
    for i in 29:
        var sx := -5.78
        var sy := 0.32 + float((i*13)%23)/10.0
        var sz := -5.1 + float((i*17)%96)/10.0
        var flake := detail("CurlingPaintLayer",Vector3(sx,sy,sz),Vector3(0.03,0.14 + float(i%4)*0.09,0.08 + float(i%5)*0.09),plaster,decay)
        flake.rotation.x = sin(float(i)*2.18)*0.21
    var hallucination := group("DistortionShadows")
    only_distorted.append(hallucination)
    for i in 11:
        var x := -4.8 + float(i%4)*2.4
        var z := -4.6 + float(i/4)*3.7
        pillar("HangingCable",Vector3(x,2.95,z),Vector3(x+0.17,1.65,z+0.21),0.018,blood_dark,hallucination)

func build_floor_wetness() -> void:
    var puddle := finish(Color(0.18,0.21,0.22,0.32),0.11)
    puddle.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    var reflected := finish(Color(0.60,0.67,0.71,0.21),0.12)
    reflected.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    var rng := RandomNumberGenerator.new()
    rng.seed = 19980427
    for i in 42:
        var x := rng.randf_range(-5.0,4.8)
        var z := rng.randf_range(-5.0,4.8)
        var width := rng.randf_range(0.14,0.55)
        var length := rng.randf_range(0.22,1.3)
        var patch := detail("IrregularFloorDampness",Vector3(x,0.018,z),Vector3(width,0.004,length),puddle)
        patch.rotation.y = rng.randf_range(-1.0,1.0)
        if i % 3 == 0:
            detail("SparseColdReflection",Vector3(x,0.024,z),Vector3(width*0.15,0.003,length*0.65),reflected)

func build_corridor_detail() -> void:
    var hall := group("CorridorArchitecturalDetails")
    for z in [8.2,12.8,18.6,24.4]:
        for x in [-1.88,1.88]:
            detail("CorridorPaintDivide",Vector3(x,1.44,z),Vector3(0.037,0.06,2.0),dark_iron,hall)
            detail("CorridorBaseboard",Vector3(x,0.15,z),Vector3(0.055,0.2,2.0),grime,hall)
    for z in [9.2,15.5,23.0]:
        detail("CeilingCableTrunk",Vector3(-1.66,2.99,z),Vector3(0.07,0.09,2.8),dark_iron,hall)
    # Red state retains readable corridor silhouettes, but reality is wrong.
    var bent := group("BentCorridorRecords")
    only_distorted.append(bent)
    for i in 9:
        var zz := 9.0 + float(i)*1.75
        var shard := detail("ImpossibleTileSeam",Vector3(0.15 + float(i%3)*0.15,0.04,zz),Vector3(0.035,0.012,1.1),blood_dark,bent)
        shard.rotation.y = float(i)*0.30

func build_lamps() -> void:
    var lamps := group("LuminousFluorescentStrips")
    for z in [0.0,10.8,15.5,22.0,25.5]:
        for x in [-0.22,0.22]:
            detail("AgedFluorescentTube",Vector3(x,2.85,z),Vector3(0.075,0.035,0.42),pale_glow,lamps)
    detail("WardEmergencyBulb",Vector3(4.95,2.95,4.50),Vector3(0.23,0.085,0.13),amber_glow,lamps)

func build_reality_debris() -> void:
    var trash := group("AbandonedWardDebris")
    only_ruined.append(trash)
    var rng := RandomNumberGenerator.new()
    rng.seed = 19980717
    for i in 22:
        var x := rng.randf_range(-4.7,4.8)
        var z := rng.randf_range(-4.6,3.7)
        var shard := detail("LooseFloorPlaster",Vector3(x,0.026,z),Vector3(rng.randf_range(0.07,0.27),0.025,rng.randf_range(0.06,0.29)),plaster,trash)
        shard.rotation.y = rng.randf_range(-PI,PI)

func build_sidelight() -> void:
    var daylight := SpotLight3D.new()
    daylight.name = "PaleWindowBounce"
    daylight.position = Vector3(0.0,2.17,-5.04)
    daylight.rotation.y = PI
    daylight.spot_range = 9.5
    daylight.spot_angle = 52.0
    daylight.light_color = Color(0.77,0.86,0.99)
    daylight.light_energy = 1.28
    daylight.shadow_enabled = false
    add_child(daylight)
    var ceiling_bounce := OmniLight3D.new()
    ceiling_bounce.name = "WarmWardPracticalBounce"
    ceiling_bounce.position = Vector3(4.2,2.56,3.8)
    ceiling_bounce.omni_range = 4.7
    ceiling_bounce.light_color = Color(0.98,0.79,0.58)
    ceiling_bounce.light_energy = 0.32
    add_child(ceiling_bounce)

func set_reality(level: int, mixing: bool = false) -> void:
    for p in only_clean:
        p.visible = (level == 0) or mixing
    for p in only_ruined:
        p.visible = (level != 0) or mixing
    for p in only_distorted:
        p.visible = (level == 2) or mixing
