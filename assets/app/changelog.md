Version 1.8.2
=============

# Additions

- Added support for cyclic online banners
- Added support to change maximum players in multiplayer rooms
- Added timestamp to chat messages in multiplayer
- Added slider tick fade in, hit, and fade out (with the Hidden mod) animations

# Changes

- The unranked text in gameplay no longer persists permanently
- Matched slider snaking in speed with osu!stable
- Updated osu!droid and osu!standard difficulty and performance calculations to match upcoming changes
- Only render slider path points that are further apart by a certain distance 
  - Improves memory usage and performance during gameplay, especially in complex Bézier sliders, without
    sacrificing important details.

# Bug Fixes

- Fixed a potential crash during game over animation when video background is enabled
- Fixed Hard Rock mod application doubly-flipping slider head and tail positions
  - Gameplay was not affected, only difficulty calculation and replay-related detections.
- Fixed incorrect replay download when downloading replays from the best pp leaderboard
- Fixed in-game updater no longer working if the last downloaded update failed to install
- Fixed slider ticks potentially being judged incorrectly in replay
- Fixed ScoreV2 combo portion calculation applying `n+1` combo rather than `n` combo
- Fixed slider repeats not generating when slider tick generation is disabled