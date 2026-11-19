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
var window_leaf: MeshInstance3D
var shadow: MeshInstance3D
var hall_light: OmniLight3D
var room_light: OmniLight3D
var reality := 0
var mixed := false

var paint_clean: StandardMaterial3D
var paint_ruined: StandardMaterial3D
var paint_warped: StandardMaterial3D
var tiles_clean: StandardMaterial3D
var tiles_ruined: StandardMaterial3D
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

func anchor(key: String, pos: Vector3, label: String) -> void:
    markers[key] = {"position":pos, "label":label}

func _ready() -> void:
    var grunge := make_grime(false)
    var chess := make_grime(true)
    paint_clean = mat(Color(0.74,0.76,0.75), grunge)
    paint_ruined = mat(Color(0.39,0.41,0.42), grunge)
    paint_warped = mat(Color(0.45,0.22,0.24), grunge)
    tiles_clean = mat(Color(0.82,0.83,0.84), chess)
    tiles_ruined = mat(Color(0.40,0.42,0.44), chess)
    iron = mat(Color(0.31,0.32,0.34))
    rust = mat(Color(0.27,0.18,0.15))
    cloth = mat(Color(0.80,0.80,0.74))
    blood = mat(Color(0.34,0.035,0.035))
    dark = mat(Color(0.095,0.10,0.11))
    build_shell()
    build_room()
    build_corridor()
    build_neighbor()
    build_lighting()
    set_reality(0)

func shell_floor(center: Vector3, size: Vector3, label: String) -> void:
    floors.append(prism(label, center, size, tiles_clean, true))

func shell_wall(center: Vector3, size: Vector3, label: String) -> void:
    walls.append(prism(label, center, size, paint_clean, true))

func build_shell() -> void:
    shell_floor(Vector3(0, -0.18, 0), Vector3(12,0.35,12), "WardFloor")
    shell_floor(Vector3(0, -0.18, 17), Vector3(4,0.35,22), "CorridorFloor")
    shell_floor(Vector3(6.4, -0.18, 20), Vector3(8.8,0.35,10), "NeighborFloor")
    shell_wall(Vector3(-6,1.6,0), Vector3(0.3,3.2,12), "WardWest")
    shell_wall(Vector3(6,1.6,0), Vector3(0.3,3.2,12), "WardEast")
    shell_wall(Vector3(0,1.6,-6), Vector3(12,3.2,0.3), "WardSouth")
    shell_wall(Vector3(-3.9,1.6,6), Vector3(4.2,3.2,0.3), "WardNorthLeft")
    shell_wall(Vector3(3.9,1.6,6), Vector3(4.2,3.2,0.3), "WardNorthRight")
    shell_wall(Vector3(-2,1.6,17), Vector3(0.26,3.2,22), "CorridorWest")
    shell_wall(Vector3(2,1.6,11.5), Vector3(0.26,3.2,11), "CorridorEastNear")
    shell_wall(Vector3(2,1.6,24.5), Vector3(0.26,3.2,7), "CorridorEastFar")
    shell_wall(Vector3(0,1.6,28), Vector3(4,3.2,0.28), "FarBoundary")
    shell_wall(Vector3(11,1.6,20), Vector3(0.3,3.2,10), "NeighborEast")
    shell_wall(Vector3(6.5,1.6,15), Vector3(9,3.2,0.3), "NeighborNorth")
    shell_wall(Vector3(6.5,1.6,25), Vector3(9,3.2,0.3), "NeighborSouth")
    prism("WardCeiling",Vector3(0,3.2,0),Vector3(12,0.25,12),dark,true)
    prism("CorridorCeiling",Vector3(0,3.2,17),Vector3(4,0.25,22),dark,true)
    prism("NeighborCeiling",Vector3(6.5,3.2,20),Vector3(9,0.25,10),dark,true)

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
    prism("WindowBlackGlass",Vector3(0,1.95,-5.78),Vector3(2.72,1.53,0.08),dark)
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
    prism("Coat",Vector3(0,1.08,0),Vector3(0.6,1.2,0.35),cloth,false,nurse)
    prism("Head",Vector3(0,1.88,0),Vector3(0.38,0.4,0.35),mat(Color(0.44,0.42,0.40)),false,nurse)
    prism("Cap",Vector3(0,2.13,0),Vector3(0.42,0.13,0.4),cloth,false,nurse)
    prism("FoldedLinen",Vector3(0,1.18,-0.35),Vector3(0.67,0.14,0.4),cloth,false,nurse)
    room_leaf = prism("WardDoor",Vector3(1.15,1.22,5.8),Vector3(0.18,2.44,2.65),dark)
    room_leaf.visible = false
    anchor("radiator",Vector3(-5.10,0.5,-1.0),"Прислушаться / постучать")
    anchor("window",Vector3(0,1.3,-5.0),"Закрыть форточку")
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
    var impossible := prism("ImpossiblePassage",Vector3(0,2.62,22.4),Vector3(3.6,0.15,0.23),blood)
    impossible.rotation.z = 0.39
    warped_props.append(impossible)
    var shadow_leak := prism("LivingHospitalShadow",Vector3(1.7,0.03,20.0),Vector3(0.18,0.02,2.5),blood)
    warped_props.append(shadow_leak)

func build_neighbor() -> void:
    prism("NeighborBed",Vector3(7.1,0.57,19.7),Vector3(2.3,0.2,3),iron,true)
    prism("NeighborMattress",Vector3(7.1,0.73,19.7),Vector3(2.2,0.15,2.88),rust)
    prism("NeighborLamp",Vector3(7,2.8,17.9),Vector3(0.6,0.13,0.28),cloth)
    shadow = prism("OtherHandShadow",Vector3(2.12,1.03,20.4),Vector3(0.04,0.64,0.2),blood)
    shadow.visible = false
    anchor("neighbor_inside",Vector3(4,1,20),"За дверью")

func build_lighting() -> void:
    var env_world := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.015,0.017,0.021)
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.22,0.23,0.25)
    env.ambient_light_energy = 0.38
    env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    env_world.environment = env
    add_child(env_world)
    room_light = OmniLight3D.new()
    room_light.position = Vector3(0,2.75,0)
    room_light.omni_range = 17.0
    room_light.light_energy = 1.5
    room_light.light_color = Color(0.83,0.85,0.9)
    room_light.shadow_enabled = true
    add_child(room_light)
    hall_light = OmniLight3D.new()
    hall_light.position = Vector3(0,2.7,19.0)
    hall_light.omni_range = 16.0
    hall_light.light_energy = 1.2
    hall_light.light_color = Color(0.82,0.84,0.91)
    add_child(hall_light)
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
    shadow.visible = mixing

func open_neighbor_door() -> void:
    neighbor_leaf.position = Vector3(2.08,1.23,21.28)
    neighbor_leaf.rotation.y = PI / 2.4
    if is_instance_valid(neighbor_barrier):
        neighbor_barrier.queue_free()
        neighbor_barrier = null
