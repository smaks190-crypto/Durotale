extends Node3D
class_name DurkaWorld

# The three realities share collision geometry and interaction anchors.
# Only decorative surfaces and lighting are swapped. Evidence is never cloned.
var markers: Dictionary = {}
var walls: Array[MeshInstance3D] = []
var floors: Array[MeshInstance3D] = []
var clean_props: Array[Node3D] = []
var ruined_props: Array[Node3D] = []
var warped_props: Array[Node3D] = []
var nurse: Node3D
var far_leaf: MeshInstance3D
var neighbor_leaf: MeshInstance3D
var neighbor_barrier: StaticBody3D
var room_leaf: MeshInstance3D
var frosted_glass: ShaderMaterial
var outside_light: OmniLight3D
var window_leaf: MeshInstance3D
var shadow: MeshInstance3D
var hall_light: OmniLight3D
var room_light: OmniLight3D
var reality := 0
var mixed := false
var ambience: Node3D

var paint_clean: ShaderMaterial
var paint_ruined: ShaderMaterial
var paint_warped: ShaderMaterial
var tiles_clean: ShaderMaterial
var tiles_ruined: ShaderMaterial
var iron: StandardMaterial3D
var rust: StandardMaterial3D
var cloth: StandardMaterial3D
var blood: StandardMaterial3D
var dark: StandardMaterial3D

func mat(color: Color, texture: Texture2D = null) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = color
    if texture != null:
        m.albedo_texture = texture
        m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
    m.roughness = 0.94
    return m

func make_grime(checkered: bool) -> Texture2D:
    var img := Image.create_empty(128, 128, false, Image.FORMAT_RGBA8)
    var rng := RandomNumberGenerator.new()
    rng.seed = 1772 if checkered else 1998
    for y in 128:
        for x in 128:
            var n := rng.randf_range(-0.11, 0.11)
            var shade := 0.60
            if checkered:
                shade = 0.16 if (int(x / 16) + int(y / 16)) % 2 == 0 else 0.63
                if rng.randf() < 0.02:
                    n -= 0.13
            else:
                shade = 0.48 if y < 70 else 0.22
                if rng.randf() < 0.08:
                    n -= 0.26
                if y < 70 and rng.randf() < 0.03:
                    n += 0.18
            img.set_pixel(x, y, Color(clampf(shade + n, 0, 1), clampf(shade + n,0,1),clampf(shade + n,0,1)))
    return ImageTexture.create_from_image(img)

func prism(label: String, center: Vector3, size: Vector3, material: Material, collision: bool = false, parent: Node3D = null) -> MeshInstance3D:
    var holder: Node3D = self if parent == null else parent
    var visual := MeshInstance3D.new()
    visual.name = label
    var box := BoxMesh.new()
    box.size = size
    visual.mesh = box
    visual.position = center
    visual.material_override = material
    holder.add_child(visual)
    if collision:
        var body := StaticBody3D.new()
        body.name = "Collider_" + label
        body.position = center
        body.collision_layer = 1
        var shape := CollisionShape3D.new()
        var cb := BoxShape3D.new()
        cb.size = size
        shape.shape = cb
        body.add_child(shape)
        holder.add_child(body)
    return visual

func tapered_piece(label: String, pos: Vector3, top_radius: float, bottom_radius: float, height: float, surface: Material, parent: Node3D) -> MeshInstance3D:
    var m := CylinderMesh.new()
    m.top_radius = top_radius
    m.bottom_radius = bottom_radius
    m.height = height
    m.radial_segments = 9
    var piece := MeshInstance3D.new()
    piece.name = label
    piece.mesh = m
    piece.material_override = surface
    piece.position = pos
    parent.add_child(piece)
    return piece

func anchor(key: String, pos: Vector3, label: String) -> void:
    markers[key] = {"position":pos, "label":label}

func _ready() -> void:
    var decay_shader := load("res://shaders/decay_wall.gdshader") as Shader
    paint_clean = ShaderMaterial.new()
    paint_clean.shader = decay_shader
    paint_clean.set_shader_parameter("damage", 0.07)
    paint_clean.set_shader_parameter("plaster_color", Color(0.74, 0.76, 0.75, 1.0))
    paint_ruined = ShaderMaterial.new()
    paint_ruined.shader = decay_shader
    paint_ruined.set_shader_parameter("damage", 0.70)
    paint_ruined.set_shader_parameter("plaster_color", Color(0.39, 0.41, 0.42, 1.0))
    paint_warped = ShaderMaterial.new()
    paint_warped.shader = decay_shader
    paint_warped.set_shader_parameter("damage", 0.92)
    paint_warped.set_shader_parameter("plaster_color", Color(0.12, 0.10, 0.12, 1.0))
    paint_warped.set_shader_parameter("stain_color", Color(0.35, 0.085, 0.08, 1.0))
    paint_clean.set_shader_parameter("lower_color", Color(0.25, 0.29, 0.285))
    paint_ruined.set_shader_parameter("lower_color", Color(0.13, 0.15, 0.15))
    paint_warped.set_shader_parameter("lower_color", Color(0.10, 0.065, 0.07))
    # World-space, wet and aged checkerboard. Repeated at 58 cm instead of
    # stretching 8 squares across an entire twelve-meter room.
    var floor_shader := load("res://shaders/aged_tiles.gdshader") as Shader
    tiles_clean = ShaderMaterial.new()
    tiles_clean.shader = floor_shader
    tiles_clean.set_shader_parameter("damage", 0.10)
    tiles_clean.set_shader_parameter("wetness", 0.42)
    tiles_ruined = ShaderMaterial.new()
    tiles_ruined.shader = floor_shader
    tiles_ruined.set_shader_parameter("damage", 0.84)
    tiles_ruined.set_shader_parameter("wetness", 0.78)
    iron = mat(Color(0.31,0.32,0.34))
    rust = mat(Color(0.27,0.18,0.15))
    cloth = mat(Color(0.80,0.80,0.74))
    blood = mat(Color(0.34,0.035,0.035))
    dark = mat(Color(0.095,0.10,0.11))
    build_shell()
    build_room()
    build_corridor()
    build_neighbor()
    build_archive()
    build_treatment()
    build_architecture()
    build_lighting()
    ambience = preload("res://scripts/atmosphere.gd").new()
    ambience.name = "HospitalSetDressing"
    add_child(ambience)
    set_reality(0)

func shell_floor(center: Vector3, size: Vector3, label: String) -> void:
    floors.append(prism(label, center, size, tiles_clean, true))

func shell_wall(center: Vector3, size: Vector3, label: String) -> void:
    walls.append(prism(label, center, size, paint_clean, true))

func build_shell() -> void:
    shell_floor(Vector3(0, -0.18, 0), Vector3(12,0.35,12), "WardFloor")
    shell_floor(Vector3(0, -0.18, 17), Vector3(4,0.35,22), "CorridorFloor")
    shell_floor(Vector3(6.4, -0.18, 20), Vector3(8.8,0.35,10), "NeighborFloor")
    shell_floor(Vector3(-4.5,-0.18,14),Vector3(5.0,0.35,6.0),"ArchiveFloor")
    shell_floor(Vector3(5.0,-0.18,11.0),Vector3(6.0,0.35,6.0),"TreatmentFloor")
    shell_wall(Vector3(-6,1.6,0), Vector3(0.3,3.2,12), "WardWest")
    shell_wall(Vector3(6,1.6,0), Vector3(0.3,3.2,12), "WardEast")
    shell_wall(Vector3(0,1.6,-6), Vector3(12,3.2,0.3), "WardSouth")
    shell_wall(Vector3(-3.9,1.6,6), Vector3(4.2,3.2,0.3), "WardNorthLeft")
    shell_wall(Vector3(3.9,1.6,6), Vector3(4.2,3.2,0.3), "WardNorthRight")
    # A readable side room reached without changing the known corridor route.
    shell_wall(Vector3(-2,1.6,8.5), Vector3(0.26,3.2,5.0), "CorridorWestNear")
    shell_wall(Vector3(-2,1.6,22.5), Vector3(0.26,3.2,11.0), "CorridorWestFar")
    shell_wall(Vector3(-7.0,1.6,14), Vector3(0.28,3.2,6.0), "ArchiveWest")
    shell_wall(Vector3(-4.5,1.6,11.0), Vector3(5.0,3.2,0.28), "ArchiveSouth")
    shell_wall(Vector3(-4.5,1.6,17.0), Vector3(5.0,3.2,0.28), "ArchiveNorth")
    # An eastern treatment room opens directly from the station.
    shell_wall(Vector3(2,1.6,7.5), Vector3(0.26,3.2,3), "CorridorEastEntry")
    shell_wall(Vector3(2,1.6,14.5), Vector3(0.26,3.2,5), "CorridorEastMid")
    shell_wall(Vector3(8.0,1.6,11.0),Vector3(0.3,3.2,6.0),"TreatmentEast")
    shell_wall(Vector3(5.0,1.6,8.0),Vector3(6.0,3.2,0.3),"TreatmentSouth")
    shell_wall(Vector3(5.0,1.6,14.0),Vector3(6.0,3.2,0.3),"TreatmentNorth")
    shell_wall(Vector3(2,1.6,24.5), Vector3(0.26,3.2,7), "CorridorEastFar")
    shell_wall(Vector3(0,1.6,28), Vector3(4,3.2,0.28), "FarBoundary")
    shell_wall(Vector3(11,1.6,20), Vector3(0.3,3.2,10), "NeighborEast")
    shell_wall(Vector3(6.5,1.6,15), Vector3(9,3.2,0.3), "NeighborNorth")
    shell_wall(Vector3(6.5,1.6,25), Vector3(9,3.2,0.3), "NeighborSouth")
    prism("WardCeiling",Vector3(0,3.2,0),Vector3(12,0.25,12),dark,true)
    prism("CorridorCeiling",Vector3(0,3.2,17),Vector3(4,0.25,22),dark,true)
    prism("NeighborCeiling",Vector3(6.5,3.2,20),Vector3(9,0.25,10),dark,true)
    prism("ArchiveCeiling",Vector3(-4.5,3.2,14),Vector3(5,0.25,6),dark,true)
    prism("TreatmentCeiling",Vector3(5,3.2,11),Vector3(6,0.25,6),dark,true)

func build_room() -> void:
    # Recognizable furnishings occupy identical physical coordinates in each reality.
    prism("BedLegA",Vector3(1.1,0.42,-2.9),Vector3(0.08,0.85,0.08),iron)
    prism("BedLegB",Vector3(3.0,0.42,-2.9),Vector3(0.08,0.85,0.08),iron)
    prism("BedLegC",Vector3(1.1,0.42,-0.3),Vector3(0.08,0.85,0.08),iron)
    prism("BedLegD",Vector3(3.0,0.42,-0.3),Vector3(0.08,0.85,0.08),iron)
    prism("BedFrame",Vector3(2.05,0.62,-1.6),Vector3(1.94,0.13,2.75),iron,true)
    var sheet := prism("CleanSheet",Vector3(2.05,0.80,-1.6),Vector3(1.83,0.23,2.62),cloth)
    clean_props.append(sheet)
    var rotten_sheet := prism("RottenMattress",Vector3(2.05,0.78,-1.6),Vector3(1.84,0.15,2.55),rust)
    ruined_props.append(rotten_sheet)
    var pillow := prism("Pillow",Vector3(2.05,0.96,-2.62),Vector3(0.96,0.15,0.42),cloth)
    clean_props.append(pillow)
    var bedside := prism("BedsideCabinet",Vector3(3.85,0.47,-2.2),Vector3(0.83,0.9,0.77),dark,true)
    var cup := prism("Mug",Vector3(3.8,0.98,-2.2),Vector3(0.18,0.16,0.18),cloth)
    clean_props.append(cup)
    var chair := prism("OldChairSeat",Vector3(-0.6,0.46,-2.1),Vector3(0.66,0.12,0.64),iron,true)
    prism("OldChairBack",Vector3(-0.6,0.86,-2.41),Vector3(0.66,0.77,0.12),iron)
    for xx in [-0.83,-0.37]:
        for zz in [-2.33,-1.90]:
            prism("ChairLeg",Vector3(xx,0.2,zz),Vector3(0.08,0.4,0.08),iron)
    # Radiator with fixed metal bracket. The bracket is invariant even when pipes disappear.
    prism("RadiatorBracket",Vector3(-5.78,0.97,-1),Vector3(0.34,0.14,0.62),iron)
    var tubes := Node3D.new()
    tubes.name = "RadiatorTubes"
    add_child(tubes)
    for i in 8:
        prism("RadiatorTube",Vector3(-5.45,0.89,-2 + float(i)*0.27),Vector3(0.32,1.17,0.12),iron,false,tubes)
    clean_props.append(tubes)
    var rusty_tubes := Node3D.new()
    rusty_tubes.name = "MissingPipesOnlyBrackets"
    add_child(rusty_tubes)
    for i in 3:
        prism("BrokenRadiator",Vector3(-5.45,0.73,-1.9 + float(i)*0.8),Vector3(0.3,0.66,0.11),rust,false,rusty_tubes)
    ruined_props.append(rusty_tubes)
    # Window + bars, kept dark enough to make it an orientation anchor.
    frosted_glass = ShaderMaterial.new()
    frosted_glass.shader = load("res://shaders/frosted_window.gdshader") as Shader
    prism("FrostedWardGlass",Vector3(0,1.95,-5.78),Vector3(2.72,1.53,0.08),frosted_glass)
    for i in 5:
        prism("WindowBarsV",Vector3(-1.2+float(i)*0.6,1.95,-5.67),Vector3(0.055,1.55,0.11),iron)
    for i in 3:
        prism("WindowBarsH",Vector3(0,1.35+float(i)*0.6,-5.64),Vector3(2.75,0.055,0.12),iron)
    window_leaf = prism("WindowLatch",Vector3(0.7,1.6,-5.48),Vector3(0.15,0.34,0.18),cloth)
    # Grime streaks and fractured wall sections are visible only in ruins.
    var stain := prism("BrokenPlaster",Vector3(-5.78,2.1,2.0),Vector3(0.05,0.8,1.5),rust)
    ruined_props.append(stain)
    var bend := prism("ImpossibleRadiatorShadow",Vector3(-3.9,3.02,-1),Vector3(3.9,0.03,0.18),blood)
    bend.rotation.z = 0.18
    warped_props.append(bend)
    # Nurse occupies the doorway briefly; geometry is deliberately angular.
    nurse = Node3D.new()
    nurse.name = "Nurse"
    nurse.position = Vector3(0.6,0,4.5)
    add_child(nurse)
    # Silhouette of a nurse, not a stack of rectangular boxes.
    tapered_piece("NurseGown",Vector3(0,0.89,0),0.25,0.42,1.22,cloth,nurse)
    tapered_piece("NurseJacket",Vector3(0,1.42,0),0.29,0.23,0.67,cloth,nurse)
    var nurse_skin := mat(Color(0.37,0.345,0.323))
    tapered_piece("Neck",Vector3(0,1.81,-0.01),0.09,0.10,0.21,nurse_skin,nurse)
    var face := SphereMesh.new()
    face.radius = 0.205
    face.height = 0.39
    face.radial_segments = 9
    face.rings = 5
    var nurse_head := MeshInstance3D.new()
    nurse_head.name = "NurseHead"
    nurse_head.mesh = face
    nurse_head.material_override = nurse_skin
    nurse_head.position = Vector3(0,2.00,-0.01)
    nurse.add_child(nurse_head)
    tapered_piece("NurseCap",Vector3(0,2.20,0.02),0.19,0.21,0.16,cloth,nurse)
    for side in [-1.0,1.0]:
        var sleeve := tapered_piece("NurseSleeve",Vector3(side*0.33,1.40,-0.025),0.13,0.105,0.73,cloth,nurse)
        sleeve.rotation.z = side * -0.17
        tapered_piece("NurseLeg",Vector3(side*0.15,0.2,0),0.095,0.075,0.39,dark,nurse)
        prism("NurseShoe",Vector3(side*0.15,0.065,-0.06),Vector3(0.19,0.12,0.34),dark,false,nurse)
    prism("FoldedLinen",Vector3(0,1.18,-0.36),Vector3(0.67,0.12,0.34),cloth,false,nurse)
    prism("FoldedLinenEdge",Vector3(0,1.25,-0.37),Vector3(0.66,0.024,0.33),mat(Color(0.62,0.62,0.59)),false,nurse)
    room_leaf = prism("WardDoor",Vector3(1.15,1.22,5.8),Vector3(0.18,2.44,2.65),dark)
    room_leaf.visible = false
    anchor("radiator",Vector3(-5.10,0.5,-1.0),"Прислушаться / постучать")
    anchor("window",Vector3(0,1.3,-5.0),"Закрыть форточку")
    anchor("bed",Vector3(2.05,0.8,-1.6),"Кровать")
    anchor("cup",Vector3(3.80,0.9,-2.2),"Кружка")
    anchor("ward_door",Vector3(0,1.0,5.5),"Палата")

func build_corridor() -> void:
    far_leaf = prism("LoopDoor",Vector3(0,1.23,27.83),Vector3(2.4,2.44,0.10),dark)
    prism("LoopDoorHandle",Vector3(0.78,1.14,27.65),Vector3(0.36,0.09,0.10),iron)
    neighbor_leaf = prism("NeighborDoor",Vector3(1.91,1.23,20.0),Vector3(0.13,2.44,2.7),dark)
    neighbor_barrier = StaticBody3D.new()
    neighbor_barrier.name = "NeighborLockCollision"
    neighbor_barrier.position = Vector3(1.9,1.23,20)
    neighbor_barrier.collision_layer = 1
    var cs := CollisionShape3D.new()
    var box := BoxShape3D.new()
    box.size = Vector3(0.18,2.44,2.7)
    cs.shape = box
    neighbor_barrier.add_child(cs)
    add_child(neighbor_barrier)
    prism("LockSpindle",Vector3(1.72,1.16,20.5),Vector3(0.13,0.12,0.15),iron)
    for z in [11.0,15.2,22.0,25.5]:
        prism("CorridorCeilingLightFrame",Vector3(0,3.01,z),Vector3(1.0,0.08,0.32),iron)
    prism("NurseCounter",Vector3(-1.4,0.85,10.5),Vector3(1.0,1.3,2.1),iron,true)
    prism("NurseSign",Vector3(-1.8,1.95,10.5),Vector3(0.04,0.32,1.2),cloth)
    var trolley := Node3D.new()
    trolley.name = "Trolley"
    add_child(trolley)
    for z in [17.2,17.95]:
        prism("GurneyWheel",Vector3(1.1,0.2,z),Vector3(0.12,0.4,0.17),iron,false,trolley)
    prism("GurneyTray",Vector3(1.0,0.95,17.6),Vector3(0.95,0.12,1.5),iron,false,trolley)
    anchor("far_door",Vector3(0,1,27.0),"Открыть дверь")
    anchor("neighbor_door",Vector3(1.5,1,20),"Дверь соседней палаты")
    anchor("station_exit",Vector3(-0.8,1,10.0),"Уйти через пост")
    anchor("medical_record",Vector3(-1.8,1,10.5),"Карточка пациента")
    anchor("trolley",Vector3(1,0.9,17.6),"Каталка")
    var impossible := prism("ImpossiblePassage",Vector3(0,2.62,22.4),Vector3(3.6,0.15,0.23),blood)
    impossible.rotation.z = 0.39
    warped_props.append(impossible)
    var shadow_leak := prism("LivingHospitalShadow",Vector3(1.7,0.03,20.0),Vector3(0.18,0.02,2.5),blood)
    warped_props.append(shadow_leak)

func build_archive() -> void:
    prism("ArchiveDesk",Vector3(-5.4,0.67,15),Vector3(1.7,1.05,0.68),iron,true)
    for i in 4:
        prism("ArchiveLedger",Vector3(-5.9+float(i)*0.23,1.21,15),Vector3(0.16,0.06,0.42),cloth)
    prism("ArchiveShelf",Vector3(-6.64,1.45,13.0),Vector3(0.32,2.5,2.15),dark,true)
    for y in [0.5,1.2,1.9]:
        prism("ArchiveRack",Vector3(-6.35,y,13.0),Vector3(0.6,0.08,2.18),iron)
    anchor("archive_record",Vector3(-5.2,1.0,15.0),"История болезни")

func build_treatment() -> void:
    prism("TreatmentBed",Vector3(6.3,0.55,11.6),Vector3(1.65,0.18,2.95),iron,true)
    var sheet := prism("TreatmentSheet",Vector3(6.3,0.70,11.6),Vector3(1.49,0.16,2.78),cloth)
    clean_props.append(sheet)
    var rotten := prism("TreatmentRot",Vector3(6.3,0.70,11.6),Vector3(1.52,0.16,2.78),rust)
    ruined_props.append(rotten)
    prism("TreatmentWardrobe",Vector3(7.38,1.08,9.15),Vector3(0.82,2.12,0.8),dark,true)
    prism("TreatmentTray",Vector3(3.75,0.72,12.65),Vector3(0.8,0.09,0.52),iron)
    for i in 3:
        prism("TreatmentGlassBottle",Vector3(3.55+float(i)*0.2,0.88,12.6),Vector3(0.13,0.28+float(i%2)*0.16,0.13),cloth)
    prism("TreatmentRail",Vector3(6.3,1.35,10.13),Vector3(1.5,0.08,0.09),iron)
    prism("OperatingLight",Vector3(5.0,2.92,11.0),Vector3(1.0,0.12,0.51),cloth)
    anchor("treatment_notes",Vector3(3.65,0.95,12.6),"Медицинский поднос")

func build_neighbor() -> void:
    prism("NeighborBed",Vector3(7.1,0.57,19.7),Vector3(2.3,0.2,3),iron,true)
    prism("NeighborMattress",Vector3(7.1,0.73,19.7),Vector3(2.2,0.15,2.88),rust)
    prism("NeighborLamp",Vector3(7,2.8,17.9),Vector3(0.6,0.13,0.28),cloth)
    shadow = prism("OtherHandShadow",Vector3(2.12,1.03,20.4),Vector3(0.04,0.64,0.2),blood)
    shadow.visible = false
    anchor("neighbor_inside",Vector3(4,1,20),"За дверью")

func build_architecture() -> void:
    # Human-scale details. Keep these free of collision so they do not block
    # the established puzzle route or the interaction anchor positions.
    var tarnished := mat(Color(0.14, 0.155, 0.155))
    var pale_iron := mat(Color(0.34, 0.345, 0.345))
    var cracked := mat(Color(0.19, 0.18, 0.17))
    var enamel_lamp := mat(Color(0.86, 0.87, 0.81))
    enamel_lamp.emission_enabled = true
    enamel_lamp.emission = Color(0.63, 0.67, 0.61)
    enamel_lamp.emission_energy_multiplier = 1.6
    # Perimeter molding and dark skirting produce legible room dimensions.
    for z in [-5.82, 5.82]:
        prism("WardSkirting",Vector3(0,0.12,z),Vector3(11.6,0.24,0.07),tarnished)
        prism("WardCornice",Vector3(0,3.01,z),Vector3(11.6,0.055,0.065),pale_iron)
    for x in [-5.82, 5.82]:
        prism("WardSkirting",Vector3(x,0.12,0),Vector3(0.07,0.24,11.6),tarnished)
        prism("WardCornice",Vector3(x,3.01,0),Vector3(0.07,0.055,11.6),pale_iron)
    for x in [-1.85, 1.85]:
        prism("HallSkirting",Vector3(x,0.12,17),Vector3(0.07,0.24,21.6),tarnished)
        prism("HallMolding",Vector3(x,1.43,17),Vector3(0.06,0.045,21.6),pale_iron)
        prism("HallCornice",Vector3(x,3.02,17),Vector3(0.06,0.06,21.6),tarnished)
    # Doors and windows must have frames, thresholds and a sense of depth.
    for x in [-1.55,1.55]:
        prism("WardDoorJamb",Vector3(x,1.19,5.76),Vector3(0.17,2.38,0.27),pale_iron)
    prism("WardDoorLintel",Vector3(0,2.41,5.76),Vector3(3.25,0.16,0.27),pale_iron)
    prism("WardDoorThreshold",Vector3(0,0.023,5.77),Vector3(3.16,0.055,0.26),tarnished)
    for x in [-1.48,1.48]:
        prism("WindowJamb",Vector3(x,1.96,-5.65),Vector3(0.10,1.69,0.18),cracked)
    for y in [1.12,2.77]:
        prism("WindowLintel",Vector3(0,y,-5.65),Vector3(3.05,0.1,0.18),cracked)
    prism("WindowSill",Vector3(0,1.08,-5.48),Vector3(3.1,0.09,0.38),pale_iron)
    # Hanging light tubes have real emissive surfaces, not white non-emissive blocks.
    for z in [0.0, 10.8, 15.5, 22.0, 25.5]:
        for x in [-0.19, 0.19]:
            prism("LitFluorescentTube",Vector3(x,2.895,z),Vector3(0.065,0.055,0.29),enamel_lamp)
    # Exposed electrical conduits and cracked tile edging break up the box look.
    for side in [-1.0,1.0]:
        prism("WardElectricalConduit",Vector3(side*5.83,2.78,-1.3),Vector3(0.065,0.05,6.6),pale_iron)
        for z in [-4.5,-2.1,0.4]:
            prism("PipeClamp",Vector3(side*5.81,2.79,z),Vector3(0.10,0.10,0.08),tarnished)
    for i in 8:
        var z: float = 7.3 + float(i)*2.6
        prism("HallCeilingSeam",Vector3(0,3.045,z),Vector3(3.65,0.018,0.02),tarnished)


func build_lighting() -> void:
    var env_world := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.015,0.017,0.021)
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.22,0.23,0.25)
    env.ambient_light_energy = 0.52
    env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    env.fog_enabled = true
    env.fog_light_color = Color(0.105, 0.115, 0.12)
    env.fog_density = 0.013
    env_world.environment = env
    add_child(env_world)
    room_light = OmniLight3D.new()
    room_light.position = Vector3(0,2.75,0)
    room_light.omni_range = 9.5
    room_light.light_energy = 1.65
    room_light.light_color = Color(0.83,0.85,0.9)
    room_light.shadow_enabled = true
    add_child(room_light)
    outside_light = OmniLight3D.new()
    outside_light.name = "DiffuseWindowGlow"
    outside_light.position = Vector3(0,2.0,-4.95)
    outside_light.omni_range = 5.5
    outside_light.light_energy = 0.43
    outside_light.light_color = Color(0.62,0.68,0.70)
    outside_light.shadow_enabled = false
    add_child(outside_light)
    hall_light = OmniLight3D.new()
    hall_light.position = Vector3(0,2.7,19.0)
    hall_light.omni_range = 9.5
    hall_light.light_energy = 1.32
    hall_light.light_color = Color(0.82,0.84,0.91)
    add_child(hall_light)
    # Distinct pools of cold light along the hall: avoid uniformly illuminating
    # the entire corridor, while retaining readable navigation on mobile.
    for z in [10.8, 15.5, 25.5]:
        var fixture := OmniLight3D.new()
        fixture.name = "HallPractical"
        fixture.position = Vector3(0,2.84,z)
        fixture.omni_range = 5.5
        fixture.light_energy = 0.72
        fixture.light_color = Color(0.69,0.77,0.78)
        fixture.shadow_enabled = false
        add_child(fixture)
    var treatment_light := OmniLight3D.new()
    treatment_light.position = Vector3(5.2,2.72,11.0)
    treatment_light.omni_range = 9.0
    treatment_light.light_energy = 0.79
    treatment_light.light_color = Color(0.82,0.85,0.84)
    add_child(treatment_light)
    var archive_light := OmniLight3D.new()
    archive_light.position = Vector3(-4.5,2.75,14)
    archive_light.omni_range = 9.5
    archive_light.light_energy = 0.64
    archive_light.light_color = Color(0.78,0.77,0.74)
    add_child(archive_light)
    var neighbor_light := OmniLight3D.new()
    neighbor_light.position = Vector3(6,2.7,20)
    neighbor_light.omni_range = 9
    neighbor_light.light_energy = 0.7
    neighbor_light.light_color = Color(0.73,0.74,0.75)
    add_child(neighbor_light)

func set_reality(new_reality: int, mixing: bool = false) -> void:
    reality = new_reality
    mixed = mixing
    for w in walls:
        w.material_override = paint_clean if reality == 0 else (paint_ruined if reality == 1 else paint_warped)
    for f in floors:
        f.material_override = tiles_clean if reality == 0 else tiles_ruined
    for p in clean_props:
        p.visible = reality == 0 or mixing
    for p in ruined_props:
        p.visible = reality != 0 or mixing
    for p in warped_props:
        p.visible = reality == 2 or mixing
    if mixing:
        for w in walls:
            w.material_override = paint_ruined
    room_light.light_color = Color(0.75,0.83,0.90) if reality != 2 else Color(0.68,0.18,0.19)
    hall_light.light_color = Color(0.85,0.86,0.88) if reality == 0 else (Color(0.48,0.49,0.50) if reality == 1 else Color(0.78,0.13,0.15))
    hall_light.light_energy = 0.75 if reality == 1 else 1.35
    room_light.light_energy = 1.65 if reality == 0 else 0.85
    outside_light.light_energy = 0.43 if reality == 0 else (0.24 if reality == 1 else 0.11)
    frosted_glass.set_shader_parameter("light_strength", 0.83 if reality == 0 else (0.42 if reality == 1 else 0.27))
    shadow.visible = mixing
    if is_instance_valid(ambience):
        ambience.call("set_reality", new_reality, mixing)

func open_neighbor_door() -> void:
    neighbor_leaf.position = Vector3(2.08,1.23,21.28)
    neighbor_leaf.rotation.y = PI / 2.4
    if is_instance_valid(neighbor_barrier):
        neighbor_barrier.queue_free()
        neighbor_barrier = null
