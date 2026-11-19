extends CharacterBody3D
class_name DurkaPlayer

const WORN_FABRIC = preload("res://shaders/worn_fabric.gdshader")

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
var walk_cycle := 0.0
var left_sleeve: MeshInstance3D
var right_sleeve: MeshInstance3D

func make_part(parent: Node3D, mesh: Mesh, location: Vector3, color: Color) -> MeshInstance3D:
    var item := MeshInstance3D.new()
    item.mesh = mesh
    item.position = location
    if color.r < 0.325:
        var cloth_shader := ShaderMaterial.new()
        cloth_shader.shader = WORN_FABRIC
        cloth_shader.set_shader_parameter("fabric_tint", color)
        item.material_override = cloth_shader
    else:
        var material := StandardMaterial3D.new()
        material.albedo_color = color
        material.roughness = 0.89
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
    var coat := Color(0.175,0.18,0.187)
    var wool := Color(0.27,0.275,0.281)
    var skin := Color(0.34,0.326,0.307)
    var shoes := Color(0.073,0.075,0.079)
    # The body is tapered like an actual old coat, never a rectangular block.
    make_part(body_mesh, faceted_cylinder(0.34,0.42,0.92,9),Vector3(0,1.09,0),coat)
    make_part(body_mesh, faceted_cylinder(0.36,0.32,0.40,9),Vector3(0,1.51,-0.015),coat)
    make_part(body_mesh, faceted_cylinder(0.30,0.35,0.13,9),Vector3(0,1.49,0),wool)
    make_part(body_mesh, box_mesh(Vector3(0.65,0.09,0.48)),Vector3(0,0.82,-0.015),wool)
    # Shirt is visible beneath the open neckline.
    make_part(body_mesh,box_mesh(Vector3(0.21,0.35,0.045)),Vector3(0,1.54,-0.30),Color(0.295,0.293,0.279))
    for side in [-1.0, 1.0]:
        var lapel := make_part(body_mesh,box_mesh(Vector3(0.14,0.39,0.06)),Vector3(side*0.17,1.52,-0.32),wool)
        lapel.rotation.z = side * 0.30
        make_part(body_mesh,box_mesh(Vector3(0.23,0.19,0.055)),Vector3(side*0.19,1.02,-0.372),wool)
    for y in [0.94,1.12,1.30]:
        make_part(body_mesh,faceted_cylinder(0.017,0.017,0.014,7),Vector3(-0.05,y,-0.378),Color(0.40,0.39,0.37))
    for side in [-1.0,1.0]:
        var sleeve := make_part(body_mesh,faceted_cylinder(0.185,0.135,0.74,6),Vector3(side*0.46,1.04,0),coat)
        sleeve.rotation.z = side * 0.16
        if side < 0.0:
            left_sleeve = sleeve
        else:
            right_sleeve = sleeve
        make_part(body_mesh,faceted_cylinder(0.145,0.16,0.11,7),Vector3(side*0.54,0.71,0),wool)
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
    head.radial_segments = 7
    head.rings = 4
    make_part(body_mesh,head,Vector3(0,1.74,0),skin)
    var hair := SphereMesh.new()
    hair.radius = 0.275
    hair.height = 0.29
    hair.radial_segments = 7
    hair.rings = 3
    make_part(body_mesh,hair,Vector3(0,1.94,0.035),Color(0.052,0.05,0.052))
    # Subtle face profile reads as human even in near-monochrome lighting.
    make_part(body_mesh,box_mesh(Vector3(0.095,0.15,0.11)),Vector3(0,1.73,-0.252),skin)
    for side in [-1.0,1.0]:
        make_part(body_mesh,box_mesh(Vector3(0.065,0.11,0.065)),Vector3(side*0.265,1.74,0),skin)
        make_part(body_mesh,box_mesh(Vector3(0.072,0.028,0.016)),Vector3(side*0.115,1.79,-0.256),Color(0.075,0.068,0.066))
        make_part(body_mesh,box_mesh(Vector3(0.15,0.035,0.04)),Vector3(side*0.17,1.29,-0.34),wool)
    make_part(body_mesh,box_mesh(Vector3(0.10,0.31,0.055)),Vector3(0,1.20,-0.345),Color(0.23,0.23,0.24))
    make_part(body_mesh,box_mesh(Vector3(0.16,0.055,0.075)),Vector3(0,1.95,0.20),Color(0.055,0.05,0.048))

    pivot = Node3D.new()
    pivot.position = Vector3(0, 1.62, 0)
    pivot.name = "ViewPivot"
    add_child(pivot)
    var spring := SpringArm3D.new()
    spring.spring_length = 1.95
    spring.margin = 0.10
    var probe := SphereShape3D.new()
    probe.radius = 0.20
    spring.shape = probe
    spring.collision_mask = 1
    spring.add_excluded_object(get_rid())
    pivot.add_child(spring)
    # SpringArm3D changes the position of its direct child every physics frame.
    # Offset the GRANDCHILD camera, not the spring's direct child, otherwise
    # the intended over-the-shoulder composition is silently reset to center.
    var shoulder := Node3D.new()
    shoulder.name = "ShoulderCameraOffset"
    spring.add_child(shoulder)
    camera = Camera3D.new()
    camera.position = Vector3(0.83, 0.14, 0)
    camera.fov = 64
    camera.near = 0.08
    camera.current = true
    shoulder.add_child(camera)
    # Soft character bounce light: keep fabric legible without lighting the
    # whole room like a gameplay flashlight.
    var character_fill := OmniLight3D.new()
    character_fill.name = "CharacterBounceFill"
    character_fill.position = Vector3(0.1, 1.85, 0.87)
    character_fill.omni_range = 2.25
    character_fill.light_energy = 0.36
    character_fill.light_color = Color(0.67, 0.72, 0.72)
    character_fill.shadow_enabled = false
    add_child(character_fill)
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
    # A restrained gait avoids the rigid sliding-mannequin effect.
    if direction.length_squared() > 0.02 and is_on_floor():
        walk_cycle += delta * 8.0
        body_mesh.position.y = sin(walk_cycle * 2.0) * 0.018
        if is_instance_valid(left_sleeve):
            left_sleeve.rotation.x = sin(walk_cycle) * 0.10
        if is_instance_valid(right_sleeve):
            right_sleeve.rotation.x = -sin(walk_cycle) * 0.10
    else:
        body_mesh.position.y = move_toward(body_mesh.position.y, 0.0, delta * 0.13)
        if is_instance_valid(left_sleeve):
            left_sleeve.rotation.x = move_toward(left_sleeve.rotation.x, 0.0, delta * 1.4)
        if is_instance_valid(right_sleeve):
            right_sleeve.rotation.x = move_toward(right_sleeve.rotation.x, 0.0, delta * 1.4)

func facing_direction() -> Vector3:
    return Vector3(0, 0, -1).rotated(Vector3.UP, yaw)
