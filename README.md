# Next Caltrain Kotlin

[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fwoodie%2Fnext-caltrain-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=7F52FF&logo=kotlin&logoColor=white)](gradle/libs.versions.toml)
[![CI](https://github.com/woodie/next-caltrain-kotlin/actions/workflows/CI.yml/badge.svg)](https://github.com/woodie/next-caltrain-kotlin/actions/workflows/CI.yml)
[![Release](https://img.shields.io/github/v/release/woodie/next-caltrain-kotlin.svg)](https://github.com/woodie/next-caltrain-kotlin/releases/latest)
[![License](https://img.shields.io/github/license/woodie/next-caltrain-kotlin.svg)](LICENSE)

Live countdown to your next Caltrain, with automatic schedule detection.

<img width="25%" 
  src="https://github.com/woodie/next-caltrain-kotlin/blob/main/pics/HomeScreen.png">
<img width="25%"
  src="https://github.com/woodie/next-caltrain-kotlin/blob/main/pics/TripListScreen.png">
<img width="25%"
  src="https://github.com/woodie/next-caltrain-kotlin/blob/main/pics/TripDetailScreen.png">

## Features

- **A live countdown** to the next train from your station, updating every
  second.
- **The right schedule, automatically:** weekday, weekend or holiday, and late
  at night, tomorrow's first trains.
- **Your commute both ways:** pick your two stations once, and after noon the
  app reverses them for the trip home.
- **Every service:** Local, Limited, Express, and South County trains to
  Gilroy, including the transfer at San Jose Diridon.
- **Stop-by-stop details** for any train.
- **Works offline:** the schedule is saved on your device and refreshed once
  a day, so timetable changes arrive without an app update.
- **Private:** no account, analytics or ads. Your stations and the schedule
  stay on your device.

## Using the app

| To | Do this |
|---|---|
| Change stations | Tap the station names |
| Reverse the direction | Tap the swap button |
| See the next few trains | Tap the countdown circle |
| Step to an earlier or later train | Drag up or down on the circle or the list |
| Jump back to the next train | Tap the reset button (shown after you step away) |
| See a train's stops | Tap the train in the list |
| Look at another day's schedule | Tap the schedule line above the list |
| See how current the schedule is | Tap the "Next Caltrain" title |

## Development

Building, testing and releasing are covered in
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

A port of the apps for
[KaiOS](https://github.com/woodie/next-caltrain-pwa) and
[J2ME](https://github.com/woodie/next-caltrain-j2me).
