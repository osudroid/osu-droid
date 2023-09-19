Version 1.7.1 Update (September 20, 2023)
=========================================

# Additions

## Support for speed multiplier, force AR, and FL follow delay in multiplayer

Players can now use speed multiplier, force AR, and FL follow delay in multiplayer via the mod selection menu.

Keep in mind that there are some important things to note:
- Speed multiplier is uniform across all players in a multiplayer room (only the host can set it)
- Force AR and FL follow delay are uniform across all players without free mod. If free mod is enabled, players can set their own settings.

## Option to disable score submission in multiplayer

Players can now disable score submission in multiplayer (as in, their scores will not submit towards solo leaderboards). This option can be toggled on in-room options.

## Support for "remove slider lock" setting in multiplayer

Multiplayer room hosts can now toggle the "remove slider lock" setting via in-room options. Keep in mind that this setting does not follow what is currently set from main menu options.

This setting is uniform across all players, meaning only room hosts can toggle this setting regardless of free mod.

## Fail and recovery system in multiplayer

In version 1.7, if a player fails in multiplayer, nothing happens to their score, except the score will not be saved and will not be submitted towards solo leaderboards.

In version 1.7.1, failing in multiplayer enters the player to a "failing state". In this state, their score will be put under other scores that are not in the state, both in in-game leaderboard and result screen. In Team VS mode, their scores will not count towards their team's total score.

In order for a player to recover from the failing state, they need to restore their HP to 100%. They must do so before the beatmap ends.

## Floating beatmap information in multiplayer room

Tapping on the beatmap card inside a multiplayer room will display a popup that contains the BPM, length, CS, AR, OD, HP, and star rating of the beatmap.

For multiplayer room hosts, they may swipe briefly when viewing this popup to avoid entering the song selection menu.

# Changes

## Select the previously picked beatmap when switching from song selection menu to multiplayer room

Previously, switching from song selection menu to multiplayer room will set an empty beatmap. Now, the previous beatmap will be set instead.

## Raise global offset limit to ±500ms

This allows more flexibility over audio offset, especially for players with wireless audio device.

## Cap difficulty multiplier at reasonable amounts

This change aims to prevent another score abuse factor by setting difficulty variables (CS, OD, and HP) to an extremely high value.

## Allow multiplayer room host to force start the game

Previously, multiplayer room hosts need to wait for all players who can play to be ready in order to start the game. Now, the status of players, including those who can play, will be ignored.
 
## Optimized global leaderboard loading performance

This change brings a bunch of performance improvements to reduce CPU and memory usage when global leaderboard is enabled, both in song selection menu and gameplay.

## Reordered beatmap information in multiplayer

The beatmap artist and title are now at the top, while the creator and the difficulty name are at the bottom.

# Bug fixes

## Fixed replay saving process potentially producing a corrupted replay

This bug has existed for a long time. However, it was heavily exacerbated by the input processing change that was introduced in version 1.7.

Unfortunately, due to the root cause of this bug, it is very hard to create a fix for corrupted replays, which means these replays will stay corrupted indefinitely.

## Use multiplayer room mod settings when entering multiplayer room

This bug led to players being able to use custom settings outside of mods, such as speed multiplier, force AR, and FL follow delay, which gave an unfair advantage to other players provided that they can play with the settings.

## Fixed a crash during game launch due to audio engine initialization

This bug was pretty critical and hard to solve, as the crash was caused by the Android system rather than the game. Some players report that this goes as far as restarting their device.

Players who were completely unable to launch the game for unknown reasons are recommended to try this new version.

## Fixed gameplay video not working if storyboard is also used (and present)

Videos will now display behind storyboards as a background, rather than storyboards completely blocking videos with beatmap backgrounds.

## Fixed "tap twice to exit" not working properly during gameplay in multiplayer

The maximum time allowed between both taps was supposed to be 300ms. However, it was found that the time determination is inconsistent. This is now fixed to always be consistent. 

## Fixed mod score multiplier applying twice to other players' scores in multiplayer

Mod score multipliers will only apply once now.

# Removals

## Removed "reduce input delay" option

This is a legacy option that was introduced in 1.6.6, and is now unnecessary to have, especially since the input processing change that was introduced in version 1.7.

# Smaller changes

- Fixed a grammatical error when a player tries to ready when a multiplayer room's host is changing beatmap
- Fixed a crash when the game does not recognize some inputs
- Shrink star in multiplayer room beatmap card if it represents a fraction
- Invalidate difficulty calculation cache after a period of time to reduce memory usage
- Fixed sliders before any timing point taking the first timing point to determine velocity rather than a default timing point
- Fixed a potential crash when new update checking fails