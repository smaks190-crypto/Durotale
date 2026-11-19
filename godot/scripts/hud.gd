extends CanvasLayer
class_name DurkaHUD

signal sensitivity_changed(value: float)
signal accessibility_changed(feature: String, enabled: bool)
signal resume_requested
signal pause_requested
signal restart_requested

var root: Control
var hint: Label
var subtitle: Label
var inventory: Label
var touch_controls: Control
var options_panel: PanelContainer
var ending_panel: PanelContainer
var effect_rect: ColorRect
var showing_pause := false
var mobile := false

func text_label(s: String, size: int, color: Color) -> Label:
    var l := Label.new()
    l.text = s
    l.add_theme_color_override("font_color", color)
    l.add_theme_font_size_override("font_size", size)
    l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    l.mouse_filter = Control.MOUSE_FILTER_IGNORE
    return l

func _ready() -> void:
    root = Control.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.mouse_filter = Control.MOUSE_FILTER_IGNORE
    add_child(root)
    effect_rect = ColorRect.new()
    effect_rect.color = Color(1,1,1,1)
    effect_rect.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    effect_rect.mouse_filter = Control.MOUSE_FILTER_IGNORE
    var shader := Shader.new()
    shader.code = """shader_type canvas_item;
render_mode unshaded;
void fragment() {
    float line = step(0.50, fract(SCREEN_UV.y * 320.0));
    float grain = fract(sin(dot(floor(SCREEN_UV*vec2(340.0,220.0)+TIME*4.0),vec2(12.9898,78.233)))*43758.5453);
    float noise = 0.022 + line*0.025 + grain*0.035;
    COLOR = vec4(vec3(0.05,0.025,0.025),noise);
}"""
    var mat := ShaderMaterial.new()
    mat.shader = shader
    effect_rect.material = mat
    root.add_child(effect_rect)

    var header := text_label("ПАЛАТА №0", 18, Color(0.77,0.77,0.73))
    header.set_anchors_preset(Control.PRESET_TOP_LEFT)
    header.position = Vector2(26,18)
    header.size = Vector2(280,34)
    header.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
    root.add_child(header)

    inventory = text_label("",17,Color(0.85,0.83,0.81))
    inventory.set_anchors_preset(Control.PRESET_TOP_RIGHT)
    inventory.anchor_left = 0.58
    inventory.anchor_right = 0.985
    inventory.offset_left = 0
    inventory.offset_right = 0
    inventory.offset_top = 20
    inventory.offset_bottom = 52
    inventory.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    root.add_child(inventory)

    hint = text_label("",19,Color(0.85,0.84,0.80))
    hint.anchor_left = 0.13
    hint.anchor_right = 0.87
    hint.anchor_top = 0.70
    hint.anchor_bottom = 0.76
    root.add_child(hint)

    subtitle = text_label("",17,Color(0.91,0.90,0.84))
    subtitle.anchor_left = 0.14
    subtitle.anchor_right = 0.86
    subtitle.anchor_top = 0.84
    subtitle.anchor_bottom = 0.965
    root.add_child(subtitle)

    make_touch_controls()
    make_options()
    make_ending()

func set_hint(value: String) -> void:
    hint.text = value

func set_subtitle(value: String) -> void:
    subtitle.text = value

func set_inventory(has_handle: bool, installed: bool) -> void:
    if installed:
        inventory.text = "Ручка установлена"
    elif has_handle:
        inventory.text = "В кармане: дверная ручка"
    else:
        inventory.text = ""

func set_vhs(enabled: bool) -> void:
    effect_rect.visible = enabled

func make_button(txt: String, bounds: Rect2, action: String) -> Button:
    var b := Button.new()
    b.text = txt
    b.position = bounds.position
    b.size = bounds.size
    b.modulate = Color(1,1,1,0.73)
    b.focus_mode = Control.FOCUS_NONE
    b.button_down.connect(func() -> void: Input.action_press(action))
    b.button_up.connect(func() -> void: Input.action_release(action))
    touch_controls.add_child(b)
    return b

func make_touch_controls() -> void:
    mobile = OS.has_feature("mobile")
    touch_controls = Control.new()
    touch_controls.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    touch_controls.mouse_filter = Control.MOUSE_FILTER_IGNORE
    touch_controls.visible = mobile
    root.add_child(touch_controls)
    # Scaled to the 1280x720 reference viewport by canvas_items stretching.
    make_button("▲",Rect2(117,505,74,70),"move_forward")
    make_button("▼",Rect2(117,643,74,70),"move_back")
    make_button("◀",Rect2(40,575,74,70),"move_left")
    make_button("▶",Rect2(192,575,74,70),"move_right")
    make_button("E",Rect2(1130,585,82,82),"interact")
    var pause_button := Button.new()
    pause_button.text = "Ⅱ"
    pause_button.position = Vector2(1205,20)
    pause_button.size = Vector2(54,48)
    pause_button.focus_mode = Control.FOCUS_NONE
    pause_button.pressed.connect(func() -> void: pause_requested.emit())
    touch_controls.add_child(pause_button)

func make_options() -> void:
    options_panel = PanelContainer.new()
    options_panel.anchor_left = 0.28
    options_panel.anchor_right = 0.72
    options_panel.anchor_top = 0.15
    options_panel.anchor_bottom = 0.86
    options_panel.mouse_filter = Control.MOUSE_FILTER_STOP
    root.add_child(options_panel)
    var outer := VBoxContainer.new()
    outer.add_theme_constant_override("separation",14)
    options_panel.add_child(outer)
    outer.add_child(text_label("ПАУЗА  /  НАСТРОЙКИ",26,Color(0.93,0.92,0.90)))
    outer.add_child(text_label("Чувствительность обзора",17,Color(0.87,0.86,0.84)))
    var sens := HSlider.new()
    sens.min_value = 0.5
    sens.max_value = 2.0
    sens.step = 0.1
    sens.value = 1.0
    sens.custom_minimum_size.y = 40
    sens.value_changed.connect(func(value: float) -> void: sensitivity_changed.emit(value))
    outer.add_child(sens)
    for pair in [["Тряска камеры","shake"],["Мигание света","flashes"],["VHS / плёночный шум","vhs"]]:
        var ch := CheckBox.new()
        ch.text = str(pair[0])
        ch.button_pressed = true
        ch.add_theme_font_size_override("font_size",18)
        var key := str(pair[1])
        ch.toggled.connect(func(value: bool) -> void: accessibility_changed.emit(key,value))
        outer.add_child(ch)
    var resume := Button.new()
    resume.text = "Вернуться"
    resume.custom_minimum_size.y = 46
    resume.pressed.connect(func() -> void: resume_requested.emit())
    outer.add_child(resume)
    options_panel.visible = false

func show_pause(value: bool) -> void:
    showing_pause = value
    options_panel.visible = value
    hint.visible = not value
    touch_controls.visible = mobile and not value

func make_ending() -> void:
    ending_panel = PanelContainer.new()
    ending_panel.anchor_left = 0.24
    ending_panel.anchor_right = 0.76
    ending_panel.anchor_top = 0.24
    ending_panel.anchor_bottom = 0.73
    root.add_child(ending_panel)
    var content := VBoxContainer.new()
    content.add_theme_constant_override("separation",18)
    ending_panel.add_child(content)
    var end_title := text_label("ПАЛАТА №0",38,Color(0.93,0.92,0.91))
    end_title.name = "Title"
    content.add_child(end_title)
    var end_text := text_label("",20,Color(0.88,0.86,0.84))
    end_text.name = "Text"
    content.add_child(end_text)
    var again := Button.new()
    again.text = "Пройти снова"
    again.custom_minimum_size.y = 48
    again.pressed.connect(func() -> void: restart_requested.emit())
    content.add_child(again)
    ending_panel.visible = false

func show_ending(choice: String) -> void:
    ending_panel.visible = true
    touch_controls.visible = false
    hint.visible = false
    subtitle.visible = false
    inventory.visible = false
    var label: Label = ending_panel.get_node("VBoxContainer/Text")
    if choice == "hold":
        label.text = "Вы удержали дверь.\nС другой стороны её вес сняла чужая рука.\nОна была там всё это время?"
    else:
        label.text = "Вы ушли через пост.\nЗа спиной щёлкнула знакомая защёлка.\nКто остался по ту сторону?"
