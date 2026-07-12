# Play Store Screenshots

Notes on capturing and sizing screenshots for the Play Store listing.

## Emulator vs. real device

Both are accepted by Google Play — there's no requirement to use a physical
device. The only rule is that screenshots must show the real app's actual
Android UI (no mockups, no other platform's frame — e.g. don't submit
screenshots that look like iOS).

> Practically: the emulator is the easier source since you can hit exact
> pixel dimensions without any device-specific scaling, but a real-device
> shot works equally well if it meets the size requirements below.

## Technical requirements

- Aspect ratio between **16:9 and 9:16** (portrait or landscape)
- Minimum **320px** on the short side
- Maximum **3840px** on the long side
- Recommended phone size: **1080 x 1920**
- At least **2 screenshots** required for the phone listing

Our captures (via `sim.sh snap`, see below) come out at **1080 x 2280** on the
emulator and the same on the test Samsung device — both within range.

## Capturing a screenshot

From the project root:

```bash
./sim.sh snap                  # saves ~/Downloads/snap-<timestamp>.png
./sim.sh snap my-name.png      # saves ~/Downloads/my-name.png
```

This pulls a raw PNG straight off the connected device/emulator framebuffer
via `adb exec-out screencap -p`, so there's no compression or re-encoding
step to worry about. If more than one device/emulator is attached, the
script will list them and exit rather than guessing which one to capture —
shut down the one you don't want first.

## Troubleshooting

**"More than one device/emulator attached"**
Kill the emulator (close its window, or `adb -s <serial> emu kill`) if
you're capturing from the real device, or vice versa.

**Empty/failed screenshot**
Make sure the device screen is on and unlocked before running `./sim.sh snap`.

## Sources

- [App screenshot sizes and guidelines for the Google Play Store](https://www.mobileaction.co/guide/app-screenshot-sizes-and-guidelines-for-the-google-play-store/)
- [Google Play Store Screenshot Requirements](https://theapplaunchpad.com/blog/google-play-store-screenshot-requirements/)
- [Play Store Screenshot Size & Dimensions — ScreenKit](https://screenkit.tools/specs/google-play-screenshot-sizes)
