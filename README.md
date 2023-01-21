<p align="center">
    <img width="300" src="/assets/logo.png">
</p>

# rimu!

[![Android CI](https://github.com/osudroid/osu-droid/workflows/Android%20CI/badge.svg?branch=master)](https://github.com/osudroid/osu-droid/actions?query=workflow%3A"Android+CI")
[![CodeFactor](https://www.codefactor.io/repository/github/osudroid/osu-droid/badge)](https://www.codefactor.io/repository/github/osudroid/osu-droid)
[![Official International Discord](https://discordapp.com/api/guilds/316545691545501706/widget.png?style=shield)](https://discord.gg/nyD92cE)

**rimu!** is a free-to-play rhythm game for Android devices based in the PC rhythm game made by peppy [osu!](https://github.com/ppy/osu)  

This project is the next osu!droid update to come up with a new user interface and new features!

## Status

This project still under development in a unstable state, and it's intended to replace current client in the future, of course we're currently accepting feedback and bug reports.

You can try it by downloading and installing the latest build available
in [releases](https://github.com/reco1I/rimu/releases) section.

## Requirements

rimu! will come with compatibility changes, some older Android devices were compatible with osu!droid will no longer be compatible with rimu!

|           Minimum            |           Target            |
|:----------------------------:|:---------------------------:|
| Android 7.0 Nougat or higher | Android 9.0 Pie or higher * |

_Android 9 Pie or higher is recommended due to native Skia library support which UI heavily relies on to draw complex effects._

## Support

You can support the project by donating to our official [Patreon](https://www.patreon.com/osudroid) and become a rimu! supporter with exclusive in-game and website features.

## Development

If you have knowledge about programming with Java or Kotlin you can contribute to the project creating a pull request.

### Prerequesites:

* We recommend to use [Android Studio](https://developer.android.com/studio) or [Intellij IDEA](https://www.jetbrains.com/idea/) as IDE.
* Java Development Kit 8 or 11 installed on your system (if you're using some of the IDEs listed above it will be installed automatically).

### Downloading the source code

Clone the repository:

```shell
git clone https://github.com/osudroid/osu-droid.git
```

To update the source code to the latest commit, run the following command inside the `rimu` directory:

```she
git pull
```

### Building

Build your own rimu! APK to test your changes or test latest commit in the repository, make sure you meets the system requirements listed above.

* If you're using Android Studio or Intellij IDEA you can directly build rimu! using `Build` feature or you can build and run in an AVD emulator using `Run` feature.

* If you're in Linux you can use the terminal to build rimu! running the command `chmod +x gradlew` and `./gradlew assembleDebug` at the rimu! source root.

_Keep in mind that debug builds aren't capable of online features!_

## License

**rimu!** is licensed under the [Apache License 2.0](https://opensource.org/licenses/Apache-2.0). Please see the licence
file for more information.
