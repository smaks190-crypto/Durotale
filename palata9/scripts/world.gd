extends Node2D
class_name WardWorld
## Оригинальная пиксельная палата: геометрия и декорации без чужих ассетов.

var door_open: bool = false
var lamp_time: float = 0.0

func _ready() -> void:
    _wall(Rect2(0, 0, 640, 154))
    _wall(Rect2(0, 445, 640, 35))
    _wall(Rect2(0, 0, 94, 480))
    _wall(Rect2(546, 0, 94, 480))
    _wall(Rect2(96, 336, 112, 97))

func _process(delta: float) -> void:
    lamp_time += delta
    queue_redraw()

func _wall(rect: Rect2) -> void:
    var body := StaticBody2D.new()
    body.position = rect.position + rect.size * 0.5
    var collider := CollisionShape2D.new()
    var shape := RectangleShape2D.new()
    shape.size = rect.size
    collider.shape = shape
    body.add_child(collider)
    add_child(body)

func _draw() -> void:
    draw_rect(Rect2(0, 0, 640, 480), Color("05070a"))
    draw_rect(Rect2(48, 64, 544, 400), Color("26342f"))
    draw_rect(Rect2(80, 154, 480, 291), Color("1c2327"))
    for x in range(80, 560, 32):
        for y in range(154, 446, 32):
            if (int(x / 32) + int(y / 32)) % 2 == 0:
                draw_rect(Rect2(x, y, 32, 32), Color("20292e"))
    draw_rect(Rect2(48, 64, 544, 88), Color("34423c"))
    for y in range(84, 150, 29):
        draw_line(Vector2(48, y), Vector2(592, y), Color("26322c"), 2.0)
    for x in range(48, 592, 34):
        draw_line(Vector2(x, 64), Vector2(x, 152), Color("26322c"), 2.0)
    draw_rect(Rect2(48, 148, 544, 7), Color("11181c"))
    draw_rect(Rect2(48, 64, 32, 400), Color("2c3833"))
    draw_rect(Rect2(560, 64, 32, 400), Color("2c3833"))
    draw_rect(Rect2(48, 445, 544, 16), Color("11161b"))
    # Стена: плакат с распорядком дня.
    draw_rect(Rect2(118, 82, 66, 62), Color("d8d1bb"))
    draw_rect(Rect2(122, 88, 58, 8), Color("b3273c"))
    for i in range(5):
        draw_rect(Rect2(124, 103 + i * 7, 45 - i % 2 * 9, 3), Color("6b6656"))
    # Решётка окна и свет луны.
    draw_rect(Rect2(435, 72, 102, 72), Color("39434e"))
    draw_rect(Rect2(441, 77, 90, 61), Color("111a29"))
    draw_circle(Vector2(508, 94), 8, Color("dfe9ee"))
    for i in range(3):
        draw_rect(Rect2(456 + i * 25, 77, 4, 61), Color("39434e"))
    # Царапины на стене.
    for i in range(5):
        draw_line(Vector2(56 + i * 6, 232), Vector2(60 + i * 6, 252), Color("b0bcb5"), 2.0)
    for i in range(3):
        draw_line(Vector2(57 + i * 7, 261), Vector2(62 + i * 7, 274), Color("c9d3cc"), 1.5)
    # Кровать.
    draw_rect(Rect2(96, 336, 112, 96), Color("39424c"))
    draw_rect(Rect2(101, 341, 102, 86), Color("96a29d"))
    draw_rect(Rect2(105, 345, 94, 19), Color("ccd6d0"))
    draw_rect(Rect2(101, 372, 102, 55), Color("3f7d76"))
    draw_rect(Rect2(101, 396, 102, 3), Color("2c554e"))
    # Сложенная записка.
    draw_rect(Rect2(214, 398, 23, 17), Color("d8d1bb"))
    draw_rect(Rect2(218, 403, 15, 2), Color("6b6656"))
    draw_rect(Rect2(218, 408, 10, 2), Color("6b6656"))
    draw_rect(Rect2(231, 410, 3, 3), Color("b3273c"))
    # Дверь №9.
    draw_rect(Rect2(270, 64, 100, 90), Color("2e3742"))
    draw_rect(Rect2(276, 69, 88, 83), Color("05070a" if door_open else "55606d"))
    if not door_open:
        draw_rect(Rect2(276, 120, 88, 32), Color("48525e"))
        draw_rect(Rect2(302, 84, 37, 10), Color("12181d"))
        draw_rect(Rect2(304, 104, 32, 23), Color("b3273c"))
        draw_rect(Rect2(350, 128, 10, 4), Color("9fb4c8"))
    else:
        draw_rect(Rect2(365, 68, 24, 84), Color("4b5663"))
    # Пульсация под неисправной лампой.
    draw_rect(Rect2(306, 58, 28, 8), Color("39424c"))
    draw_rect(Rect2(310, 64, 20, 4), Color("e8be78", 0.64 + 0.16 * sin(lamp_time * 2.0)))
