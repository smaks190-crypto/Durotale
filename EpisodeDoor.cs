using Godot;

// The hinge carries both the slab and its collision. Removing the lever
// changes only the lever, so its spindle survives every change of ward.
public partial class EpisodeDoor : Node3D
{
    public float Target { get; set; }
    public bool Pulled { get; set; }
    public Node3D Lever { get; private set; }
    public Node3D OutsideHand { get; private set; }
    private StaticBody3D _slab;
    private float _opening;
    private float _baseYaw;
    private float _swing;

    public void Build(Vector3 hinge, float baseYaw, float width, float swing, bool hasLever)
    {
        Position = hinge;
        _baseYaw = baseYaw;
        _swing = swing;
        Rotation = new Vector3(0, baseYaw, 0);
        _slab = new StaticBody3D();
        AddChild(_slab);
        var size = new Vector3(width, 2.35f, .075f);
        var center = new Vector3(width/2, size.Y/2, 0);
        Box(_slab, center, size, "42433f");
        _slab.AddChild(new CollisionShape3D { Shape = new BoxShape3D { Size = size }, Position = center });
        foreach (float side in new[]{-1f, 1f})
        {
            Box(_slab, new Vector3(width*.5f, .25f, side*.042f),new Vector3(width*.92f,.36f,.012f),"666862");
            Box(_slab, new Vector3(width*.5f,1.79f,side*.045f),new Vector3(width*.52f,.50f,.018f),"232722");
            Box(_slab, new Vector3(width*.5f,1.79f,side*.056f),new Vector3(width*.46f,.44f,.012f),"777a70");
            for(int i=1;i<5;i++)
                Box(_slab,new Vector3(width*(.27f+i*.09f),1.79f,side*.065f),new Vector3(.012f,.43f,.012f),"55574f");
            Box(_slab,new Vector3(width-.17f,1.04f,side*.05f),new Vector3(.08f,.22f,.015f),"74766d");
            Box(_slab,new Vector3(width-.17f,1.08f,side*.085f),new Vector3(.037f,.037f,.10f),"a2a294");
        }
        Lever = new Node3D { Position = new Vector3(width-.17f,1.08f,0) };
        _slab.AddChild(Lever);
        foreach(float side in new[]{-1f,1f})
            Box(Lever,new Vector3(-.075f,0,side*.146f),new Vector3(.19f,.055f,.046f),"999a91");
        Lever.Visible = hasLever;
        OutsideHand = new Node3D { Position = new Vector3(width-.16f,1.12f,.10f) };
        _slab.AddChild(OutsideHand);
        var sleeve = Box(OutsideHand,new Vector3(.14f,.16f,.17f),new Vector3(.15f,.18f,.38f),"32332f");
        sleeve.Rotation = new Vector3(-.7f,.45f,-.35f);
        var palm = Box(OutsideHand,new Vector3(.035f,.05f,.035f),new Vector3(.12f,.145f,.075f),"aca89b");
        palm.Rotation = new Vector3(.26f,.3f,-.22f);
        for(int i=0;i<4;i++)
        {
            var finger = Box(OutsideHand,new Vector3(-.004f+i*.031f,-.031f,.012f),new Vector3(.024f,.075f,.036f),"aaa698");
            finger.Rotation = new Vector3(-.5f,.05f,-.04f);
        }
        OutsideHand.Visible = false;
        Snap(0);
    }

    public static MeshInstance3D Box(Node3D parent, Vector3 position, Vector3 size, string color)
    {
        var mesh = new MeshInstance3D {
            Mesh = new BoxMesh { Size = size }, Position = position,
            MaterialOverride = new StandardMaterial3D { AlbedoColor = new Color(color), Roughness = .87f }
        };
        parent.AddChild(mesh);
        return mesh;
    }

    public void Snap(float extent)
    {
        Target = extent;
        _opening = extent;
        Apply();
    }

    private void Apply()
    {
        Rotation = new Vector3(0, _baseYaw + _opening*_swing, 0);
    }

    public void Tick(float dt)
    {
        _opening = Mathf.MoveToward(_opening, Target, dt*(Pulled ? 3.9f : .92f));
        Apply();
    }
}
