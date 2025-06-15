![osudroid](https://github.com/osudroid/osu-droid/assets/52914632/8747ab50-c77d-4808-91aa-5058891e4bcd)

# osu-droid
[![Official International Discord](https://discordapp.com/api/guilds/316545691545501706/widget.png?style=shield)](https://discord.gg/nyD92cE)
[![Android](https://github.com/osudroid/osu-droid/workflows/Android/badge.svg?branch=master)](https://github.com/osudroid/osu-droid/actions?query=workflow%3A"Android")
[![CodeFactor](https://www.codefactor.io/repository/github/osudroid/osu-droid/badge)](https://www.codefactor.io/repository/github/osudroid/osu-droid)

osu!droid is a free-to-play circle clicking rhythm game for Android devices. It was a game hatched many years ago by the
[osu!](https://osu.ppy.sh/home) community. This project is now being developed by a small group of fans and also with the help of foreign friends.

## Status

osu!droid is under constant development. Features are constantly being added and bugs are constantly being fixed, but
it's playable and fun!

### Downloading the source code

Clone the repository:

```sh
git clone https://github.com/osudroid/osu-droid.git
```
Open the folder in Android Studio.

To update the source code to the latest commit, run the following command inside the `osu-droid` directory:

```she
git pull
```

### Building

osu!droid requires Java 17 to build.

In Android Studio, you can `Build` a debug release to test your changes. The output directory of your `.apk` is inside
`build/output` of `osu-droid`'s directory.

If you prefer the command line, and you are on Linux, run `chmod +x gradlew` and `./gradlew assembleDebug` inside the
directory to build the debug `.apk` files.

## Contributing

We welcome any sort of contributions, as long as they're helpful. Those who aren't able to contribute code may instead
suggest small changes like grammar fixes or report client issues via [Feature request](https://github.com/osudroid/osu-droid/issues/11) or [GitHub issues](https://github.com/osudroid/osu-droid/issues).

## License

osu!droid is licensed under the [Apache-2.0](https://opensource.org/licenses/Apache-2.0) License. Please see the LICENSE file for more information.
