using Godot;

/// <summary>Optional paper trail. Only the ink changes. The leaf stays turned and
/// the cuts on the brass plate remain in the same physical location.</summary>
public partial class CaseNotes : Node3D
{
    private enum Site { None, Board, Chart, Plate }
    private static readonly Vector3 Board = new(-1.89f, 1.91f, 9.02f);
    private static readonly Vector3 Chart = new(2.20f, 1.54f, 9.70f);
    private static readonly Vector3 Plate = new(1.96f, 1.46f, .94f);
    private Node3D _leaf;
    private Label3D _room, _shift;
    private bool _turned;

    public void Build()
    {
        if (_leaf != null) return;
        var chart = Mount(Chart, Mathf.Pi/2);
        Box(chart, Vector3.Zero, new(.30f,.39f,.022f), "453f37");
        Box(chart, new(0,0,.016f), new(.266f,.347f,.007f), "a69b85");
        Print(chart,"КОПИЯ",new(0,.099f,.021f),.0014f);
        Scratches(chart, new(0,.041f,.024f), "5a554d");
        Print(chart,"между койками",new(0,-.078f,.022f),.0007f);
        _leaf = new Node3D {Position = new(0,.17f,.026f)};
        chart.AddChild(_leaf);
        Box(_leaf,new(0,-.165f,.003f),new(.257f,.330f,.006f),"cac5b4");
        Print(_leaf,"ЛИСТ ОБХОДА",new(0,-.071f,.009f),.00097f);
        _room = Print(_leaf,"",new(0,-.141f,.009f),.0015f);
        for (int i=0;i<5;i++)
            Box(_leaf,new(0,-.198f-i*.020f,.008f),new(.192f,.002f,.003f),"878578");
        Box(chart,new(0,.184f,.036f),new(.093f,.032f,.016f),"797974");

        var plate = Mount(Plate,-Mathf.Pi/2);
        Box(plate,Vector3.Zero,new(.23f,.13f,.025f),"625e52");
        Scratches(plate,new(0,-.006f,.015f),"c4bba7");
        foreach(float s in new[]{-1f,1f})
            Box(plate,new(.097f*s,0,.015f),new(.012f,.012f,.007f),"3c3c37");
        var shift=Mount(Board,Mathf.Pi/2);
        Box(shift,new(0,0,.019f),new(.22f,.245f,.004f),"b9b4a3");
        _shift = Print(shift,"",new(0,.050f,.024f),.00088f);
        Print(shift,"ЗАЯВКА",new(0,-.018f,.024f),.0010f);
        Scratches(shift,new(0,-.091f,.026f),"625d54",.66f);
        State(0);
    }

    public void State(int world)
    {
        if (_room == null) return;
        _room.Text=world switch { 0=>"№ 6", 1=>"№ 8", _=>"№ 0" };
        _shift.Text=world switch { 0=>"ПАЛАТА 7", 1=>"СКЛАД 4", _=>"ПОСТ" };
    }

    public string Prompt(Patient patient,int world) => Find(patient) switch {
        Site.Board => "E   Прочесть заявку",
        Site.Chart => _turned ? "E   Прочесть копию" : "E   Отогнуть лист",
        Site.Plate => "E   Провести по зарубкам",
        _ => ""
    };

    public string Inspect(Patient patient,int world)
    {
        switch(Find(patient))
        {
            case Site.Board:
                return world switch {
                    0 => "Заявка слесарю. «В седьмой слышно, как стучат из шестой». Вместо подписи — две короткие черты и длинная.",
                    1 => "Поверх слова «палата» вписали «склад № 4». Под новым номером проступают две короткие черты и длинная.",
                    _ => "На бланке отпечатан адрес самого поста. Внизу три вдавленные черты. Под последней бумага прорвалась."
                };
            case Site.Chart:
                if(!_turned)
                {
                    _turned=true;
                    CreateTween().SetTrans(Tween.TransitionType.Sine).TweenProperty(_leaf,"rotation:x",-2.64f,.48f);
                    return "Под листом застряла копирка. На обороте рукой слесаря выведено «между койками». Ниже — две короткие черты и длинная.";
                }
                return world switch {
                    0 => "На обложке шестая палата. Чернила на копии ещё пачкают палец. Слесарь обвёл перегородку, к которой придвинуты обе койки.",
                    1 => "На обложке восьмая. Копирка высохла и крошится, но три бороздки продавлены насквозь. Последняя заметно длиннее.",
                    _ => "На обложке ноль. Если провести пальцем по обороту, чувствуется тот же прочерк после двух коротких."
                };
            case Site.Plate:
                return world switch {
                    0 => "На стыке двух палат под краской нащупываются три зарубки. Две короткие, одна длинная. За пластиной чуть дрожит стена.",
                    1 => "Краска облетела вместе с номером. На металле виден след напильника. Третья бороздка по-прежнему доходит до винта.",
                    _ => "По одну сторону пластины коридор холодный, по другую чувствуется сквозняк. Палец точно попадает в знакомую длинную зарубку."
                };
            default: return "";
        }
    }

    private static Site Find(Patient patient)
    {
        var p=patient.GlobalPosition;
        if(p.X>2.25f && p.Z>1.15f && Faces(patient,Chart)) return Site.Chart;
        if(p.X>=1.8f) return Site.None;
        if(Faces(patient,Board)) return Site.Board;
        return Faces(patient,Plate) ? Site.Plate : Site.None;
    }

    private static bool Faces(Patient patient,Vector3 target)
    {
        var v=target-(patient.GlobalPosition+Vector3.Up*1.48f);
        return v.LengthSquared()<2.72f && v.Normalized().Dot(patient.Gaze)>.58f;
    }

    private Node3D Mount(Vector3 pos,float yaw)
    {
        var mount=new Node3D {Position=pos,Rotation=new Vector3(0,yaw,0)};
        AddChild(mount);
        return mount;
    }

    private static void Box(Node3D parent,Vector3 pos,Vector3 size,string hex)
    {
        var material=new StandardMaterial3D {AlbedoColor=new Color(hex),Roughness=.93f};
        parent.AddChild(new MeshInstance3D {
            Position=pos, Mesh=new BoxMesh {Size=size,Material=material}
        });
    }

    private static void Scratches(Node3D parent,Vector3 pos,string hex,float scale=1)
    {
        for(int i=0;i<3;i++)
        {
            float length=(i==2?.067f:.036f)*scale;
            Box(parent,pos+new Vector3((i-1)*.039f*scale,length*.5f,0),
                new(.005f*scale,length,.002f),hex);
        }
    }

    private static Label3D Print(Node3D parent,string text,Vector3 pos,float pixel)
    {
        var label=new Label3D {
            Position=pos,Text=text,FontSize=26,PixelSize=pixel,OutlineSize=0,
            Modulate=new Color("343530"),DoubleSided=false
        };
        parent.AddChild(label);
        return label;
    }
}
