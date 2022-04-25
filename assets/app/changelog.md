Changelog for 1.6.8
===================
## Additions:

- Delete local replay
- Custom playfield size ranging from 80%-100%
    - Now you can keep your fingers away from system status bar!
- Sudden Death (SD) mod (unranked)
    - Automatically fails gameplay after a miss. Slider breaks will not trigger fail.
- Small Circle (SC) mod (unranked)
    - Adds the beatmap's circle size (CS) by 4, if you want to challenge yourself with way smaller circles.
- Perfect (PF) mod
    - SS or fail (unranked).
- Flashlight (FL) mod
    - Behaves similarly to osu! PC's FL mod, however the lightened area might be different (unranked).
    - The delay at which the lightened area follows your cursor can be adjusted to anywhere from 120ms to 1200ms.
- Really Easy (RE(z)) mod (unranked)
    - Easier EZ, only decreases AR slightly instead of cutting it in half.
- ScoreV2 mod (unranked)
    - Uses score calculation similar to osu!lazer.
    - Slider judgement stays the same (unchanged).
- Speed Modifier (unranked)
    - Adjusts a beatmap's speed between 0.5x and 2x.
    - Can be stacked with DT, NC, and HT.
- Force AR (unranked)
    - Forcefully sets a beatmap's AR to anywhere from 0 to 12.5.
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
    - Like average offset, this is only visible in gameplay if show FPS is enabled in Graphics settings.
    - When replaying a score, the result may differ from the original value due to the way replay is saved.

## Changes:

- Updated audio engine
    - Should improve performance and optimize latency.
- PR (Precise) mod is ranked
- EZ (Easy) mod has 3 lives
- RX (Relax) mod and AP (AutoPilot) mod replays will be saved with 0.01x score multiplier
- Updated star rating system
    - Improves calculation accuracy to be closer to osu! PC star rating.
- 10 simultaneous cursor input support
- Improved framerate for storyboard
- Custom skins are sorted alphabetically
- A cursor will appear when using Auto or AP (AutoPilot) mod
- Rewritten beatmap parser, now able to load some beatmaps that were not possible to load
- Sliders will gradually fade out if HD (Hidden) mod is active
- Slightly reworked the display of options list
- Changing volume-related options no longer requires a game restart
- Improved file-related operations' performance
- Use `HTTPS` protocol for all web-related operations
- Moved skin selection option to Graphics category
- Changed the game's domain to https://osudroid.moe/
- Changed leaderboard avatar to point to game domain instead of Gravatar

## Bug fixes:

- Fixed a bug where follow points that are too small will make the game lag
- Fixed a bug where multiple overlapping notes can be hit by only tapping once
- Fixed a bug where NC (NightCore) mod speed multiplier is not the same as DT (DoubleTime) mod in some beatmaps
- Fixed a bug where sliders with negative length or infinite BPM makes a beatmap unplayable
- Fixed a bug where some beatmaps can crash the game when tapping a note due to out of bound hitsounds
- Fixed a bug for spinners
    - Spinners with negative length or less than 50ms will spin automatically.
- Fixed a bug where some slider ticks fail to display correctly
- Fixed a bug where very high velocity reverse sliders (buzz sliders) have incorrect length
- Fixed a bug where certain skin sound elements will crash the game
    - The game will ignore audio files that have 0 B size.
- Fixed SD card bug by moving library cache file to the game's private directory
- Fixed a bug in navigation bar (should disable properly)
- Fixed a bug where score would show incorrectly above 100 million during gameplay
- Fixed a bug where beatmap length greater than 1 hour is displayed incorrectly
- Fixed a bug where full combo count in some beatmaps are inconsistent due to precision error in slider tick calculation
- Fixed a bug where combo count stays at 0 if complex effect is disabled without restarting the game
- Fixed a bug where score goes to negative value beyond 2,147,483,647 (score will be capped at said value)
- Fixed a bug where an ill-formed beatmap can crash the game during import process
- Fixed a bug where custom directories are not loaded properly
- Fixed a bug where player avatars in online leaderboard are not loaded when not using Wi-Fi connection
- Fixed a bug where re-watching replays can crash the game
- Fixed a bug where custom beatmap skin's hitcircle texture does not apply to sliders if not overridden
- Fixed a bug where background music volume setting does not apply in song selection menu
- Fixed a bug where some mutually exclusive mods can be selected together
- Fixed a bug where offline replays do not get saved
- Fixed a bug where beatmaps that haven't finished downloading gets imported
- Fixed a bug where some beatmaps would fail to load properly due to abnormal timing point placement
- Fixed a bug where hardware dither option is not working as intended
- Fixed a bug where retrying a beatmap with a storyboard causes the game to crash

## Removals:

- Removed split-screen support as the game restarts if used in split screen mode
- Removed average offset counter during gameplay

## Additions [+], changes [=], bug fixes [*], and removals [-] since the previous pre-release:

- [+] Added slider dim for FL (Flashlight) mod
- [+] Added break dim for FL (Flashlight) mod
- [+] Added a slight delay to FL (Flashlight) mod lightened area movement towards the cursor
    - Defaults at 120ms, but can be adjusted to anywhere from 120ms to 1200ms.
- [+] Added the ability to save failed replays
- [+] Added support for push notifications
- [+] Added a separate loading screen for any game-related operations that takes some time to process
- [+] Added the option to disable triangles animation
    - Users with issues while triangles animation is active are encouraged to try this option.
- [+] Added the option to replace all beatmaps' backgrounds with the current skin's `menu-background` image
    - Only affects in-game display. The actual beatmap background file will not be replaced.
- [+] Added the option to hide in-game UI
    - If enabled, this will hide combo bursts, health bar, combo counter, accuracy counter, and score counter during gameplay.
- [+] Added the option to scan download directory for `.osk` (skin) files
- [+] Added unstable rate (UR) in gameplay and result screen
    - Like average offset, this is only visible in gameplay if show FPS is enabled in Graphics settings.
    - When replaying a score, the result may differ from the original value due to the way replay is saved.
- [+] Added the option to rotate cursor into `skin.json`
    - Format in `skin.json` (set to `true` to enable, `false` to disable):
        ```json
        "Cursor": {
            "rotateCursor": true
        }
        ```
- [=] Updated audio engine
    - Should improve performance and optimize latency.
- [=] Sliders will gradually fade out if HD (Hidden) mod is active
- [=] Changing volume-related options no longer requires a game restart
- [=] Improved file-related operations' performance
- [=] Use `HTTPS` protocol for all web-related operations
- [=] FL (Flashlight) mod dim area now starts at the center of the screen
- [=] Moved PP information in score result scene
- [=] FL (Flashlight) mod dim area now disappears during break time
- [=] A cursor will appear when using Auto or AP (AutoPilot) mod
- [=] Slightly reworked the display of options list
- [=] Moved skin selection option to Graphics category
- [=] Changed the game's domain to https://osudroid.moe/
- [=] Changed leaderboard avatar to point to game domain instead of Gravatar
- [=] Renamed "display PP in score" option to "display score statistics in score" to incorporate unstable rate and hit error to the option
- [*] Fixed a bug where offline replays do not get saved
- [*] Fixed a bug where FL (Flashlight) mod dim area flickers if the player taps during break
- [*] Fixed a bug where beatmaps that haven't finished downloading gets imported
- [*] Fixed a bug where star rating calculation does not take speed multiplier into account
- [*] Fixed a bug where some animated hit results (100k, 300k, and 300g) are not displayed properly
- [*] Fixed a bug where some beatmaps would fail to load properly due to abnormal timing point placement
- [*] Fixed a bug where FL (Flashlight) mod dim area would go out of screen if a note is placed outside of playfield
- [*] Fixed a bug where hardware dither option is not working as intended
- [*] Fixed a bug where retrying a beatmap with a storyboard causes the game to crash
- [-] Removed split-screen support as the game restarts if used in split screen mode
- [-] Removed average offset counter during gameplay
