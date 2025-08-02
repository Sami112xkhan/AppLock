# AppLock - Secure Your Apps with Biometric Authentication

A modern Android app locker that protects your applications with biometric authentication (fingerprint, face unlock, or device PIN).

## 🚀 Features

- **🔐 Biometric Authentication**: Secure your apps with fingerprint, face unlock, or device PIN
- **📱 App Locking**: Lock any installed app on your device
- **⚡ Fast & Lightweight**: Optimized performance with minimal battery usage
- **🎨 Modern UI**: Beautiful Material 3 design with smooth animations
- **🔍 Smart Search**: Quickly find apps with intelligent search functionality
- **⚙️ Customizable Settings**: Tailor the app to your preferences
- **🛡️ Anti-Uninstall Protection**: Prevent unauthorized app removal (optional)

## 📸 Screenshots

[Add screenshots here when available]

## 🛠️ Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Package Name**: `com.samikhan.applock`

## 🔧 Installation

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 26+
- Kotlin 1.8+

### Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/Sami112xkhan/AppLock.git
   cd AppLock
   ```

2. Open the project in Android Studio

3. Sync Gradle files and build the project:
   ```bash
   ./gradlew assembleDebug
   ```

4. Install on your device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📱 Usage

1. **Initial Setup**:
   - Open the app and set a PIN
   - Enable biometric authentication in settings
   - Grant necessary permissions (Accessibility Service, Usage Stats)

2. **Locking Apps**:
   - Browse the app list
   - Toggle the switch next to apps you want to lock
   - Use search to quickly find specific apps

3. **Accessing Locked Apps**:
   - When you try to open a locked app, the lock screen appears
   - Use biometric authentication or PIN to unlock
   - Apps remain unlocked for a configurable duration

## 🔐 Permissions

- **Accessibility Service**: Required for app monitoring
- **Usage Stats**: Alternative backend for app detection
- **System Alert Window**: For lock screen overlay
- **Device Admin**: For anti-uninstall protection (optional)

## 🏗️ Project Structure

```
app/src/main/java/com/samikhan/applock/
├── core/
│   ├── biometric/          # Biometric authentication
│   ├── broadcast/          # Broadcast receivers
│   ├── monitoring/         # App monitoring services
│   ├── navigation/         # Navigation components
│   ├── ui/                 # Common UI components
│   └── utils/              # Utility functions
├── data/
│   └── repository/         # Data layer
├── features/
│   ├── admin/              # Device admin features
│   ├── appintro/           # App introduction
│   ├── applist/            # Main app list
│   ├── lockscreen/         # Lock screen UI
│   ├── setpassword/        # Password setup
│   └── settings/           # App settings
├── services/               # Background services
└── ui/                     # UI components and icons
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Original project by Pranav
- Material 3 Design System
- Jetpack Compose team
- Android Biometric API

## 📞 Support

- **Developer**: Sami Khan
- **GitHub**: [@Sami112xkhan](https://github.com/Sami112xkhan)
- **Donation**: [PayPal](https://paypal.me/SamiKhan112)

## 🔄 Changelog

### Version 1.0.0
- Initial release
- Biometric authentication support
- App locking functionality
- Modern Material 3 UI
- Custom branding and icons

---

**Note**: This is a customized version of the original AppLock project with enhanced biometric authentication and modern UI improvements.
