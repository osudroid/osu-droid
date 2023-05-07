Changelog for 1.6.8
===================
## Additions:

- Delete local replay
- Custom playfield size ranging from 80%-100%
  - Now you can keep your fingers away from system status bar!
- Sudden Death (SD) mod
  - Automatically fails gameplay after a miss. Slider breaks will not trigger fail.
- Perfect (PF) mod
  - SS or fail.
- Flashlight (FL) mod
  - Behaves similarly to osu!stable's FL mod, however the lightened area might be different.
  - The delay at which the lightened area follows your cursor can be adjusted to anywhere from 120ms to 1200ms.
  - Setting the delay to other value than 120ms will make the mod unranked.
- Speed Modifier
  - Adjusts a beatmap's speed between 0.5x and 2x.
  - Can be stacked with the DoubleTime (DT), NightCore (NC), and HalfTime (HT) mods.
- Small Circle (SC) mod (unranked)
  - Adds the beatmap's circle size (CS) by 4, if you want to challenge yourself with way smaller circles.
- Really Easy (RE(z)) mod (unranked)
  - Easier EZ, only decreases AR slightly instead of cutting it in half.
- ScoreV2 mod (unranked)
- Force AR (unranked)
  - Forcefully sets a beatmap's approach rate (AR) to anywhere from 0 to 12.5.
  - Ignores any effect from mods.
- User Panel
  - Tap your profile in main menu to go to your profile page!
- Internal Audio Recording
  - Only supported for Android 10 or above.
- Option to remove slider lock and spinner lock
  - Scores made with this option enabled will be unranked.
- Exit dialog prompt when you want to exit the game
  - No more accidental exits when you mistap or accidentally press your screen!
- Sort beatmap by stars and length
- Search beatmap based on difficulty name
- Multiple input support in one frame, making double-tap possible
- Animated `hit0`, `hit50`, `hit100`, `hit300`
  - Using these elements animated will differentiate them from their displayed element on result screen.
  - Up to 60 frames are supported
- Random welcome sound
- Customizable exit sound via skins
- Warning message if your storage is low to prevent replay/score corruption
  - Low storage causes bugs such as unwatchable replays and inability to watch local replays.
- Option to precalculate slider path
  - Improves performance during gameplay at the cost of beatmap loading time.
- Option to show score statistics when opening a score
  - Score statistics includes star rating, PP (performance points), and more.
- Option to hide replay text when watching a replay
- Option to save failed replays
- Option to hide in-game UI
  - If enabled, this will hide progress bar, combo bursts, health bar, combo counter, accuracy counter, and score counter during gameplay.
- Option to replace all beatmaps' backgrounds with the current skin's `menu-background` image
  - Only affects in-game display. The actual beatmap background file will not be replaced.
- Option to disable triangles animation
  - Users with issues while triangles animation is active are encouraged to try this option.
- Separate loading screen for any game-related operations that takes some time to process
- Support for push notifications
- Option to scan download directory for `.osk` (skin) files
- Display unstable rate (UR) in gameplay and result screen
  - Only visible in gameplay if show FPS is enabled in Graphics settings.
  - When replaying a score, the result may differ from the original value due to the way replays are saved.
- Live pp counter
  - Only visible if show FPS in enabled in Graphics settings.
  - Can be toggled via Advanced settings.
- Option to rotate cursor in `skin.json`
  - Format in `skin.json` (set to `true` to enable, `false` to disable):
    ```json
    "Cursor": {
        "rotateCursor": true
    }
    ```
- In-game beatmap downloader, using chimu.moe
  - Due to some limitations, only one beatmapset can be downloaded at once.

## Changes:

- Updated audio engine
  - Should improve performance and optimize latency.
- The Precise (PR) mod is ranked
- The Easy (EZ) mod has 3 lives
- The Relax (RX) mod and AutoPilot (AP) mod replays will be saved with 0.01x score multiplier
- Updated star rating and performance points system
  - Improves calculation accuracy to be closer to osu!stable star rating.
- 10 simultaneous cursor input support
- Improved frame rate for storyboard
- Custom skins are sorted alphabetically
- Automated cursor movement in Auto or AP mod
- New beatmap parser, now able to load some beatmaps that were not possible to load
- Sliders will gradually fade out if the Hidden (HD) mod is active
- Reworked the display of options list
- Changing volume-related options no longer requires a game restart
- Improved file-related operations' performance
- Use `HTTPS` protocol for all web-related operations
- Moved skin selection option to Graphics category
- Changed the game's domain to https://osudroid.moe/
- Changed leaderboard avatar to point to game domain instead of Gravatar
- New music player notification style
  - The notification may persist even when the game is closed, however the notifcation should be swipeable.
- Separated snaking sliders into its own option
- Improved game visuals
  - Adjusted the fade-in duration of circles and sliders for non-HD and HD
  - Adjusted the fade-out duration of circles and sliders for HD
  - Added animations to a slider's follow circle when it gains or loses tracking
  - Slider head and tail overlays will fade properly when HD is used

## Bug fixes:

- Fixed a bug where follow points that are too small will make the game lag
- Fixed a bug where multiple overlapping notes can be hit by only tapping once
- Fixed a bug where the NC mod's speed multiplier is not the same as the DT mod in some beatmaps
- Fixed a bug where sliders with negative length or infinite BPM makes a beatmap unplayable
- Fixed a bug where some beatmaps can crash the game when tapping a note due to out of bound hitsounds
- Fixed a bug where spinners that are too short will count as misses
  - Spinners with negative length or less than 50ms will spin automatically.
- Fixed a bug where some slider ticks fail to display correctly
- Fixed a bug where very high velocity reverse sliders (buzz sliders) have incorrect length
- Fixed a bug where certain skin sound elements will crash the game
  - The game will ignore audio files that have less than 1 KB size.
- Fixed a bug where the navigation bar may not be disabled properly
- Fixed a bug where the score would show incorrectly above 100 million during gameplay
- Fixed a bug where beatmap length greater than 1 hour is displayed incorrectly
- Fixed a bug where the full combo count in some beatmaps are inconsistent due to precision error in slider tick calculation
- Fixed a bug where the combo count stays at 0 if complex effect is disabled without restarting the game
- Fixed a bug where the score goes to negative value beyond 2,147,483,647 (score will be capped at said value)
- Fixed a bug where an ill-formed beatmap can crash the game during import process
- Fixed a bug where custom beatmap or skin directories are not loaded properly
- Fixed a bug where player avatars in online leaderboard are not loaded when not using Wi-Fi connection
- Fixed a bug where re-watching replays can crash the game
- Fixed a bug where custom beatmap skin's hitcircle texture does not apply to sliders if not overridden
- Fixed a bug where the background music volume setting does not apply in song selection menu
- Fixed a bug where some mutually exclusive mods can be selected together
- Fixed a bug where offline replays do not get saved
- Fixed a bug where beatmaps that haven't finished downloading gets imported
- Fixed a bug where some beatmaps would fail to load properly due to abnormal timing point placement
- Fixed a bug where hardware dither option is not working as intended
- Fixed a bug where retrying a beatmap with a storyboard causes the game to crash
- Fixed an issue where the game can potentially lock up for a while when playing a beatmap with online leaderboard

## Removals:

- Removed split-screen support as the game restarts if used in split screen mode
- Removed average offset counter during gameplay
- Removed the "Super slider" graphics settings (it is now enabled by default)