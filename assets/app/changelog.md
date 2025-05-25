Version 1.8.4
=============

# Additions

- Add mod preset <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
  - Can be used to save and quickly switch between different mod configurations.
- Add ability to customize redesigned user interfaces' theme via `skin.json` <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
- Add Wind Down mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Slows the game down as gameplay progresses.
  - Initial and final speed can be configured in the mod menu.
  - Mod icon is skinnable with `selection-mod-winddown`.
- Add Wind Up mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Speeds the game up as gameplay progresses.
  - Initial and final speed can be configured in the mod menu.
  - Mod icon is skinnable with `selection-mod-windup`.
- Add Mirror mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Flips hit objects vertically or horizontally.
  - Can be configured in the mod menu. 
  - Mod icon is skinnable with `selection-mod-mirror`.
- Add Synesthesia mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Colors hit objects based on their rhythm.
  - Mod icon is skinnable with `selection-mod-synesthesia`.
- Add Random mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Randomizes the position of hit objects.
  - A custom seed can be provided in the mod menu to ensure the same randomization in the same beatmap.
  - Angle sharpness of jumps can be configured in the mod menu.
  - Mod icon is skinnable with `selection-mod-random`.
- Add Muted mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Gradually adjusts the volume of the music (and optionally hitsounds) as gameplay progresses.
  - Can be configured to start from full volume or silence, or to add a metronome beat for assistance.
  - Mod icon is skinnable with `selection-mod-muted`.
- Add Freeze Frame mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Advances the approach rate of hit objects that are in the same combo.
  - Mod icon is skinnable with `selection-mod-freezeframe`.
- Add Approach Different mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Changes the way approach circles shrinks towards hit objects.
  - Available changes are initial scale and rate of shrink.
  - Mod icon is skinnable with `selection-mod-approachdifferent`.
- Add "only fade approach circles" setting to Hidden mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - This setting only fades the approach circles of hit objects and nothing else.
  - Enabling this setting will set the mod's score multiplier to 1x and mark scores as unranked.

# Changes

- Rework mod storage system <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I) and [Rian8337](https://github.com/Rian8337)</span>
  - Allows for more mods and more expansive mod capabilities, some of them have been demonstrated via the new mods that
    have been added alongside this release!
- Redesign mod menu user interface <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I) and [Rian8337](https://github.com/Rian8337)</span>
  - With the addition of many more mods, the current mod menu became too cluttered and could not handle them. As such,
    it has been redesigned to also account for the overall theme of other interfaces.
- Replace speed modify with Custom Speed mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - As a part of the mod storage system rework, speed modify is now considered an actual mod.
  - Mod icon is skinnable with `selection-mod-customspeed`.
- Replace forced difficulty statistics with Difficulty Adjust mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - As a part of the mod storage system rework, forced difficulty statistics are now considered an actual mod.
  - Mod icon is skinnable with `selection-mod-difficultyadjust`.
- Redesign gameplay loading screen <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
  - Now displays epilepsy warning. 
  - Now includes settings that were removed from the current mod menu, plus additional ones such as beatmap-specific
    offset.
- Unify hit object scaling equation <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Previously, the scaling of hit objects was inconsistent as it was influenced by the height of the device. This means
    that under osu!pixels measurement, a hit object may have had a bigger size in some devices compared to others.
  - Now, the scaling of hit objects is consistent across all devices with respect to osu!pixels measurement.
  - **This means that circle sizes change in all devices. However, the change has been made to match the new circle sizes
    to the current ones as close as possible**.
  - To ensure proper playback and difficulty calculation of replays to this date, a new system mod called "Replay V6"
    has been added to restore the old scaling equation. This mod cannot be picked by players and is only available for
    replays.
- Partially match stacking behavior with osu!stable <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - The previous stacking behavior was wrong for a multitude of reasons, namely:
    - It does not stack sliders
    - It is based on the delta time of two objects instead of approach rate, the factor that actually affects
      readability
    - Stacking can still apply even when two objects are not in the same position
  - More importantly, or perhaps the primary driving factor of this change, is because stack offset calculation depends
    on the device's screen resolution.
  - These have now been fixed.
  - Like the previous change, the new Replay V6 mod restores the old stacking behavior.
- Bump osu!droid skill multipliers <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Increases star rating and performance points of all beatmaps to account for hit object stacking and scale changes.
- Exclude some accessibility services from accessibility detection <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Some system services could not be disabled, but they would prevent the player from playing.
  - Current excluded services are:
    - `com.android.systemui`
    - `com.miui.voiceassist`
- Ensure that HUD editor only uses Autoplay mod <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span> 

# Bug Fixes

- Fix a potential crash when stopping hitsounds during pausing <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fix gameplay space not being clipped <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Now, hit object parts that are outside the gameplay space would not be rendered.
- Fix circle size conversion in Easy, Really Easy, and Hard Rock mod using wrong scale unit in difficulty calculation <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Affects the performance points of scores with these mods applied.
- Fix gameplay time potentially being behind audio time until gameplay is restarted or finished <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fix gameplay loading screen being scaled incorrectly when playfield size settings are used <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fix Autoplay mod being persisted after quitting from HUD editor <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>