Version 1.7 Update (August 20, 2023)
====================================

# Additions
## Multiplayer
This feature is in beta, as it lacks additional features that are nice to have, such as beatmap failing status in the live
leaderboard, failing gameplay mechanic (where your score would be put under any scores that are not failing), failing
recovery, and more that we have probably not noticed. As such, we are open to feedback.

Despite that, this feature has been tested by multiple testers and is stable enough to warrant a public beta testing.

To use this feature, you must be logged in to the server. It can be accessed from the main menu after pressing "Play".

## Gameplay video
This feature is in beta. As of now, the game supports H.264-encoded videos under the `.mp4`, `.3gp`, `.mkv`, and `.webm`
file formats. You may enable this feature in the settings under the mod selection menu.

Keep in mind that due to compatibility, devices running in Android versions below 6 will be unable to see gameplay video
if they use speed-changing modifications such as DoubleTime, NightCore, HalfTime, and the speed multiplier setting.

By default, the game will automatically delete videos that are not supported. If you wish to keep the videos, you may
opt out from this behavior by disabling the "Delete unsupported videos" setting found in the "Beatmaps" sub-menu.

## "Seek to previously selected beatmap" in random button
Often times, you end up pressing the random button accidentally without knowing which beatmap you were on. To solve this
problem, the random button now supports previous seeking. This similar to osu!stable's behavior when you press `Shift + F2`.

To use this feature, you may hold the random button briefly. Holding it continuously will cycle through your 10 most
recently switched beatmaps, regardless of whether you selected the previous beatmap via the random button.

# Changes
## Improved forbidden accessibility services information
The game will inform you about the services that you must disable in order to play the game. 
Additionally, the game will provide a shortcut to your accessibility settings if your system supports it. 

## Improved cursor trail
The cursor trail has been improved to match osu!stable's cursor trail closer.

## Moved touch handler from update to input thread
This is one of the main reasons why the game's response towards cursor movement feels unresponsive and sluggish. Touch
or input latency during gameplay should be improved by a margin for most players.

# Bug fixes

- Fixed incorrect stacking for sliders in very old beatmaps in difficulty calculation
- Fixed a potential case where timing points can get loaded twice in gameplay
- Fixed a potential case where clearing the game's cache when it is running can crash the game
- Fixed an issue where checking for updates when the game server is unavailable will crash the game
- Fixed a potential case where an empty beatmap may show up after a beatmap import process has finished
- Fixed an issue where the game is insensitive towards beatmap file changes
- Fixed an issue where the game would crash if it fails to play certain sound samples