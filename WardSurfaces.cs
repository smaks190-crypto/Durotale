using Godot;
using System;
using System.Collections.Generic;

// Small, tileable swatches are shared by every piece of furniture and architecture.
internal sealed class WardSurfaces
{
    internal enum Finish { Lime, Enamel, Floor, Ceiling, Wood, Steel, Linen, Rubber, Ceramic, Window, Paper, Lamp, Ink, Ember }
    private readonly Dictionary<(Finish, int), Material> cache = new();

    private static float Grain(int x, int y, int seed)
    {
        unchecked
        {
            uint h = (uint)(x * 374761393 + y * 668265263 + seed * 97531);
            h = (h ^ (h >> 13)) * 1274126177;
            return (h ^ (h >> 16)) / (float)uint.MaxValue;
        }
    }

    private static float Noise(int x, int y, int scale, int salt)
    {
        int period = 128 / scale;
        int u = x / scale, v = y / scale;
        float fx = (float)(x % scale) / scale, fy = (float)(y % scale) / scale;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float a = Mathf.Lerp(Grain(u, v, salt), Grain((u + 1) % period, v, salt), fx);
        float b = Mathf.Lerp(Grain(u, (v + 1) % period, salt), Grain((u + 1) % period, (v + 1) % period, salt), fx);
        return Mathf.Lerp(a, b, fy);
    }

    public Material Get(Finish f, int state)
    {
        if (cache.TryGetValue((f, state), out var found)) return found;
        if (f == Finish.Lime || f == Finish.Floor)
        {
            var materialName = f == Finish.Lime ? "decay_wall" : "aged_tiles";
            var finish = new ShaderMaterial { Shader = GD.Load<Shader>($"res://shaders/{materialName}.gdshader") };
            finish.SetShaderParameter("damage", state == 0 ? .04f : state == 1 ? .86f : .98f);
            if (f == Finish.Lime)
            {
                finish.SetShaderParameter("plaster_color", state == 0 ? new Color(.60f,.59f,.56f) : new Color(.42f,.43f,.42f));
                finish.SetShaderParameter("lower_color", state == 0 ? new Color(.28f,.30f,.29f) : new Color(.205f,.22f,.215f));
                finish.SetShaderParameter("chip_color",new Color(.18f,.17f,.16f));
            }
            else finish.SetShaderParameter("wetness",state == 0 ? .24f : .81f);
            cache[(f,state)] = finish;
            return finish;
        }
        if (f == Finish.Window)
        {
            var window = new ShaderMaterial { Shader = GD.Load<Shader>("res://shaders/frosted_window.gdshader") };
            window.SetShaderParameter("light_strength", state == 0 ? 1.65f : .91f);
            cache[(f,state)] = window;
            return window;
        }
        var img = Image.CreateEmpty(128, 128, false, Image.Format.Rgb8);
        float decay = state == 0 ? 0 : state == 1 ? 1 : .82f;
        for (int y = 0; y < 128; y++)
        for (int x = 0; x < 128; x++)
        {
            float grain = Grain(x, y, 11);
            float broad = Noise(x, y, 32, 9);
            float n = broad * .5f + Noise(x, y, 8, 5) * .32f + Noise(x, y, 4, 4) * .18f;
            float pits = Mathf.SmoothStep(.45f, .70f, n + grain * .09f);
            float v = 0, warmth = 0;
            switch (f)
            {
                case Finish.Enamel:
                    v = .36f + grain * .035f + n * .05f - decay * .12f + decay * (pits > .66f ? (.33f - grain * .18f) : 0);
                    warmth = -.009f; break;
                case Finish.Ceiling:
                    v = .49f + n * .055f - decay * (.12f + pits * .12f);
                    if (x < 2 || y < 2) v *= .77f;
                    break;
                case Finish.Wood:
                    v = .23f + Noise(x / 2, y, 4, 45) * .095f + Grain(0, y, 9) * .06f + n * .09f;
                    v += decay * (pits - .45f) * .25f; warmth = .037f; break;
                case Finish.Steel:
                    v = .31f + n * .16f + grain * .055f;
                    if (decay > 0 && pits > .22f) {v *= .55f; warmth = .036f;}
                    break;
                case Finish.Linen:
                    v = .80f + n * .065f + MathF.Sin(y * .15f + n * 7) * .03f - decay * (.23f + pits * .35f);
                    if ((y % 4 == 0) != (x % 4 == 0)) v -= .018f;
                    warmth = .016f; break;
                case Finish.Rubber: v = .12f + grain * .04f + n * .05f; break;
                case Finish.Ceramic: v = .77f + n * .045f - decay * pits * .3f; break;
                case Finish.Paper: v = .74f + n * .09f - decay * (.15f + pits * .23f); warmth = .035f; break;
                case Finish.Lamp: v = .91f + n * .045f; break;
                case Finish.Ink: v = .062f + grain * .025f; break;
                case Finish.Ember: img.SetPixel(x, y, new Color(.39f + grain * .08f, .10f, .085f)); continue;
            }
            img.SetPixel(x, y, new Color(Mathf.Clamp(v + warmth,0,1), Mathf.Clamp(v,0,1), Mathf.Clamp(v - warmth,0,1)));
        }
        img.GenerateMipmaps();
        float scale = f == Finish.Ceiling ? .75f : f == Finish.Linen ? 2f : .65f;
        var material = new StandardMaterial3D {
            AlbedoTexture = ImageTexture.CreateFromImage(img), Uv1Triplanar = true, Uv1WorldTriplanar = true,
            Uv1Scale = Vector3.One * scale,
            TextureFilter = BaseMaterial3D.TextureFilterEnum.NearestWithMipmaps,
            Roughness = .91f
        };
        if (f == Finish.Lamp)
        {
            material.EmissionEnabled = true;
            material.Emission = new Color(.69f,.70f,.67f);
            material.EmissionEnergyMultiplier = 1.3f;
        }
        cache[(f,state)] = material;
        return material;
    }
}
