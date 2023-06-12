Changelog for 1.6.8 Hotfix (June 12, 2023)
===================
## Additions:

- `skin.ini` converter
  - Converts a `skin.ini` file from osu!stable skins and saves it as `skin.json` inside the respective skin folder.
- New options for `skin.json`:
  - `hitCirclePrefix`: Denotes the prefix of texture files used in circle numbers. The default value is `default`. In this case, the game will pick up `default-0`, `default-1`, `default-2`, and so on for circle number textures.
  - `scorePrefix`: Denotes the prefix of texture files used in the score and accuracy counter. The default value is `score`. In this case, the game will pick up `score-0`, `score-1`, `score-2`, and so on for score and accuracy counter number textures.
  - `comboPrefix`: Denotes the prefix of texture files used in the combo counter. The default value is `combo`. In this case, the game will pick up `combo-0`, `combo-1`, `combo-2`, and so on for combo counter number textures.
  - The JSON format:
  ```json
      "Fonts": {
          "hitCirclePrefix": "default",
          "scorePrefix": "score",
          "comboPrefix": "combo"
      }
  ```

## Changes:

- Revamped online avatar retrieval
  - Now you do not need to clear application cache anymore after you change your avatar!
- Several changes towards skin imports
  - A 1 pixel transparent texture will be used for `sliderendcircle` and/or `sliderendcircleoverlay` if they are not present in an imported skin. 
  - Fully transparent or 1 pixel `selection-mods`, `selection-random`, or `selection-options` textures will be replaced by their default textures respectively, otherwise the buttons will become unpressable or invisible.

## Bug fixes:

- Fixed a bug where the game fails to launch if the login password field is empty
- Fixed a bug where beatmap importing process quits if it fails to import a beatmap instead of continuing to process the next beatmap
- Fixed a bug where the follow circle of a slider may remain on screen after the slider finishes
- Fixed a bug where the sorting of global leaderboard is reverted
- Fixed a bug where the Sudden Death mod does not fail immediately if the Easy mod is active
- Fixed a bug where the Flashlight dim area covers the hit error meter and song progress bar
- Fixed a bug where game configurations may be loaded twice
- Fixed a bug where the Flashlight lightened area may not move if the next object to press is a circle
- Fixed a bug where the color of slider start and end circles follows the slider body's color in kiai mode if the `sliderFollowComboColor` setting in `skin.json` is set to `false`