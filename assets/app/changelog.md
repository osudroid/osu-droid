Version 1.8.2
=============

# Additions

- Added support for cyclic online banners <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span>
- Added support to change maximum players in multiplayer rooms <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Added timestamp to chat messages in multiplayer <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Added slider tick fade in, hit, and fade out (with the Hidden mod) animations <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>

# Changes

- Make unranked text in gameplay not persist permanently <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Matched slider snaking in speed with osu!stable <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Updated osu!droid and osu!standard difficulty and performance calculations to match upcoming changes <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Only render slider path points that are further apart by a certain distance <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Improves memory usage and performance during gameplay, especially in complex Bézier sliders, without
    sacrificing important details.

# Bug Fixes

- Fixed a potential crash during game over animation when video background is enabled <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span> 
- Fixed Hard Rock mod application doubly-flipping slider head and tail positions <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
  - Gameplay was not affected, only difficulty calculation and replay-related detections.
- Fixed incorrect replay download when downloading replays from the best pp leaderboard <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed in-game updater no longer working if the last downloaded update failed to install <span style="font-size: 0.75em">by [Reco1I](https://github.com/Reco1I)</span> 
- Fixed slider ticks potentially being judged incorrectly in replay <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed ScoreV2 combo portion calculation applying `n+1` combo rather than `n` combo <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed slider repeats not generating when slider tick generation is disabled <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed negative duration spinners persisting indefinitely <span style="font-size: 0.75em">by [Rian8337](https://github.com/Rian8337)</span>
- Fixed wrong download speed unit in beatmap downloader <span style="font-size: 0.75em">by [SweetIceLolly](https://github.com/SweetIceLolly)</span>