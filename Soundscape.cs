using Godot;
using System;
using System.Collections.Generic;

/// <summary>All recordings are synthesized locally. Cues use world coordinates so the
/// same mounting bracket remains audible at the same point through every change.</summary>
public partial class Soundscape : Node3D
{
    private const int Rate = 16000;
    private const float Tau = 2 * MathF.PI;
    private readonly Dictionary<string, AudioStreamWav> recordings = new();
    private readonly AudioStreamPlayer[] rooms = new AudioStreamPlayer[3];
    private readonly List<AudioStreamPlayer3D> live = new();
    private bool built, quiet;
    private int state, stepIndex;

    public void Build()
    {
        if (built) return;
        built = true;
        ProcessMode = ProcessModeEnum.Pausable;
        foreach (var entry in new (string name, float length)[] {
            ("knock", .62f), ("step", .39f), ("step2", .42f), ("hinge", 1.13f),
            ("latch", .45f), ("wind", 2.45f), ("voices", 3.8f), ("handle", .86f) })
        {
            var noise = new Grain(283 + recordings.Count * 1069);
            recordings[entry.name] = Wave(entry.length, false, t => Transient(entry.name, t, noise));
        }
        for (int i = 0; i < rooms.Length; i++)
        {
            int room = i;
            var noise = new Grain(4821 + i);
            rooms[i] = new AudioStreamPlayer {
                Stream = Wave(6, true, t => Room(room, t, noise)),
                VolumeDb = i == 0 ? -13 : -76
            };
            AddChild(rooms[i]);
            rooms[i].Play();
        }
    }

    public void State(int value) => state = Math.Clamp(value, 0, 2);

    public void Cue(string kind, Vector3 where)
    {
        if (!built || quiet) return;
        if (kind == "step" && (++stepIndex & 1) == 0) kind = "step2";
        if (!recordings.TryGetValue(kind, out var wave)) return;
        var source = new AudioStreamPlayer3D {
            Stream = wave, UnitSize = 3.2f, MaxDistance = 35,
            VolumeDb = kind == "voices" ? -2 : kind.StartsWith("step") ? -5 : 0,
            AttenuationFilterCutoffHz = 7200, MaxDb = 0
        };
        AddChild(source);
        source.GlobalPosition = where;
        live.Add(source);
        source.Finished += () => { live.Remove(source); source.QueueFree(); };
        source.Play();
    }

    public override void _Process(double delta)
    {
        if (!built || quiet) return;
        for (int i = 0; i < rooms.Length; i++)
            rooms[i].VolumeDb = Mathf.MoveToward(rooms[i].VolumeDb, i == state ? -13 : -76, (float)delta * 36);
    }

    public void Silence()
    {
        quiet = true;
        foreach (var stream in rooms) stream?.Stop();
        foreach (var source in live) { source.Stop(); source.QueueFree(); }
        live.Clear();
    }

    private sealed class Grain
    {
        private uint seed;
        private float low, mid, last;
        public Grain(int seed) { this.seed = (uint)seed; }
        public void Next(out float air, out float grit, out float edge)
        {
            seed = unchecked(seed * 1664525u + 1013904223u);
            float n = (seed >> 8) * (2f / 16777215f) - 1;
            low += .035f * (n - low);
            mid += .35f * (n - mid);
            air = low; grit = mid - low; edge = n - last;
            last = n;
        }
    }

    private static AudioStreamWav Wave(float seconds, bool loop, Func<float,float> sample)
    {
        int count = (int)(seconds * Rate);
        byte[] bytes = new byte[count * 2];
        for (int i = 0; i < count; i++)
        {
            float t = i / (float)Rate;
            float edge = loop ? 1 : Math.Min(1, (seconds-t)*75);
            short v = (short)(Math.Clamp(sample(t) * edge, -.96f, .96f) * 32767);
            bytes[i*2] = (byte)(v & 255);
            bytes[i*2+1] = (byte)((v >> 8) & 255);
        }
        return new AudioStreamWav { Format = AudioStreamWav.FormatEnum.Format16Bits,
            MixRate = Rate, Data = bytes, LoopMode = loop ? AudioStreamWav.LoopModeEnum.Forward : AudioStreamWav.LoopModeEnum.Disabled,
            LoopEnd = count };
    }

    private static float S(float hz, float t) => MathF.Sin(Tau * hz * t);
    private static float Decay(float t, float rate) => MathF.Exp(-t*rate);

    private static float Room(int room, float t, Grain noise)
    {
        noise.Next(out float air, out float grit, out _);
        float fan = 1 + .13f * S(.5f, t);
        return room switch {
            0 => fan*(.09f*S(50,t) + .025f*S(100,t) + .015f*S(350,t) + .39f*air + .026f*grit),
            1 => (.6f + .3f*S(1f/6,t))*(.7f*air+.12f*grit) + .07f*S(43,t),
            _ => .10f*S(48,t) + .075f*S(51,t) + .06f*S(73,t) + (.5f+.15f*S(.5f,t))*air
        };
    }

    private static float Transient(string name, float t, Grain noise)
    {
        noise.Next(out float air, out float grit, out float edge);
        switch (name)
        {
            case "knock":
                return (.52f*S(163,t)+.26f*S(312,t))*Decay(t,32) +
                    .26f*S(641,t)*Decay(t,12) + .11f*S(1012,t)*Decay(t,17) + edge*.51f*Decay(t,220);
            case "step":
            case "step2":
                float shoe = name == "step" ? 73 : 68;
                return .45f*S(shoe,t)*Decay(t,27) + .85f*air*Decay(t,13) +
                    .39f*grit*Decay(Math.Abs(t-.085f),35);
            case "hinge":
                float sweep = MathF.Sin(Tau*(232*t+40*t*t)+1.7f*S(13,t));
                return MathF.Pow(MathF.Max(0, MathF.Sin(MathF.PI*t/1.13f)),.8f) *
                    (.19f*sweep+.10f*S(437,t)+.4f*grit)*(.58f+.28f*S(18,t));
            case "latch":
                float click = MathF.Max(0,t-.087f);
                return (.33f*S(910,t)+.48f*edge)*Decay(t,90) +
                    (t>.087f ? (.54f*S(271,click)+.5f*grit)*Decay(click,48) : 0);
            case "handle":
                return (.46f*S(193,t)+.64f*edge)*Decay(t,58)+.23f*S(879,t)*Decay(t,8) +
                    .23f*S(1274,t)*Decay(t,11) + grit*.4f*Decay(Math.Abs(t-.16f),38);
            case "wind":
                return MathF.Sin(MathF.PI*t/2.45f) * (.96f*air+.10f*grit+.09f*S(183,t));
            case "voices":
                // Two overlapping voiced vowels through a closed door. Soft consonant
                // gaps and shifting formants suggest conversation without exposition.
                return Voice(t,119,0,1.8f,air) + Voice(t,174,1.46f,2.18f,air);
            default: return 0;
        }
    }

    private static float Voice(float time, float fundamental, float start, float duration, float air)
    {
        float t=time-start;
        if (t<=0 || t>=duration) return 0;
        float phrase = MathF.Sin(MathF.PI*t/duration);
        float vowels = .14f + .86f * MathF.Pow(MathF.Abs(S(2.05f,t)),.75f);
        float phase = Tau*(fundamental*t + .66f*S(.85f,t));
        float vocal = .16f*MathF.Sin(phase)+.10f*MathF.Sin(phase*2)+
            (.11f+.07f*S(1.4f,t))*MathF.Sin(phase*4)+(.045f+.04f*S(.9f,t))*MathF.Sin(phase*8);
        return phrase*vowels*(vocal+air*.3f);
    }
}
