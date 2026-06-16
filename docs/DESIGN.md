# Next Caltrain — Design Document

This document captures the architecture, conventions, and lessons learned from
building the PWA (`next-caltrain-pwa`) and iOS (`next-caltrain-swift`) versions
of Next Caltrain. The Kotlin/Android app should follow these same patterns.

---

## Project lineage

The app originated as a J2ME app (~2018), was rewritten as a KaiOS PWA
(JavaScript), and then ported to iOS (SwiftUI). The core logic — `GoodTimes`,
`CaltrainSchedule`, `CaltrainService` — has been faithfully carried across all
three platforms. The Kotlin app is the fourth implementation.

---

## Schedule data

### Source

Schedule data is published as `schedule.json` at:

```
https://next-caltrain-pwa.appspot.com/schedule.json
```

This file is shared by all clients (PWA, iOS, Android). It is generated from
Caltrain GTFS CSVs by `tools/convert_schedule.py` in the iOS repo, then
deployed from the PWA repo via `npm run deploy`.

### Format

```json
{
  "specialDates": { "2026-01-19": 2 },
  "northStops": ["Gilroy", ..., "San Francisco"],
  "southStops": ["San Francisco", ..., "Gilroy"],
  "northWeekday": { "101": [null, null, 277, ...] },
  "northWeekend": { ... },
  "northHoliday": { ... },
  "southWeekday": { ... },
  "southWeekend": { ... },
  "southHoliday": { ... },
  "scheduleDate": 1781137864000
}
```

- Times are **minutes since midnight** (e.g. 5:52am = 352).
- Times past midnight use values > 1440 (e.g. 24:05 = 1445).
- Missing stops are `null`.
- `scheduleDate` is epoch milliseconds — the mtime of the newest source CSV.
- `specialDates` maps date strings to schedule type (0=Weekday, 1=Weekend, 2=Holiday).
- `northStops` and `southStops` are mirror-ordered (north is south reversed).
- Train arrays are indexed by stop position; sparse arrays are normal.

### Caching

On first launch with no cache and a failed fetch, show a loading/error screen.
With a valid cache, race the network fetch against a timeout (10s); whichever
resolves first wins. Cache valid responses to disk for next launch.

---

## GoodTimes

The central time utility. Key design decision: **subtract 2 hours from the
real clock** so that trains running past midnight (e.g. 24:05 = 1445 minutes)
still appear on "today's" schedule rather than rolling to tomorrow at midnight.

### Properties

| Property | Description |
|---|---|
| `date` | Current schedule date as `yyyy-MM-dd` (using rolled-back time) |
| `minutes` | Minutes since midnight (rolled back), used for all comparisons |
| `seconds` | Seconds component, for the countdown timer |
| `dotw` | Day of week (0=Sunday … 6=Saturday), rolled back |
| `tomorrowDate` | Next calendar date as `yyyy-MM-dd` |
| `tomorrowDotw` | `(dotw + 1) % 7` |

### Debug overrides

The iOS and PWA implementations include debug overrides for testing
(`debugOverrideMinutes`, `debugOverrideDotw`). The Kotlin app should do
the same — they are essential for testing schedule rollover and South County
edge cases without waiting for the right time of day.

---

## CaltrainSchedule

Determines which schedule type applies for a given date/dotw.

### Schedule types

| Value | Label | When |
|---|---|---|
| 0 | Weekday | Monday–Friday, not a special date |
| 1 | Weekend | Saturday or Sunday |
| 2 | Holiday | Date in `specialDates` with value 2 |

### Key method: `optionIndexFor(date, dotw)`

Takes a date string and day-of-week integer; returns the schedule type index.
Special dates override dotw-based logic. This is factored out so tomorrow's
schedule type can be computed without a full GoodTimes object.

### Tomorrow's schedule type

```
tomorrowLabel = optionIndexFor(goodTimes.tomorrowDate, goodTimes.tomorrowDotw)
```

This is used when appending tomorrow's trips and when navigating to trip
details for a future (tomorrow) trip.

---

## CaltrainService

### Direction

`direction(from, to)` uses the `southStops` list as the canonical ordering.
If the origin index < destination index, direction is South; otherwise North.

### Train types

| Train ID range | Type |
|---|---|
| 101–400 | Local |
| 401–500 | Limited |
| 501–800 | Express |
| 801–900 | South County (diesel) |
| 901+ | Unknown |

### South County

Stations: Gilroy, San Martin, Morgan Hill, Blossom Hill, Capitol.
Transfer station: San Jose Diridon.

South County trains (801–900) only run between San Jose Diridon and Gilroy.
Electric trains (101–800) only run between San Francisco and San Jose Diridon.

### Transfer logic

A transfer at San Jose Diridon is needed when **exactly one** endpoint is a
South County station and the schedule is Weekday. Weekend/Holiday schedules
have no South County service.

**Southbound (SF → Gilroy):**
- Find all electric trains from origin → SJD (arrive times).
- Find all SC trains from SJD → destination (depart times).
- For each SC train, pair with the **last** electric that arrives SJD ≤ SC depart.
- Result sorted by origin departure time.

**Northbound (Gilroy → SF):**
- Find all SC trains from origin → SJD (arrive times).
- Find all electric trains from SJD → destination (depart times).
- For each SC train, pair with the **first** electric departing SJD ≥ SC arrival.
- Result sorted by origin departure time.

### Route array format

Direct routes: `[trainId, depart, arrive]`
Transfer routes: `[trainId, depart, arrive, transferTrainId, transferDepart]`

---

## Tomorrow trips

After building today's trip list, append tomorrow's trips shifted by
`+1440` minutes so they sort after today's and produce correct countdowns.

Mark these trips with `isFuture = true` so they render with inactive styling
(no countdown, schedule label shown instead).

### South County Friday → Saturday edge case

South County trains only run on weekdays. When today is Friday and tomorrow
is Saturday, `tomorrowScheduleType` is `.weekend`, which has no SC service.
`tomorrowRoutes` is empty, so nothing is appended. Today's full list remains
visible — do not show "NO TRAINS".

When all of today's trips have departed and there are no tomorrow trips,
select the **first** trip of the day (offset = 0) rather than the last
departed one.

### Trip detail for future trips

When navigating to the detail view for a future trip, pass
`tomorrowScheduleType` (not today's `scheduleType`) so the correct schedule
table is used to look up stop times.

---

## Testing conventions

### Fixture builder pattern

Rather than using real schedule data in unit tests, build minimal fixtures
with just enough stations and trains to exercise the logic under test. See
`SpecFixtures.swift` in the iOS repo for the pattern:

- 16 stations: San Francisco (0), San Jose Diridon (7), Morgan Hill (14), Gilroy (15)
- Electric trains run SF ↔ SJD only
- Diesel/SC trains run SJD ↔ Gilroy only
- Builder accepts `.normal`, `.none`, or `.custom` service per schedule type

### Debug overrides for time-sensitive tests

Use `GoodTimes.debugOverrideMinutes` and `GoodTimes.debugOverrideDotw` to
pin the clock and day of week in tests. This makes rollover and South County
edge cases testable without waiting for specific times.

### What to test

- `GoodTimes`: tomorrowDate, tomorrowDotw (including Sat→Sun wrap)
- `CaltrainSchedule`: optionIndexFor (weekday, Saturday, Sunday, special date), tomorrowLabel
- `CaltrainService`: direct routes, transfer routes (both directions), no-service cases
- `TripViewModel`: isFuture flag, tomorrow appending, South County edge case, offset clamping

---

## UI conventions

- **No bold headings** — use `.regular` font weight throughout.
- **2-hour rollback** applies to display times as well as schedule selection.
- **Inactive trips** (past or future/tomorrow): gray/muted styling, no countdown.
- **Transfer rows**: show both train numbers (e.g. `#514 + #814`), with a
  sublabel at the transfer station ("transfer to #814").
- **Status text**: countdown for upcoming trains; schedule label for
  past/swapped/future; "DEPARTING" (blinking) when departing; "NO TRAINS" when empty.

---

## Lessons learned

- **RowWidthKey / unused PreferenceKey**: an unused `GeometryReader` preference
  measuring row width in `TripDetailView` caused a SwiftUI layout feedback loop
  and UI hang. Remove any preference key that isn't actually consumed.

- **UIScreen.main.bounds vs window.bounds**: on iOS 16+, `UIScreen.main.bounds`
  returns portrait dimensions regardless of orientation. Use
  `UIWindowScene → windows.first → bounds` for orientation-aware height.

- **GeometryReader on ZStack**: a `GeometryReader` placed as a ZStack sibling
  of a growing list gets compressed by the list, creating a feedback loop.
  Use `window.bounds.height` to measure available screen height instead.

- **NavigationLink safe-area jump**: pushed views using `.navigationBarHidden(true)`
  can render flush under the status bar on first frame. Match the exact structure
  of a working root view (toolbar HStack as first child of outermost VStack,
  plain `.padding(.top, 8)`) rather than trying `.safeAreaInset` or
  `UIApplication`-derived insets.

- **`isFuture` and schedule type**: when tapping a future (tomorrow) trip to
  view details, pass `tomorrowScheduleType` to the detail view, not
  `scheduleType`. Using today's schedule type produces an empty stop list.

- **`nextIndex` works automatically for tomorrow trips**: since tomorrow's
  shifted times (1440+) are always greater than today's current minutes,
  no special handling is needed in `nextIndex`.

- **Transfers only on Weekday**: the transfer detection guard
  (`scheduleType == .weekday`) is important — weekend/holiday schedules
  have no South County service, so transfer logic would produce empty results
  or incorrect pairings.

- **`saturdayTripIds` was legacy**: Caltrain formerly ran Saturday-only trains
  that didn't run on Sunday. This field appeared in the schedule data for years
  but was never read by any app logic. Removed in 2026.
