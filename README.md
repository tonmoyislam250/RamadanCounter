# Roza Rayan (Ramadan Counter 2026)

Roza Rayan is a modern, lightweight Android application designed to help users track Ramadan 2026 (1447 AH) timings across all 64 districts of Bangladesh.

## Features

- **Real-time Countdown**: Live countdown to the next Sehri or Iftar event.
- **District-wise Timings**: Supports all 8 divisions and 64 districts of Bangladesh.
- **Islamic Day Logic**: Automatically switches to the next day's schedule after Iftar.
- **Telegram-style UI**: Clean, modern interface inspired by Telegram's design.
- **Dark Mode Support**: Full support for system dark theme with a deep blue palette.
- **Edge-to-Edge Display**: Modern UI that flows behind the status and navigation bars.
- **Official Data**: Timings based on the Islamic Foundation Bangladesh's official schedule.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: XML Layouts (ConstraintLayout, Material Components)
- **Architecture**: Simple Activity-based with dynamic CSV parsing.
- **Libraries**:
    - `androidx.core:core-splashscreen` for modern startup transitions.
    - `com.google.android.material:material` for high-quality UI components.

## How It Works

The app reads division-specific CSV files from the `assets` folder. When a user selects their Division and District, the app parses the relevant data, calculates the remaining time based on the system clock, and updates the UI every second. It intelligently handles the transition between Sehri and Iftar, as well as the transition to the next day's Sehri after Iftar is complete.

## Data Source

All timings are based on the Islamic Foundation Bangladesh's official schedule for Ramadan 1447 AH / 2026 AD.

---
*Note: Dates are subject to moon sighting and may vary by 1 day.*
