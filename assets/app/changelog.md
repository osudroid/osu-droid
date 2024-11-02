Version 1.8
===========

# Additions

## User interface redesigns

Many of menu-related user interfaces in the game have been redesigned to bring a refresh to old user interfaces.
The following menus have been redesigned:

- Options menu
- Mod customization menu (that pops up when "Settings" in the mod menu is pressed)
- Beatmap management dialog
- Score management dialog
- Game exit
- Beatmap search bar in song selection menu
- Multiplayer chat
- Multiplayer options
- Beatmap downloader download progress

## New ranking system

This update switches the global ranking system to use a metric called performance points that more accurately measures the
skill of a player.

Starting from this version, the "Score" text in your in-game profile card is replaced with "Performance" which denotes
your performance points.

Consequently, this switch also changes how your overall accuracy is calculated:
- Previously, your overall accuracy was calculated by averaging the accuracy of all scores you have submitted.
- Now, your overall accuracy is calculated by calculating a weighted average of your top scores' accuracy.

Your in-game profile and in the website now displays your top 100 scores in terms of performance points.

If you are coming from osu!stable, osu!lazer, or know what performance points are, it should be noted that osu!droid
does not use the same algorithm as osu!standard. The algorithm used in osu!droid is a heavily modified algorithm that
is designed to reflect osu!droid's gameplay environment. This algorithm is colloquially known as "osu!droid pp" or
dpp for short.

## Input block area

This new addition allows you to block specific areas within your screen from receiving presses during gameplay. Useful
for preventing accidental touches.

## Smaller additions

- Added support for pp-based global beatmap leaderboard in song selection menu
- Added video setting summary regarding supported video formats
- Added support for manually importing replay files in options
  - Can be accessed under the "Library" section
  - Both `.odr` and `.edr` files are supported. However, only `.odr` files from osu!droid version 1.6.7 onwards are supported
- Added a new button in song selection menu to switch difficulty algorithm between osu!droid and osu!standard
  - The button can be skinned with the following skin element names:
    - `selection-difficulty-droid`
    - `selection-difficulty-droid-over`
    - `selection-difficulty-standard`
    - `selection-difficulty-standard-over`
  - The button can be customized in `skin.json`'s `Layout` configuration under `DifficultySwitcher`, just like `ModsButton`
    and the like. Here is an example configuration:
    ```json
    {
      "Layout": {
        "useNewLayout": true,
        "DifficultySwitcher": {
          "x": 108.2,
          "y": 195.4,
          "w": 120.5,
          "h": 132.1,
          "scale": 1.25
        }
      }
    }
    ```
- Added support for `sliderslide` and `sliderwhistle` hitsounds
- Added support for `check-off`, `check-on`, `click-short`, and `click-short-confirm` skin samples
- Added support for `SamplesMatchPlaybackRate` beatmap configuration
- Added support for `scorebar-marker` texture
- Added support for custom file hitsounds
- Added support for circular song progress indicator in gameplay
- Added support to cancel gameplay loading by pressing the back button
- Added an animation to slider end arrow rotation when snaking animation is enabled
- Added a slight dim to hitobjects that cannot be hit yet in gameplay
- Added a rotation effect to miss hit judgement effects in gameplay
- Added most common BPM metric to beatmap information in song selection menu
- Added force maximum refresh rate setting
  - This may not work as the system may prevent the game from setting its own refresh rate 
- Added support for `skin.ini`'s `AnimationFramerate` setting in `skin.json`
  - The default value is `-1` when converting a `skin.ini` to `skin.json`, and `60` otherwise
  - To use in `skin.json`, add the following entry:
    ```json
    {
      "Utils": {
        "animationFramerate": 60
      }
    }
    ```
- Added support for `skin.ini`'s `LayeredHitSounds` setting in `skin.json`
  - To use in `skin.json`, add the following entry:
    ```json
    {
      "Utils": {
        "layeredHitSounds": true
      }
    }
    ```
- Added support for `skin.ini`'s `SpinnerFrequencyModulate` setting in `skin.json`
  - To use in `skin.json`, add the following entry:
    ```json
    {
      "Utils": {
        "spinnerFrequencyModulate": true
      }
    }
    ```
- Added support for `skin.ini`'s `SliderBallFlip` setting in `skin.json`
  - To use in `skin.json`, add the following entry:
    ```json
    {
      "Slider": {
        "sliderBallFlip": true
      }
    }
    ```
- Added support for `skin.ini`'s `ScoreOverlap` setting in `skin.json`
  - To use in `skin.json`, add the following entry:
    ```json
    {
      "Fonts": {
        "scoreOverlap": 0
      }
    }
    ```
- Added support for `skin.ini`'s `ComboOverlap` setting in `skin.json`
  - To use in `skin.json`, add the following entry:
    ```json
    {
      "Fonts": {
        "comboOverlap": 0
      }
    }
    ```

# Changes

## Minimum Android version requirement

osu!droid version 1.8 has an increased minimum Android version of 7, effectively removing support for Android 5, 5.1, and 6.

## Storage migration

This update migrates the storage location of local scores, beatmap collections, and beatmap options into an integrated
database. Doing this increases the import time of beatmaps and fixes a few problems, namely:
- Duplicated beatmapsets in song selection menu
- Scores in local leaderboard potentially not showing in its beatmap

## Background difficulty calculation

This update moves the difficulty calculation process of beatmaps that would normally be done during import into a
background process. This significantly improves the import time of beatmaps. As consequences, the star rating sorting in
song select menu will not work properly while the background process is running, and all beatmaps that have not been
calculated will display a star rating of 0.

During gameplay, background difficulty calculation is paused to prevent performance degradation.

## Updated gameplay elements

The following gameplay elements' display has been updated to match osu!stable:

- Follow points
  - In addition to display parity, its skin element (`followpoint`) can now be animated
- Health bar
- Hit lighting
- Score counter
- Combo counter
- Accuracy counter

## Smaller changes

- Circles and sliders can now be hit as early as 400ms before the circle's start time
  - Previously, circles could immediately be hit after the approach circle has progressed halfway.
  - Consequently, sliders could be hit only after its slider head enters the meh hit window, preventing the player from
    slider breaking for hitting a slider's head too early.
- More significant performance improvements in gameplay than version 1.7.2
- Separated average offset and unstable rate displays in gameplay into separate settings
- Miss hit judgement effect only plays in a slider's tail rather than its head and tail
- The `sliderfollowcircle` and `sliderb` skin elements can now be animated
- Optimized engine buffer update and writing logic
- Optimized gameplay loading time by only reloading beatmap when necessary
- Optimized real-time PP counter by not recalculating a beatmap's difficulty when retrying it
- Optimized real-time PP counter's update operation after an object's judgement
- Optimized osu!standard difficulty calculation's difficulty spike nerf application
- Optimized beatmap switching operation in song selection menu
- The `x` and `y` properties in `skin.json`'s `Layout` configuration now affects a button's position
- CS and OD conversions in song selection menu are now displayed using osu!droid metrics rather than osu!standard
- Background music in song selection menu now adjusts based on selected mods and settings
- Changed "chimu.moe" mention to beatmap downloader when there are no songs
- Reversed the way offsets are applied - positive values now mean objects appear earlier
- Background music now reduces its volume when going from song selection menu to main menu
- Increased the speed of background music volume ramp up after selecting a beatmapset in song selection menu
- Matched skip time behavior with osu!stable
- Revamped FPS counter
  - Maximum FPS is now displayed
  - Now displays two FPS counters: update FPS and draw FPS. The previous FPS counter only displays update FPS
  - Changes color based on current FPS compared to maximum FPS
- Gameplay HUD now ignores playfield size setting
- The real-time PP counter has been moved next to circular song progress or accuracy counter
  - Its `d` and `p` letters can be skinned with the `score-d` and `score-p` skin elements 
- Average offset and unstable rate counters are now hidden during autoplay
- Updated Korean, Japanese, and Russian translations

# Removals

- Removed the "Calculate slider path" setting. It is now enabled by default thanks to the aforementioned performance improvements
- Removed internal volume adjustment of normal, whistle, and clap hitsounds, where normal and whistle hitsounds' volume
were reduced by 20% and clap hitsounds' volume were reduced by 15%
- Removed the "Player Name" setting. It is now combined with the "Username" option
- Removed fractional part of real-time PP and unstable rate counters
- Removed fractional part of beatmap BPM displays in song selection menu

# Bug fixes

- Fixed beatmapsets potentially getting duplicated after import operation
- Fixed login fail messages other than "Cannot connect to server" not displaying properly
- Fixed "Show FPS" setting affecting the display of other counters (average offset, unstable rate, and real-time PP counter)
- Fixed wrong textures being displayed before in-game video plays on devices with Mali GPUs
- Fixed real-time pp counter taking the next object's difficulty attributes when the current object is active in gameplay
- Fixed real-time pp counter text potentially getting cut off
- Fixed some gameplay animations not being affected by speed multiplier
- Fixed pp being calculated and displayed in multiplayer team mode
- Fixed multitouch detection not working properly due to improperly specified hardware features from the system
- Fixed device back button resuming gameplay after game over when playing Sudden Death + Relax/Autopilot
- Fixed object hits potentially playing a different (and unintended) hitsound when hit with a different offset
- Fixed kiai flashing time being based on the first timing point rather than the currently active timing point
- Fixed slider tick judgement in reversed spans being inaccurate under certain conditions
- Fixed slider tick judgement in a slider's span potentially resuming even after all ticks in the span have been judged, giving more combo than intended
- Fixed slider tick judgement in a slider's span potentially being completely skipped in very short sliders under low frame rate
- Fixed slider tracking state recovering when the player enters the follow circle area rather than the slider ball
- Fixed modern spinners not playing sampleset-specific and addition-specific hitsounds on a successful (non-miss) hit
- Fixed modern spinners being able to be spun before the start time of the spinner, giving more score than classical spinners
- Fixed modern spinners not auto-completing like classical spinners when its duration is under 50ms
- Fixed a slider's length potentially falling short from its expected length
- Fixed force OD not being affected by speed multiplier in song selection menu
- Fixed a slider head's hit window not being capped at the slider's span duration
- Fixed slider ends increasing combo when not hit
- Fixed animatable textures not being unloaded properly when changing skins
- Fixed old (non-skin.json) button layouts using fixed height for offset rather than texture-based height
- Fixed beatmap background and video paths potentially being parsed incorrectly
- Fixed music preview time defaulting at 50% music length instead of 40%
- Fixed ScoreV2 value not updating after misses
- Fixed sound volume setting being doubly applied in some situations
- Fixed current mods state not being saved when going out from song selection menu
- Fixed background music volume not ramping up upon leaving song selection menu
- Fixed object starting point potentially being screwed up
- Fixed object approach rate being rounded up (making it off by at most 0.5ms in real time)
- Fixed memory leak when reading and saving replays
- Fixed misleading metronome effect setting description, denoting that it is only applied to the NightCore mod
- Fixed crash when attempting to restore game state after the system kills the game due to low memory
- Fixed cancel button in beatmap downloader not working properly
- Fixed object judgement relying on non-replay judgement processing when replaying in gameplay