using Godot;
using System;
using System.Collections.Generic;

/// <summary>Foot-origin, close third-person controller. Static geometry and doors share collision layer 1.</summary>
public partial class Patient : CharacterBody3D
{
    public bool Mobile { get; set; } = true;
    public bool Awake { get; set; }
    public float Sensitivity { get; set; } = 1f;
    public bool Shake { get; set; } = true;
    public Vector3 Gaze => new Basis(Vector3.Up, Rotation.Y) * new Basis(Vector3.Right, _pitch) * Vector3.Forward;
    public Camera3D View { get; private set; }

    private Node3D _figure, _leftArm, _rightArm, _rightElbow, _leftLeg, _rightLeg, _leftCalf, _rightCalf;
    private PhysicsShapeQueryParameters3D _cameraProbe;
    private float _pitch = -.075f, _phase, _stride, _impulse, _stand, _reach;
    private bool _holding;
    private static void Bind(string name, Key key)
    {
        if (!InputMap.HasAction(name))
        {
            InputMap.AddAction(name);
            InputMap.ActionAddEvent(name, new InputEventKey { PhysicalKeycode = key });
        }
    }

    public void Build()
    {
        if (View != null) return;
        Bind("durka_left", Key.A);
        Bind("durka_right", Key.D);
        Bind("durka_forward", Key.W);
        Bind("durka_back", Key.S);
        FloorSnapLength = .22f;
        FloorMaxAngle = Mathf.DegToRad(48f);
        CollisionMask = 1;
        AddChild(new CollisionShape3D {
            Shape = new CapsuleShape3D { Radius = .255f, Height = 1.72f },
            Position = Vector3.Up * .87f
        });
        _figure = Figure(false);
        AddChild(_figure);
        _leftArm = _figure.GetNode<Node3D>("LeftArm");
        _rightArm = _figure.GetNode<Node3D>("RightArm");
        _rightElbow = _rightArm.GetNode<Node3D>("Elbow");
        _leftLeg = _figure.GetNode<Node3D>("LeftLeg");
        _rightLeg = _figure.GetNode<Node3D>("RightLeg");
        _leftCalf = _leftLeg.GetNode<Node3D>("Calf");
        _rightCalf = _rightLeg.GetNode<Node3D>("Calf");
        View = new Camera3D { Name = "ShoulderCamera", Fov = 64, Near = .035f, Far = 75, Current = true };
        AddChild(View);
        _stand = Awake ? 1f : 0f;
        _cameraProbe = new PhysicsShapeQueryParameters3D {
            Shape = new SphereShape3D { Radius = .105f },
            CollisionMask = 1,
            Exclude = new Godot.Collections.Array<Rid> { GetRid() }
        };
        View.Position = new Vector3(.42f, 1.62f, .98f);
    }

    public void Face(float yaw)
    {
        Rotation = new Vector3(0, yaw, 0);
        _pitch = -.075f;
    }

    public void Jolt() => _impulse = 1;
    public void Hold(bool holding) => _holding = holding;

    public override void _UnhandledInput(InputEvent input)
    {
        if (!Mobile || View == null || Input.MouseMode != Input.MouseModeEnum.Captured) return;
        if (input is InputEventMouseMotion mouse)
        {
            float speed = .0021f * Mathf.Clamp(Sensitivity, .1f, 4f);
            RotateY(-mouse.Relative.X * speed);
            _pitch = Mathf.Clamp(_pitch - mouse.Relative.Y * speed, -1.12f, .92f);
        }
    }

    public override void _PhysicsProcess(double delta)
    {
        if (View == null) return;
        float dt = (float)delta;
        Vector2 move = Mobile && Awake
            ? Input.GetVector("durka_left", "durka_right", "durka_forward", "durka_back") : Vector2.Zero;
        Vector3 desired = Basis * new Vector3(move.X, 0, move.Y) * 2.25f;
        Vector3 horizontal = new Vector3(Velocity.X, 0, Velocity.Z).MoveToward(desired, 13 * dt);
        Velocity = new Vector3(horizontal.X, IsOnFloor() ? -.3f : Mathf.Max(Velocity.Y - 16 * dt, -18f), horizontal.Z);
        if (Awake) MoveAndSlide();
        else Velocity = Vector3.Zero;
        float actualSpeed = new Vector2(GetRealVelocity().X, GetRealVelocity().Z).Length();
        _stride = Mathf.MoveToward(_stride, Mathf.Clamp(actualSpeed / 2.25f, 0, 1), dt * 5);
        _phase += dt * (1.5f + _stride * 7.8f);
        _stand = Mathf.MoveToward(_stand, Awake ? 1 : 0, dt * 1.9f);
        _reach = Mathf.MoveToward(_reach, _holding ? 1 : 0, dt * 3);
        _impulse = Mathf.MoveToward(_impulse, 0, 2.2f * dt);
        Animate();
        PlaceCamera();
    }

    private void Animate()
    {
        float sitting = 1 - _stand;
        float cycle = Mathf.Sin(_phase) * _stride;
        _figure.Position = new Vector3(0, -.32f * sitting + .009f * Mathf.Abs(cycle), 0);
        _leftLeg.Rotation = new Vector3(1.28f*sitting + cycle*.42f, 0, -.025f);
        _rightLeg.Rotation = new Vector3(1.28f*sitting - cycle*.42f, 0, .025f);
        _leftCalf.Rotation = new Vector3(-1.38f*sitting - Mathf.Max(cycle,0)*.36f,0,0);
        _rightCalf.Rotation = new Vector3(-1.38f*sitting + Mathf.Min(cycle,0)*.36f,0,0);
        _leftArm.Rotation = new Vector3(.20f + .46f*sitting - cycle*.28f, -.04f, -.09f);
        _rightArm.Rotation = new Vector3(.28f + .41f*sitting + cycle*.28f + _reach*.92f, -.12f, .18f + _reach*.16f);
        _rightElbow.Rotation = new Vector3(.94f + .13f*sitting - .55f*_reach, -.18f, 0);
    }

    private void PlaceCamera()
    {
        var pitchBasis = new Basis(Vector3.Right, _pitch);
        float bob = Shake ? .012f * Mathf.Cos(_phase * 2) * _stride + _impulse*.018f*Mathf.Sin(_phase*30) : 0;
        Vector3 pivot = GlobalPosition + Vector3.Up * (1.47f - .29f*(1-_stand) + bob);
        Vector3 localArm = pitchBasis * new Vector3(.43f, .18f, 1.05f);
        Vector3 arm = GlobalBasis * localArm;
        _cameraProbe.Transform = new Transform3D(Basis.Identity, pivot);
        _cameraProbe.Motion = arm;
        var space = GetWorld3D().DirectSpaceState;
        float[] motion = space.CastMotion(_cameraProbe);
        float safeFraction = motion.Length > 0 ? motion[0] : 1;
        float distance = Mathf.Max(0, arm.Length()*safeFraction - .025f);
        View.GlobalPosition = pivot + arm.Normalized()*distance;
        View.Rotation = new Vector3(_pitch, 0, 0);
        // When pressed against a wall, the sightline can pass through the character's skull.
        _figure.Visible = distance > .43f;
    }

    public static Node3D Figure(bool nurse) => HospitalFigure.Create(nurse);
}

// Elliptical rings are lofted into flat triangular facets. Faces, garments and hands
// have separate silhouettes so details survive low resolution and oblique light.
internal static class HospitalFigure
{
    private readonly record struct Ring(float Y, float X, float Z, float OffsetZ = 0);
    private static readonly Dictionary<string, StandardMaterial3D> Materials = new();
    private static StandardMaterial3D Mat(string hex)
    {
        if (!Materials.TryGetValue(hex, out var mat))
        {
            mat = new StandardMaterial3D {
                AlbedoColor = new Color(hex), Roughness = .97f,
                VertexColorUseAsAlbedo = true, CullMode = BaseMaterial3D.CullModeEnum.Disabled
            };
            Materials.Add(hex, mat);
        }
        return mat;
    }

    private static Node3D Joint(Node3D parent, string name, float x, float y, float z)
    {
        var n = new Node3D {Name=name, Position=new Vector3(x,y,z)};
        parent.AddChild(n);
        return n;
    }

    private static void Facet(SurfaceTool st, Vector3 a, Vector3 b, Vector3 c, float shade)
    {
        Vector3 normal = (c-a).Cross(b-a).Normalized();
        var grey = new Color(shade, shade, shade);
        foreach (var v in new[]{a,b,c})
        {
            st.SetNormal(normal); st.SetColor(grey); st.AddVertex(v);
        }
    }

    private static MeshInstance3D Loft(Node3D parent, string name, Vector3 pos, string color, int count, params Ring[] rings)
    {
        var st = new SurfaceTool();
        st.Begin(Mesh.PrimitiveType.Triangles);
        st.SetMaterial(Mat(color));
        Vector3 Point(int j, Ring ring)
        {
            float angle = (j+.5f) * Mathf.Tau/count;
            return new Vector3(Mathf.Sin(angle)*ring.X, ring.Y, -Mathf.Cos(angle)*ring.Z + ring.OffsetZ);
        }
        for (int i=0; i<rings.Length-1; i++)
        {
            for (int j=0; j<count; j++)
            {
                var a=Point(j,rings[i]); var b=Point(j+1,rings[i]);
                var c=Point(j,rings[i+1]); var d=Point(j+1,rings[i+1]);
                float grey=.87f + .019f*((i*3+j*7)%7);
                Facet(st,a,b,c,grey); Facet(st,b,d,c,grey-.025f);
            }
        }
        for(int j=1; j<count-1; j++)
        {
            Facet(st,Point(0,rings[0]),Point(j+1,rings[0]),Point(j,rings[0]),.9f);
            Facet(st,Point(0,rings[^1]),Point(j,rings[^1]),Point(j+1,rings[^1]),1);
        }
        var mesh = new MeshInstance3D {Name=name, Mesh=st.Commit(), Position=pos};
        parent.AddChild(mesh);
        return mesh;
    }

    // A flat sewn panel with its own silhouette and face shading, e.g. a lapel or collar.
    private static void Panel(Node3D parent, string color, params Vector3[] points)
    {
        var st=new SurfaceTool(); st.Begin(Mesh.PrimitiveType.Triangles); st.SetMaterial(Mat(color));
        for(int i=1;i<points.Length-1;i++) Facet(st, points[0], points[i], points[i+1], .98f);
        parent.AddChild(new MeshInstance3D {Mesh=st.Commit()});
    }

    public static Node3D Create(bool nurse)
    {
        var f=new Node3D {Name=nurse?"Sister":"PatientFigure"};
        string cloth=nurse?"aaa9a0":"504d47";
        string cuff=nurse?"c4c0b4":"393733";
        string skin=nurse?"b3aaa0":"aa9c8e";
        string hair=nurse?"37352f":"302f2d";
        Loft(f,"Body",Vector3.Zero,cloth,8,
            new(.78f,.205f,.132f,.015f),new(.94f,.17f,.12f),new(1.19f,.195f,.135f),
            new(1.37f,.252f,.15f),new(1.445f,.184f,.117f),new(1.48f,.083f,.08f));
        if(nurse) Loft(f,"Apron",Vector3.Zero,"b7b6ae",8,new(.57f,.225f,.152f),new(.82f,.194f,.127f),new(1.01f,.171f,.127f));
        // Jacket with a horizontal back yoke, two hanging pleats and a centre seam.
        if(!nurse)
        {
            Panel(f,"64605a",new(-.183f,1.39f,.090f),new(0,1.42f,.133f),new(.183f,1.39f,.090f),new(.143f,1.305f,.115f),new(0,1.275f,.142f),new(-.143f,1.305f,.115f));
            foreach(float s in new[]{-1f,1f})
            {
                Panel(f,"3e3c38",new(.139f*s,1.32f,.117f),new(.122f*s,1.28f,.123f),new(.094f*s,.95f,.117f),new(.114f*s,1.06f,.12f));
                Panel(f,"726c62",new(.065f*s,1.467f,-.083f),new(.17f*s,1.39f,-.082f),new(.085f*s,1.32f,-.14f),new(.044f*s,1.24f,-.137f));
            }
            Panel(f,"46443f",new(-.006f,1.276f,.143f),new(.006f,1.276f,.143f),new(.004f,.797f,.142f),new(-.004f,.797f,.142f));
        }
        Loft(f,"Collar",Vector3.Zero,cuff,8,new(1.424f,.108f,.094f),new(1.478f,.096f,.087f));
        Loft(f,"Neck",Vector3.Zero,skin,7,new(1.462f,.065f,.065f),new(1.54f,.073f,.068f));
        var head=Joint(f,"Head",0,1.51f,-.012f);
        Loft(head,"Face",Vector3.Zero,skin,8,
            new(0,.053f,.072f,-.026f),new(.050f,.094f,.10f,-.014f),
            new(.121f,.105f,.109f,-.005f),new(.185f,.102f,.105f),new(.219f,.073f,.077f));
        Loft(head,"Hair",Vector3.Zero,hair,9,new(.145f,.107f,.108f,.009f),
            new(.198f,.112f,.115f),new(.241f,.080f,.089f),new(.251f,.038f,.048f));
        Loft(head,"Nape",new(0,.048f,.057f),hair,7,new(0,.067f,.036f),new(.045f,.094f,.051f),new(.119f,.106f,.053f));
        // Uneven cropped locks break up the helmet silhouette at the back of the head.
        for(int i=0;i<7;i++)
        {
            float a=(i-3)*.40f;
            float x=Mathf.Sin(a), z=Mathf.Cos(a);
            var lockRoot=Joint(head,"NapeLock",x*.097f,.105f,z*.098f+.008f);
            lockRoot.Rotation=new Vector3(.08f,a,0);
            Panel(lockRoot, i%2==0?"403d39":"353431",
                new(-.021f,.056f,.004f),new(.023f,.048f,.006f),
                new(.011f,-.031f-i%3*.011f,0),new(-.010f,-.011f,.009f));
        }
        foreach(float s in new[]{-1f,1f})
        {
            Loft(head,"Ear",new(s*.101f,.073f,0),skin,5,new(0,.019f,.016f),new(.037f,.02f,.025f),new(.058f,.01f,.015f));
            Panel(head,"4a453f",new(s*.017f,.124f,-.108f),new(s*.069f,.131f,-.09f),new(s*.066f,.12f,-.093f),new(s*.022f,.116f,-.107f));
            Panel(head,"665d54",new(s*.022f,.107f,-.106f),new(s*.064f,.111f,-.095f),new(s*.051f,.101f,-.101f));
        }
        Panel(head,"b4a797",new(0,.122f,-.108f),new(-.016f,.076f,-.12f),new(.003f,.071f,-.143f),new(.017f,.078f,-.116f));
        Panel(head,"71665b",new(-.032f,.045f,-.105f),new(.03f,.046f,-.104f),new(.013f,.041f,-.108f));
        if(nurse)
        {
            Loft(head,"Cap",new(0,.208f,.015f),"d6d3c9",6,new(0,.109f,.107f),new(.051f,.115f,.079f));
            Loft(head,"Bun",new(0,.08f,.105f),hair,7,new(0,.044f,.041f),new(.048f,.061f,.056f),new(.085f,.047f,.046f));
        }
        for(int i=0;i<2;i++)
        {
            float s=i==0?-1:1;
            var arm=Joint(f,i==0?"LeftArm":"RightArm",s*.21f,1.36f,0);
            Loft(arm,"Sleeve",Vector3.Zero,cloth,7,new(-.30f,.068f,.072f),new(-.13f,.071f,.080f),new(.02f,.083f,.088f),new(.057f,.050f,.060f));
            var elbow=Joint(arm,"Elbow",0,-.275f,0);
            elbow.Rotation = new Vector3(nurse?.79f:.59f,0,0);
            Loft(elbow,"Forearm",Vector3.Zero,cloth,7,new(-.245f,.050f,.056f),new(-.17f,.057f,.070f),new(-.044f,.071f,.071f),new(0,.060f,.068f));
            Loft(elbow,"Cuff",new(0,-.236f,0),cuff,7,new(-.025f,.053f,.06f),new(.012f,.057f,.061f));
            Loft(elbow,"Palm",new(0,-.259f,0),skin,6,new(-.093f,.035f,.032f),new(-.064f,.045f,.035f),new(-.013f,.039f,.031f),new(0,.031f,.031f));
            for(int finger=0;finger<4;finger++)
            {
                float x=(finger-1.5f)*.016f;
                Loft(elbow,"Finger",new(x,-.339f,0),skin,5,new(-.044f+.004f*Mathf.Abs(finger-1),.006f,.008f,-.01f),new(-.018f,.009f,.012f),new(0,.009f,.012f));
            }
            Loft(elbow,"Thumb",new(-s*.035f,-.294f,-.006f),skin,5,new(-.048f,.012f,.013f,-.017f),new(-.018f,.015f,.015f,-.009f),new(0,.012f,.014f));
            var leg=Joint(f,i==0?"LeftLeg":"RightLeg",s*.094f,.86f,0);
            string trousers=nurse?"88847c":"363633";
            Loft(leg,"Thigh",Vector3.Zero,trousers,7,new(-.42f,.077f,.086f),new(-.21f,.096f,.107f),new(0,.10f,.119f));
            var calf=Joint(leg,"Calf",0,-.403f,0);
            Loft(calf,"Trouser",Vector3.Zero,trousers,7,new(-.377f,.070f,.083f),new(-.307f,.078f,.075f),new(-.104f,.078f,.091f,.008f),new(0,.078f,.083f));
            Loft(calf,"Shoe",new(0,-.451f,-.041f),"282826",8,new(0,.079f,.144f,-.01f),new(.045f,.083f,.145f,-.012f),new(.09f,.064f,.093f,.018f));
        }
        return f;
    }
}
