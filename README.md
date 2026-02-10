# 🎾 Padel Tennis Scorer

<p align="center">
  <strong>Professional NFC-Based Padel Tennis Scoring System for Android</strong>
</p>

## 📱 Overview

Padel Tennis Scorer is a modern Android application designed for professional padel tennis match tracking. Using NFC technology, players can automatically score points by scanning their assigned NFC tags, creating a seamless and hands-free scoring experience.

## ✨ Features

### 🏆 Core Functionality
- **NFC Tag Scoring**: Automatic point registration via NFC tag scanning
- **Manual Point Entry**: Manual point buttons for each team
- **Real-Time Score Display**: Large, easy-to-read score cards showing Sets, Games, and Points
- **Tennis Scoring Logic**: Complete implementation of tennis/padel scoring rules
  - Regular point progression (0, 15, 30, 40, Game)
  - Deuce and Advantage handling
  - Golden Point mode option
  - Tiebreak support (7 points with 2-point lead)
- **Match Configuration**: Customizable best-of-3 or best-of-5 sets

### 🎨 User Interface
- **Dual-Team Display**: Split-screen design with distinctive team colors (Red/Teal)
- **Fullscreen Mode**: Immersive landscape orientation optimized for visibility
- **Adjustable Font Size**: Slider control (10-100sp) for optimal readability
- **Responsive Design**: Clean, modern Material Design interface

### ⚙️ Advanced Features
- **Undo/Redo System**: Multi-level undo (up to 10 steps) for score corrections
- **Match History**: Persistent storage of completed matches with full statistics
- **Player Management**: Tag assignment and player tracking
- **Duplicate Scan Protection**: 5-second timeout to prevent accidental double-scoring
- **Database Integration**: Room database with Supabase sync capabilities

### 📊 Match Statistics
- Match duration tracking
- Total points, games, and sets per team
- Player-specific statistics and win records
- Historical match database

## 🚀 Technical Stack

- **Language**: Java
- **Minimum SDK**: Android 21 (Lollipop)
- **Target SDK**: Android 35
- **Architecture**: MVP (Model-View-Presenter)
- **Database**: Room + Supabase
- **NFC**: Mifare Classic support via native libraries

## 🔧 Build Information

- **Version**: 1.0.4
- **Version Code**: 302
- **Package**: com.padeltennis.scorer

## 📥 Installation

### Requirements
- Android device with NFC capability
- Android 5.0 (Lollipop) or higher
- NFC-enabled Mifare Classic tags

### Download
Download the latest APK/AAB from the [Releases](https://github.com/marion909/Padel-Tennis/releases) page.

## 🎮 How to Use

1. **Setup Match**
   - Configure number of sets (Best of 3/5)
   - Enable/disable Golden Point mode
   - Assign teams and players

2. **Tag Assignment**
   - Assign NFC tags to each player
   - Tags are read from Mifare Classic blocks 5-6

3. **Score During Match**
   - Scan your NFC tag after winning a point
   - Or use the manual (+) button under your team
   - Adjust font size via "Aa" button
   - Use undo (↺) button to correct mistakes

4. **Match Completion**
   - View match results
   - Review statistics
   - Match is automatically saved to database

## 🎯 Scoring Rules

### Regular Game
- 0 → 15 → 30 → 40 → Game
- At deuce (40-40):
  - **Golden Point**: Next point wins the game
  - **Advantage**: Traditional advantage scoring

### Set Win
- First to 6 games with 2-game lead
- At 6-6: Tiebreak to 7 points (2-point lead required)

### Match Win
- Best of 3 sets: First to 2 sets
- Best of 5 sets: First to 3 sets

## 🔐 Security

The app includes built-in keystore signing for release builds. Credentials are managed securely and not exposed in the public repository.

## 📝 Recent Updates (v1.0.4)

- ✅ Added adjustable font size slider (10-100sp)
- ✅ Implemented manual point buttons for each team
- ✅ Added multi-level undo functionality (up to 10 steps)
- ✅ Improved UI/UX with better button placement
- ✅ Enhanced score display responsiveness

## 🛠️ Development

### Building from Source

```bash
git clone https://github.com/marion909/Padel-Tennis.git
cd Padel-Tennis
./gradlew assembleRelease
```

### Building AAB for Google Play

```bash
./gradlew bundleRelease
```

The signed AAB will be located in:
```
apprts/build/outputs/bundle/release/apprts-release.aab
```

### Project Structure

```
Padel-Tennis/
├── apprts/                    # Main application module
│   ├── src/main/
│   │   ├── java/              # Java source files
│   │   │   └── com/rfidresearchgroup/activities/main/
│   │   │       ├── PadelScoreActivity.java
│   │   │       ├── PadelConfigActivity.java
│   │   │       └── PadelTagAssignmentActivity.java
│   │   ├── res/               # Android resources
│   │   │   ├── layout/        # XML layouts
│   │   │   └── drawable/      # Graphics & icons
│   │   └── AndroidManifest.xml
│   └── build.gradle           # App-level build configuration
├── build.gradle               # Project-level build configuration
└── README.md                  # This file
```

## 🧩 Key Components

### Activities
- **MainMenuActivity**: Entry point and main navigation
- **PadelConfigActivity**: Match configuration (sets, golden point)
- **PadelTagAssignmentActivity**: Player and NFC tag assignment
- **PadelScoreActivity**: Live match scoring and display
- **MatchResultActivity**: Post-match results and statistics

### Database Entities
- **MatchEntity**: Complete match records
- **PlayerEntity**: Player profiles and statistics
- **MatchPlayerEntity**: Player-match relationships

### Native Libraries
- **libnfc**: NFC communication
- **libpm3**: Proxmark3 integration
- **libcrapto1**: Cryptography utilities

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Connect an NFC-enabled Android device
5. Run the app

## 📄 License

This project is based on RFID Tools and includes modifications for padel tennis scoring.

Original components copyright © 2019 RFID Research Group
Padel Tennis modifications © 2026 Marion

Licensed under GPL v3.0

## 🙏 Credits

### Dependencies
- **Terminal**: [Termux](https://github.com/termux) - Terminal emulator components
- **Database**: Room + Supabase - Data persistence and sync
- **NFC**: Native RFID libraries - Tag communication

### Based On
This app is built upon the RFID Tools framework by the RFID Research Group, adapted specifically for padel tennis match scoring.

## 📞 Contact & Support

For issues, feature requests, or questions:
- Open an issue on [GitHub](https://github.com/marion909/Padel-Tennis/issues)
- Email: support@padeltennis.scorer

## 🗺️ Roadmap

### Planned Features
- [ ] Bluetooth device support
- [ ] Match export to PDF/CSV
- [ ] Tournament mode with brackets
- [ ] Video replay integration
- [ ] Multi-language support (English, German, Spanish)
- [ ] Cloud match sharing
- [ ] Player rankings and leaderboards

## 🎯 Version History

### v1.0.4 (Current)
- Added adjustable font size slider
- Implemented manual point buttons
- Added multi-level undo functionality
- UI improvements

### v1.0.3
- Initial NFC scoring implementation
- Database integration
- Match history tracking

---

**Made with ❤️ for Padel Tennis enthusiasts**


