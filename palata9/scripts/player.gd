extends CharacterBody2D
class_name WardPlayer
## Герой: физическое перемещение внутри палаты; стороны и простая анимация шага.

const WALK_SPEED: float = 132.0
var can_move: bool = false
var walk_time: float = 0.0
var face: Vector2 = Vector2.DOWN

func _ready() -> void:
    var shape := RectangleShape2D.new()
    shape.size = Vector2(17, 12)
    var hitbox := CollisionShape2D.new()
    hitbox.shape = shape
    hitbox.position.y = 1.0
    add_child(hitbox)

func _physics_process(delta: float) -> void:
    var dir := Vector2.ZERO
    if can_move:
        dir = Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
    velocity = dir * WALK_SPEED
    if dir.length_squared() > 0.01:
        face = dir
        walk_time += delta * 10.0
    move_and_slide()
    queue_redraw()

func _draw() -> void:
    var stride := 2.0 if velocity.length_squared() > 0.0 and sin(walk_time) > 0.0 else 0.0
    draw_rect(Rect2(-11, 14, 22, 3), Color(0.02, 0.04, 0.06, 0.5))
    draw_rect(Rect2(-9, 1, 7, 15 + stride), Color("343a46"))
    draw_rect(Rect2(2, 1, 7, 15 - stride), Color("343a46"))
    draw_rect(Rect2(-12, -16, 24, 20), Color("8195ab"))
    draw_rect(Rect2(-11, -16, 22, 3), Color("c0c7d0"))
    draw_rect(Rect2(-10, -35, 20, 21), Color("ecc9a3"))
    draw_rect(Rect2(-12, -38, 24, 9), Color("2b2433"))
    draw_rect(Rect2(-12, -31, 4, 9), Color("2b2433"))
    if face.y >= -0.3:
        draw_rect(Rect2(-6, -25, 3, 3), Color("1c1524"))
        draw_rect(Rect2(4, -25, 3, 3), Color("1c1524"))
    elif face.x != 0:
        draw_rect(Rect2(5 if face.x > 0 else -7, -25, 3, 3), Color("1c1524"))
