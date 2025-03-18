Version 1.8.3
=============

# Additions

- Added snaking out sliders setting <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Added gameplay HUD editor <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I) and [Rian8337](https://github.com/Rian8337)</span> 
  - For the time being, the editor only allows you to resize and rescale new/existing elements. More elements and
    operations will be added in the future (divide and conquer, as they say).
  - The HUD settings are saved in `skin.json` under the `HUD` property.
- Added hit judgement result specific colors to hit error meter indicators <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Added vibration feedback on hitobject hit <span style="font-size: 0.75em">by [Acivev](https://github.com/Acivev)</span>
  - This can be managed under the `Input` category in options
- Added Traceable (TC) mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Only shows approach circles, slider body borders, slider ticks, and slider repeat arrows
  - Incompatible with the Hidden (HD) mod
  - Current score multiplier is 1.06
  - Impacts star rating (and therefore performance points) for osu!droid star rating system and performance points for
    osu!standard star rating system
  - Mod icon can be skinned with `selection-mod-traceable`

# Changes

- Optimize cursor animation performance during gameplay <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
- Hide beatmap count of default beatmap collection <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Cycle between score online beatmap leaderboard - pp online beatmap leaderboard - local beatmap leaderboard in
  leaderboard switcher button <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Replace the Precise (PR) default mod icon <span style="font-size: 0.75em">by [Acivev](https://github.com/Acivev)</span>
- Optimize difficulty calculation by only calculating necessary skills <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Improve hit error meter readability <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
  - The hit error area is now slightly transparent to give a better visibility for hit error indicators.
- Stretch game surface to fullscreen <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
  - This is an attempt to fix an (abnormally) large blank area in some devices. This area restriction is provided by
    Android and is called the **cutout area**.
  - This means that the game will **also render in the display's cutout area (i.e., front camera notches)**. This is
    intended for now and is aimed to be improved in later versions.
- Bump difficulty calculator version <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Triggers difficulty calculation across all beatmaps to apply the most recent update and bug fixes towards the
    algorithm.

# Bug Fixes

- Fixed beatmaps completely disappearing after selecting the default beatmap collection <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed input dialogs being completely hidden <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
- Fixed another potential crash of video playback not supporting custom speed <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed wallpaper blending into game surface in some devices <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
- Fixed periodic banner swapping breaking after clicking on the banner <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
- Fixed osu!standard slider tail generation using legacy last tick rather than actual slider tail <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Affects osu!standard difficulty calculation.
- Fixed classical spinners being able to be spun at 3/4th preempt time <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed faulty rhythm grouping comparison in rhythm difficulty calculation <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Affects both osu!droid and osu!standard difficulty calculation.
- Fixed slider head judgement in replays set before version 1.8 if the 50 hit window is shorter than the slider's
  duration <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>

# Removals

- Removed the following settings as they have been merged with the HUD editor: <span style="font-size: 0.75em">(by [Reco1I](https://github.com/Reco1I))</span>
  - Progress indicator type
  - Hit error meter
  - Show average offset
  - Show unstable rate
  - Display real-time PP counter
- Removed the `Hide navigation bar` setting <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
  - This is now enabled by default. 
- Removed the `Global beatmap leaderboard scoring mode` setting <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - This has been moved to the leaderboard switcher button.