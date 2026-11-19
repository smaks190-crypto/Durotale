extends CharacterBody3D
class_name DurkaPlayer

const SPEED := 3.35
const GRAVITY := 16.0
var sensitivity := 0.0026
var yaw := 0.0
var pitch := -0.04
var paused := false
var camera: Camera3D
var pivot: Node3D
var body_mesh: Node3D
var game: Node3D

func make_part(parent: Node3D, mesh: Mesh, location: Vector3, color: Color) -> MeshInstance3D:
    var item := MeshInstance3D.new()
    item.mesh = mesh
    item.position = location
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = 0.94
    material.shading_mode = BaseMaterial3D.SHADING_MODE_PER_PIXEL
    item.material_override = material
    parent.add_child(item)
    return item

func faceted_cylinder(top: float, bottom: float, height: float, sides: int = 6) -> CylinderMesh:
    var mesh := CylinderMesh.new()
    mesh.top_radius = top
    mesh.bottom_radius = bottom
    mesh.height = height
    mesh.radial_segments = sides
    return mesh

func box_mesh(size: Vector3) -> BoxMesh:
    var m := BoxMesh.new()
    m.size = size
    return m

func _ready() -> void:
    name = "Patient"
    collision_layer = 2
    collision_mask = 1
    floor_snap_length = 0.2
    var collider := CollisionShape3D.new()
    var capsule := CapsuleShape3D.new()
    capsule.radius = 0.32
    capsule.height = 1.8
    collider.shape = capsule
    collider.position.y = 0.91
    add_child(collider)

    body_mesh = Node3D.new()
    body_mesh.name = "LowPolyPatient"
    add_child(body_mesh)
    # Low-poly, tapered human silhouette rather than Roblox boxes.
    var coat := Color(0.34,0.34,0.33)
    var wool := Color(0.42,0.41,0.39)
    var skin := Color(0.44,0.398,0.355)
    var shoes := Color(0.073,0.075,0.079)
    make_part(body_mesh, faceted_cylinder(0.295,0.43,1.08,7),Vector3(0,1.04,0),coat)
    make_part(body_mesh, faceted_cylinder(0.32,0.37,0.22,7),Vector3(0,1.46,0),wool)
    for side in [-1.0,1.0]:
        var sleeve := make_part(body_mesh,faceted_cylinder(0.185,0.135,0.74,6),Vector3(side*0.46,1.04,0),coat)
        sleeve.rotation.z = side * 0.20
        make_part(body_mesh,faceted_cylinder(0.115,0.125,0.42,6),Vector3(side*0.18,0.48,0),Color(0.13,0.135,0.14))
        var glove := SphereMesh.new()
        glove.radius = 0.14
        glove.height = 0.25
        glove.radial_segments = 6
        glove.rings = 3
        make_part(body_mesh,glove,Vector3(side*0.54,0.62,-0.04),skin)
        make_part(body_mesh,box_mesh(Vector3(0.10,0.26,0.08)),Vector3(side*0.155,1.47,-0.3),Color(0.30,0.29,0.28))
        var shoe := make_part(body_mesh,box_mesh(Vector3(0.25,0.17,0.43)),Vector3(side*0.18,0.10,-0.08),shoes)
        shoe.rotation.y = side*0.08
    var head := SphereMesh.new()
    head.radius = 0.27
    head.height = 0.52
    head.radial_segments = 11
    head.rings = 7
    make_part(body_mesh,head,Vector3(0,1.74,0),skin)
    var hair := SphereMesh.new()
    hair.radius = 0.254
    hair.height = 0.22
    hair.radial_segments = 10
    hair.rings = 5
    make_part(body_mesh,hair,Vector3(0,1.93,0.032),Color(0.105,0.10,0.10))
    var back_hair := SphereMesh.new()
    back_hair.radius = 0.255
    back_hair.height = 0.32
    back_hair.radial_segments = 10
    back_hair.rings = 5
    make_part(body_mesh,back_hair,Vector3(0,1.83,0.115),Color(0.097,0.092,0.088))
    make_part(body_mesh,box_mesh(Vector3(0.19,0.085,0.12)),Vector3(0,1.89,0.235),Color(0.125,0.12,0.115))
    make_part(body_mesh,box_mesh(Vector3(0.13,0.12,0.12)),Vector3(0,1.72,-0.25),skin)
    make_part(body_mesh,box_mesh(Vector3(0.12,0.31,0.06)),Vector3(0,1.20,-0.34),Color(0.22,0.22,0.225))
    # The rear of an old civilian jacket needs silhouette and fabric structure,
    # not a single featureless cone. These patches have no collision.
    var seam := Color(0.24,0.25,0.245)
    make_part(body_mesh,box_mesh(Vector3(0.026,0.64,0.038)),Vector3(0,1.02,0.365),seam)
    make_part(body_mesh,box_mesh(Vector3(0.54,0.055,0.11)),Vector3(0,1.38,0.285),wool)
    for side in [-1.0,1.0]:
        var shoulder := make_part(body_mesh,box_mesh(Vector3(0.19,0.055,0.26)),Vector3(side*0.32,1.49,0.055),coat)
        shoulder.rotation.z = side * 0.14
        var back_pleat := make_part(body_mesh,box_mesh(Vector3(0.035,0.61,0.028)),Vector3(side*0.23,1.03,0.323),seam)
        back_pleat.rotation.z = side * 0.07
        make_part(body_mesh,box_mesh(Vector3(0.15,0.11,0.046)),Vector3(side*0.29,0.86,0.31),wool)
    var fill := OmniLight3D.new()
    fill.name = "SubtleCharacterFill"
    fill.position = Vector3(0.8,2.15,1.35)
    fill.omni_range = 3.1
    fill.light_energy = 0.65
    fill.light_color = Color(0.77,0.78,0.80)
    fill.shadow_enabled = false
    add_child(fill)

    pivot = Node3D.new()
    pivot.position = Vector3(0, 1.64, 0)
    pivot.name = "ViewPivot"
    add_child(pivot)
    var spring := SpringArm3D.new()
    spring.spring_length = 2.38
    spring.margin = 0.12
    var probe := SphereShape3D.new()
    probe.radius = 0.25
    spring.shape = probe
    spring.collision_mask = 1
    spring.add_excluded_object(get_rid())
    pivot.add_child(spring)
    # SpringArm3D rewrites direct children's position! A nested mount keeps
    # the camera's lateral shoulder offset while the arm handles collisions.
    var shoulder_mount := Node3D.new()
    shoulder_mount.name = "OverShoulderMount"
    spring.add_child(shoulder_mount)
    camera = Camera3D.new()
    camera.position = Vector3(1.24, 0.16, 0)
    camera.fov = 69
    camera.current = true
    shoulder_mount.add_child(camera)
    apply_orientation()
    if not OS.has_feature("mobile"):
        Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func apply_orientation() -> void:
    rotation.y = yaw
    if is_instance_valid(pivot):
        pivot.rotation.x = pitch

func _unhandled_input(event: InputEvent) -> void:
    if paused:
        return
    if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
        look_by(event.relative.x, event.relative.y)
    elif event is InputEventScreenDrag:
        var viewport_width := get_viewport().get_visible_rect().size.x
        if event.position.x > viewport_width * 0.44:
            look_by(event.relative.x * 1.6, event.relative.y * 1.6)

func look_by(dx: float, dy: float) -> void:
    yaw -= dx * sensitivity
    pitch = clampf(pitch - dy * sensitivity, -0.65, 0.6)
    apply_orientation()

func _physics_process(delta: float) -> void:
    if paused:
        velocity = Vector3.ZERO
        return
    var direction := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
    var local_step := Vector3(direction.x, 0, direction.y)
    local_step = local_step.rotated(Vector3.UP, yaw)
    velocity.x = local_step.x * SPEED
    velocity.z = local_step.z * SPEED
    if not is_on_floor():
        velocity.y -= GRAVITY * delta
    else:
        velocity.y = -0.1
    move_and_slide()

func facing_direction() -> Vector3:
    return Vector3(0, 0, -1).rotated(Vector3.UP, yaw)
