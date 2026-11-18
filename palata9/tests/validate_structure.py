from pathlib import Path
import re
p = Path(__file__).resolve().parents[1]
for name in ["project.godot", "scenes/main.tscn", "export_presets.cfg", "scripts/main.gd", "scripts/world.gd", "scripts/player.gd", "scripts/overlay.gd", "scripts/mobile_controls.gd", "tests/smoke_test.gd"]:
    assert (p / name).is_file(), name
for source in (p / "scripts").glob("*.gd"):
    text = source.read_text(encoding="utf-8")
    assert '\r' not in text
    assert not re.search(r'\t', text)
for asset in ["left","right","up","down","action","restart"]:
    assert (p / "assets" / "touch" / f"{asset}.svg").exists(), asset
assert 'import_etc2_astc=true' in (p / "project.godot").read_text()
print('PALATA9_STATIC_TEST_PASSED')
