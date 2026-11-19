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

func make_part(parent: Node3D, mesh: Mesh, location: Vector3, color: Color) -> void:
    var item := MeshInstance3D.new()
    item.mesh = mesh
    item.position = location
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = 1.0
    material.shading_mode = BaseMaterial3D.SHADING_MODE_PER_PIXEL
    item.material_override = material
    parent.add_child(item)

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
    make_part(body_mesh, box_mesh(Vector3(0.68, 0.76, 0.38)), Vector3(0, 1.11, 0), Color(0.16,0.16,0.17))
    make_part(body_mesh, box_mesh(Vector3(0.28, 0.7, 0.33)), Vector3(-0.52, 1.05, 0), Color(0.16,0.16,0.17))
    make_part(body_mesh, box_mesh(Vector3(0.28, 0.7, 0.33)), Vector3(0.52, 1.05, 0), Color(0.16,0.16,0.17))
    make_part(body_mesh, box_mesh(Vector3(0.28, 0.75, 0.34)), Vector3(-0.19, 0.43, 0), Color(0.14,0.14,0.15))
    make_part(body_mesh, box_mesh(Vector3(0.28, 0.75, 0.34)), Vector3(0.19, 0.43, 0), Color(0.14,0.14,0.15))
    var head := SphereMesh.new()
    head.radius = 0.27
    head.height = 0.55
    head.radial_segments = 7
    head.rings = 4
    make_part(body_mesh, head, Vector3(0, 1.74, 0), Color(0.28,0.265,0.25))
    make_part(body_mesh, box_mesh(Vector3(0.56, 0.13, 0.54)), Vector3(0, 1.89, 0.03), Color(0.07,0.07,0.08))

    pivot = Node3D.new()
    pivot.position = Vector3(0, 1.64, 0)
    pivot.name = "ViewPivot"
    add_child(pivot)
    var spring := SpringArm3D.new()
    spring.spring_length = 2.6
    spring.margin = 0.12
    var probe := SphereShape3D.new()
    probe.radius = 0.25
    spring.shape = probe
    spring.collision_mask = 1
    spring.add_excluded_object(get_rid())
    pivot.add_child(spring)
    camera = Camera3D.new()
    camera.position = Vector3(0.45, 0.1, 0)
    camera.fov = 72
    camera.current = true
    spring.add_child(camera)
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
