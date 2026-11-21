#!/usr/bin/env python3
"""Turns raw phone captures into Play Store screenshots.

Raw device captures (e.g. 1080x2424) exceed Play's 2:1 max aspect ratio, so each one is placed
on a 1080x1920 (9:16) brand canvas with a headline, a subline and the screen in rounded corners.

    python3 tools/frame_screenshots.py              # frames playstore/screenshots/raw/*.png
Captions come from CAPTIONS below, keyed by raw file name (without extension). Needs ImageMagick.
"""
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RAW = ROOT / "playstore/screenshots/raw"
OUT = ROOT / "playstore/screenshots/phone"
FONT = ROOT / "app/src/main/res/font/google_sans_flex.ttf"

W, H = 1080, 1920
SCREEN_H = 1560            # height of the phone screen inside the frame
SCREEN_TOP = 300           # screen starts just below the caption
RADIUS = 56

# file stem -> (background, headline, subline)
CAPTIONS = {
    "01-home":       ("#3A44B8", "Browse your way", "An independent browser on the Gecko engine"),
    "02-shield":     ("#5B3A9E", "Ads and trackers, gone", "Zuno Shield blocks them before they load"),
    "03-tabs":       ("#2F6E8E", "Tabs, the smarter way", "Vertical tabs, workspaces and pinned sites"),
    "04-private":    ("#2A1F4E", "Private, for real", "Separate tabs. Nothing saved."),
    "05-background": ("#8E3A5B", "Keep the music going", "Background play with lock screen controls"),
    "06-settings":   ("#3A44B8", "Make it yours", "Wallpaper colours, dark mode and more"),
    "07-siteinfo":   ("#2F6E8E", "Know every site", "Connection, certificate and blocking details"),
}


def run(*args):
    subprocess.run([str(a) for a in args], check=True)


def frame(src: pathlib.Path) -> pathlib.Path:
    bg, headline, sub = CAPTIONS.get(src.stem, ("#3A44B8", src.stem.split("-", 1)[-1].title(), ""))
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = OUT / f".{src.stem}.screen.png"
    dst = OUT / f"{src.stem}.png"
    # Screen: scale to SCREEN_H tall, round the corners.
    run("magick", src, "-resize", f"x{SCREEN_H}",
        "(", "+clone", "-alpha", "extract", "-fill", "black", "-colorize", "100",
        "-fill", "white", "-draw", f"roundrectangle 0,0 %[fx:w-1],%[fx:h-1] {RADIUS},{RADIUS}", ")",
        "-alpha", "off", "-compose", "CopyOpacity", "-composite", tmp)
    run("magick", "-size", f"{W}x{H}", f"xc:{bg}",
        # soft decorative circles in the brand style
        "-fill", "rgba(255,255,255,0.07)", "-draw", f"circle {W - 90},{H - 60} {W + 260},{H - 60}",
        "-fill", "rgba(255,255,255,0.05)", "-draw", "circle 60,120 60,-160",
        "-font", FONT, "-fill", "white", "-pointsize", "76", "-gravity", "north",
        "-annotate", "+0+96", headline,
        "-fill", "#FFD8E4", "-pointsize", "38", "-annotate", "+0+200", sub,
        # drop shadow + screen
        "(", tmp, "-background", "rgba(0,0,0,0.45)", "-shadow", "40x24+0+18", ")",
        "-gravity", "north", "-geometry", f"+0+{SCREEN_TOP - 24}", "-compose", "over", "-composite",
        tmp, "-gravity", "north", "-geometry", f"+0+{SCREEN_TOP}", "-compose", "over", "-composite",
        "-crop", f"{W}x{H}+0+0", "+repage", "-alpha", "off", dst)
    tmp.unlink(missing_ok=True)
    return dst


def main():
    raws = sorted(RAW.glob("*.png"))
    if not raws:
        sys.exit(f"no raw screenshots in {RAW}")
    for src in raws:
        print("framed", frame(src).relative_to(ROOT))


if __name__ == "__main__":
    main()
