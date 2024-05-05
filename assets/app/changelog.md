Version 1.7.2 Patch 3
=====================

## Additions
- Added a (more) apparent way to retry beatmap loading in beatmap downloader if the original attempt fails

## Changes
- Changed user agent in beatmap downloader to bypass web crawler preventions
  - This speeds up requests in beatmap downloader.

## Fixes
- Fixed another crash when the game detects low available memory
- Fixed a crash when entering new player name

Version 1.7.2 Patch 2
=====================

## Changes
- Changed "Chimu" in multiplayer beatmap card to something else that denotes download availability

## Fixes
- Fixed crash when the game detects low available memory
- Fixed crash when leaving multiplayer

Version 1.7.2 Patch 1
=====================

## Additions

- Added ability to display the current beatmap's ranking status
  - Powered by osu.direct, thank you!

## Changes
- Replaced Chimu with an integrated beatmap downloader
  - Supports multiple mirrors, but currently only supports osu.direct.
- Combo text shadow now fades 2.5 times faster
- Updated copyright year

## Fixes
- Fixed flipped custom difficulty statistics score multiplier
- Fixed in-game updater not working due to missing app permission
- Fixed beatmap picking in multiplayer not working properly if the first beatmap in a beatmapset is selected
- Fixed disconnect during gameplay in multiplayer restarting the entire beatmap
- Fixed failing status in multiplayer not accounting for Easy mod lives
- Fixed HR/REZ/EZ not disabling upon enabling all force difficulty statistics if the mods are enabled first
- Fixed NC with speed modify not giving sound pitch
- Fixed storyboard overlay layer not correctly handled
- Fixed auto cursor falling behind fast moving objects
- Fixed replay cursor recording time desynchronization

Version 1.7.2
=============

# Additions

## Force difficulty statistics (CS, OD, and HP)

This release adds force CS, OD, and HP to accompany its AR sibling. Like force AR, they keep their respective difficulty
statistic constant at a certain value while ignoring effect from game modifications and speed modify.

### Score multiplier

Force CS and OD have their respective score multipliers that decreases or increases depending on how far you are from
a beatmap's original statistics.

Force HP does not have a score multiplier as it does not increase the difficulty to get
an SS rank in a beatmap.

Assessing difficulty from AR is significantly more difficult and beatmap-dependent.
There are beatmaps where increasing AR makes them more difficult, and there are beatmaps where decreasing AR gives the
same effect.

### Multiplayer

This feature is available in multiplayer, and behaves exactly like force AR. Players can change the settings only when
free mod is enabled, otherwise only the room's host can do so.

The room host can disallow players from changing force difficulty statistics in free mod by disabling the "Allow force
difficulty statistics in free mod" setting in room options.

### Mod restrictions

When all force difficulty statistics are used, the Hard Rock (HR), Easy (EZ), and Really Easy (RE) mods cannot be used.
The aforementioned mods do not have any effect when all difficulty statistics are forced.

## New update system

This release ships a new update system.

Instead of redirecting you to the game's website, the new update system allows you to download the update and install it 
directly in-game.

## Multiplayer reconnection system

In previous releases' multiplayer, disconnecting from the server instantly kicks you from the room. This is not desired
as disconnections happen quite often (for example, when your device switches from mobile data to Wi-Fi or vice versa).

In this release, the client will attempt to reconnect to the server within 30 seconds after disconnection before finally
kicking you from the room.

## Smaller additions

- Added default slider hint for converted `.osk` skins
- Added a graphics option to not change background dim level during breaks

# Changes

## Gameplay optimizations

Another bunch of gameplay optimizations have made their way into this release. You should expect less input delay in
this release.

## Smaller changes

- Decreased playfield size setting minimum value to 50%
- Changed playfield size setting to a slider
- Selecting a beatmap with different music within the same beatmapset in song selection menu will play the music
- Rearranged room information text in multiplayer to account for force difficulty statistics
- Chat sound in multiplayer will not play during gameplay
- Decreased chat sound volume in multiplayer by 25%

# Removals

## SmallCircle (SC) mod

The Small Circle (SC) mod has been removed in favor of force CS. Existing scores with the Small Circle mod will be
converted to use force CS.

Players can reflect the Small Circle mod by using force CS with +4 CS from the original CS. The score multiplier of
force CS is designed so that such setting gives the same score multiplier as the Small Circle mod.

# Bug fixes

- Fixed a potential crash when countdown sounds are missing
- Fixed speed pp length bonus not taking all objects into account
- Fixed circle-only accuracy calculation in accuracy pp being completely off
- Fixed slider hint width not being affected by CS
- Fixed object opacity calculation in difficulty calculation not considering AR>10 fade in duration
- Fixed object opacity calculation in difficulty calculation not considering Hidden fade in duration
- Fixed modern spinners not recovering HP at all
- Fixed beatmap difficulty calculation cache completely not working
- Fixed circle fade in duration being slower at speed multiplier below 1x and faster at speed multiplier above 1x
