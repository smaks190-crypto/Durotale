extends Node2D
class_name MobileControls
## Мультитач-крестовина и контекстная кнопка действия.

var direction_buttons: Array[TouchScreenButton] = []
var action_button: TouchScreenButton
var restart_button: TouchScreenButton

func _ready() -> void:
    for action in ["palata_action", "palata_cancel", "mobile_action", "mobile_restart"]:
        if not InputMap.has_action(action):
            InputMap.add_action(action)
    direction_buttons.append(_button("left", Vector2(42, 411), "ui_left"))
    direction_buttons.append(_button("right", Vector2(146, 411), "ui_right"))
    direction_buttons.append(_button("up", Vector2(94, 359), "ui_up"))
    direction_buttons.append(_button("down", Vector2(94, 462), "ui_down"))
    action_button = _button("action", Vector2(580, 413), "mobile_action")
    restart_button = _button("restart", Vector2(580, 413), "mobile_restart")
    update_phase("title")

func _button(asset: String, center: Vector2, action: String) -> TouchScreenButton:
    var button := TouchScreenButton.new()
    button.name = asset.capitalize() + "Button"
    button.action = action
    button.texture_normal = load("res://assets/touch/%s.svg" % asset) as Texture2D
    button.position = center - Vector2(34.0, 34.0)
    button.visibility_mode = TouchScreenButton.VISIBILITY_TOUCHSCREEN_ONLY
    add_child(button)
    return button

func update_phase(phase: String) -> void:
    var use_dpad: bool = phase in ["room", "door_menu", "battle_menu", "battle_sub", "battle_defend", "gallery", "title"]
    for b in direction_buttons:
        b.visible = use_dpad
    action_button.visible = phase not in ["ending", "death"]
    restart_button.visible = phase in ["ending", "death"]
