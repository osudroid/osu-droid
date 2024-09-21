Version 1.8
===========

# Additions

## User interface redesigns

Many of menu-related user interfaces in the game have been redesigned to bring a refresh to old user interfaces.
The following menus have been redesigned:

- Options menu
- Mod customization menu 9that pops up when "Settings" in the mod menu)
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
- Added a new button in song selection menu to switch difficulty algorithm between osu!droid and osu!standard
- Added support for `sliderslide` and `sliderwhistle` hitsounds
- Added support for custom file hitsounds
- Added an animation to slider end arrow rotation when snaking animation is enabled
- Added a slight dim to hitobjects that cannot be hit yet in gameplay
- Added a rotation effect to miss hit judgement effects in gameplay

# Changes

## Storage migration

This updates migrates the storage location of local scores, beatmap collections, and beatmap options into an integrated
database. Doing this increases the import time of beatmaps and fixes the problem of duplicated beatmapsets in song
selection menu.

## Background difficulty calculation

This update moves the difficulty calculation process of beatmaps that would normally be done during import into a
background process. This significantly improves the import time of beatmaps. As consequences, the star rating sorting in
song select menu will not work properly while the background process is running, and all beatmaps that have not been
calculated will display a star rating of 0.

During gameplay, background difficulty calculation is paused to prevent performance degradation.

## Smaller changes

- Circles and sliders can now be hit as early as 400ms before the circle's start time
  - Previously, circles could immediately be hit after the approach circle has progressed halfway.
  - Consequently, sliders could be hit only after its slider head enters the meh hit window, preventing the player from
    slider breaking for hitting a slider's head too early
- More significant performance improvements in gameplay than version 1.7.2
- Separated average offset and unstable rate displays in gameplay into separate settings
- Changed hit lighting effect animation to match osu!stable
- Changed combo counter animation to match osu!stable
- Miss hit judgement effect only plays in a slider's tail rather than its head and tail

# Removals

- Removed the "Calculate slider path" setting. This setting is now enabled by default thanks to the aforementioned
performance improvements
- Removed internal volume adjustment of normal, whistle, and clap hitsounds, where normal and whistle hitsounds' volume
were reduced by 20% and clap hitsounds' volume were reduced by 15%

# Bug fixes

- Fixed login fail messages other than "Cannot connect to server" not displaying properly
- Fixed "Show FPS" setting affecting the display of other counters (average offset, unstable rate, and real-time PP counter)
- Fixed wrong textures being displayed before in-game video plays on devices with Mali GPUs
- Fixed real-time pp counter taking the next object's difficulty attributes when the current object is active in gameplay
- Fixed real-time pp counter text potentially getting cut off
- Fixed some gameplay animations not scaling on speed multiplier
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