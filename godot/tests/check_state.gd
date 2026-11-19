extends SceneTree
func _initialize():call_deferred("check")
func check():
    var g=load("res://chapter.tscn").instantiate()
    root.add_child(g)
    g.pocket=true
    g.pattern.assign([.41,.76])
    for i in [2,0,1]:
        g.ward.state(i)
        if not g.pocket or g.pattern.size()!=2:
            quit(1)
            return
    print("STATE_TESTS_OK")
    quit()
