"""用 Edge headless 把 docs/logo.svg 渲染成 5 个密度的 PNG。

完整支持 SVG <linearGradient>。
"""
import subprocess
import tempfile
from pathlib import Path

ROOT = Path("E:/Projects/PhotoBox")
SVG = ROOT / "docs" / "logo.svg"
EDGE = Path("C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe")

# 5 个密度
DENSITIES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def render(size: int) -> Path:
    """渲染单个尺寸的 PNG 到 res/drawable/_tmp_logo_{size}.png"""
    out = ROOT / f"app/src/main/res/drawable/_tmp_logo_{size}.png"
    html = f"""<html><head><style>
body {{ margin:0; padding:0; background:transparent }}
img {{ display:block }}
</style></head>
<body><img src="file:///{SVG.as_posix()}" width="{size}" height="{size}"></body></html>"""
    html_file = Path(tempfile.gettempdir()) / f"_logo_{size}.html"
    html_file.write_text(html, encoding="utf-8")
    try:
        subprocess.run(
            [
                str(EDGE),
                "--headless",
                "--disable-gpu",
                "--no-sandbox",
                "--hide-scrollbars",
                f"--window-size={size},{size}",
                f"--screenshot={out}",
                f"file:///{html_file.as_posix()}",
            ],
            check=True,
            timeout=30,
        )
    finally:
        html_file.unlink(missing_ok=True)
    return out


def main():
    for name, size in DENSITIES.items():
        png = render(size)
        out_dir = ROOT / "app/src/main/res" / f"mipmap-{name}"
        out_dir.mkdir(parents=True, exist_ok=True)
        for variant in ("ic_launcher.png", "ic_launcher_round.png"):
            target = out_dir / variant
            target.write_bytes(png.read_bytes())
        png.unlink(missing_ok=True)
        print(f"  {name} ({size}x{size}) → {out_dir}")


if __name__ == "__main__":
    main()