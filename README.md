# AURA — AI-based User Restriction, Attention & Attendance

A complete native Android classroom management system with on-device AI, overlay-based attention control, and comprehensive attendance tracking.

## 🚀 Quick Start

### For Students (Android App)
```bash
# Clone and build
git clone https://github.com/officialnj52-creator/Aura_square.git
cd Aura_square

# Open in Android Studio
# Build APK: Build > Build Bundle(s) / APK(s) > Build APK(s)
```

### For Teachers (Setup)
1. **Set up Supabase backend**
2. **Configure hardware hubs (optional)**
3. **Deploy teacher dashboard**

## 📁 Project Structure

```
Aura_square/
├── app/                          # Android application
│   ├── src/main/
│   │   ├── java/com/aura/app/   # Kotlin source code
│   │   │   ├── ai/              # TensorFlow Lite AI classification
│   │   │   ├── data/            # Database and repositories
│   │   │   ├── network/         # Supabase integration
│   │   │   ├── service/         # Background services
│   │   │   ├── ui/              # Jetpack Compose UI
│   │   │   │   ├── student/     # Student-facing screens
│   │   │   │   └── teacher/     # Teacher dashboard
│   │   │   ├── utils/           # Utility functions
│   │   │   └── MainActivity.kt  # Main entry point
│   │   ├── res/                 # Android resources
│   │   └── AndroidManifest.xml  # Permissions and configuration
│   └── build.gradle.kts         # App dependencies
├── supabase/                    # Backend infrastructure
│   ├── migrations/              # PostgreSQL schema
│   │   ├── 001_initial_schema.sql
│   │   └── 002_rls_policies.sql
│   └── functions/               # Edge Functions
│       ├── session/            # Session management
│       ├── finalizeSession/     # Attendance finalization
│       └── uploadEvent/         # Event streaming
└── hardware/                    # Optional classroom hubs
    └── aura_hub/                # NodeMCU/ESP32 firmware
        └── aura_hub.ino         # Arduino sketch
```

## 🛠️ Technology Stack

### Android (Native)
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room with SQLCipher encryption
- **AI**: TensorFlow Lite (on-device classification)
- **Async**: Kotlin Coroutines + Flow

### Backend
- **Database**: Supabase (PostgreSQL)
- **Real-time**: Supabase Realtime subscriptions
- **API**: Supabase Edge Functions (TypeScript)
- **Security**: Row Level Security (RLS) policies

### AI & Computer Vision
- **Framework**: TensorFlow Lite
- **Model**: MobileNetV2 + Custom classification layer
- **Input**: Screenshot bitmap (224x224)
- **Categories**: EDUCATION, MESSAGING, SOCIAL, VIDEO, GAME, UNKNOWN

### Hardware (Optional)
- **Platform**: NodeMCU ESP32
- **Communication**: WiFi + HTTP requests
- **Protocol**: REST API with JSON payloads
- **Features**: Network scanning, proximity detection

## 📋 Features

### Student Experience
- ✅ **AI App Classification** - Real-time app usage monitoring
- ✅ **Class Mode Overlay** - Full-screen attention enforcement
- ✅ **Accessibility Escape Detection** - Automatic overlay restoration
- ✅ **Offline Operation** - Local buffering with automatic sync
- ✅ **Emergency Help System** - Quick teacher notifications
- ✅ **Privacy Protection** - On-device processing, encrypted storage

### Teacher Dashboard
- ✅ **Session Management** - Start/stop classroom sessions
- ✅ **Real-time Monitoring** - Live student attention tracking
- ✅ **Attendance Analytics** - Detailed usage statistics
- ✅ **Emergency Responses** - Handle student help requests
- ✅ **Session Finalization** - Complete attendance records

### System Features
- ✅ **Production Ready** - ProGuard obfuscation, release builds
- ✅ **Security First** - End-to-end encryption, authentication
- ✅ **Scalable Architecture** - Cloud-native with offline support
- ✅ **Hardware Integration** - Optional classroom hub devices

## 🚀 Installation

### Prerequisites
- **Android Studio** Hedgehog 2023.1.1 or newer
- **JDK** 17 or newer
- **Android SDK** API level 24-34
- **Supabase** account (for backend)

### Android App Setup

1. **Clone the repository**
```bash
git clone https://github.com/officialnj52-creator/Aura_square.git
cd Aura_square
```

2. **Open in Android Studio**
   - File > Open > Select `Aura_square` folder
   - Wait for Gradle sync to complete

3. **Configure Supabase**
   - Create `local.properties` file:
     ```properties
     supabase.url=your-supabase-url
     supabase.anonKey=your-anonymous-key
     supabase.serviceKey=your-service-role-key
     ```

4. **Build the app**
   - Build > Build Bundle(s) / APK(s) > Build APK(s)
   - Or use command line: `./gradlew assembleDebug`

5. **Install and test**
   - Transfer APK to Android device
   - Grant required permissions when prompted

### Supabase Backend Setup

1. **Create new project**
   - Go to [supabase.com](https://supabase.com)
   - Create new project

2. **Run database migrations**
   ```bash
   supabase db push
   ```

3. **Deploy Edge Functions**
   ```bash
   supabase functions deploy session
   supabase functions deploy finalizeSession
   supabase functions deploy uploadEvent
   ```

4. **Set environment variables**
   ```bash
   supabase secrets set HUB_SECRET=your-secure-hub-secret
   supabase secrets set DATA_RETENTION_DAYS=90
   ```

### Hardware Setup (Optional)

1. **Install Arduino IDE 2.0+**
2. **Add ESP32 board support**
3. **Open `hardware/aura_hub/aura_hub.ino`**
4. **Configure WiFi credentials**
5. **Upload to NodeMCU ESP32**
6. **Power on in classroom location**

## 🎯 Usage

### For Students
1. **Install the AURA app**
2. **Grant required permissions** (MediaProjection, Accessibility, etc.)
3. **Connect to classroom network** (AURA_XXXXX)
4. **Join Class Mode** when prompted by teacher
5. **Use emergency help** button if needed

### For Teachers
1. **Set up teacher account** in Supabase
2. **Create classroom sessions** through teacher dashboard
3. **Monitor student attention** in real-time
4. **Handle emergency requests** from students
5. **Finalize attendance** at session end

## 🔒 Security & Privacy

- **On-device AI processing** - No screenshots leave the device
- **End-to-end encryption** - All data encrypted in transit and at rest
- **Role-based access control** - Row Level Security in Supabase
- **No data sharing** - Educational data stays private
- **Compliant design** - FERPA and GDPR considerations

## 🛠️ Development

### Building for Production
```bash
# Release build with obfuscation
./gradlew assembleRelease

# Sign with release keystore
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA256 \
  -keystore your-release-key.keystore \
  app/build/outputs/apk/release/app-release.apk \
  your-alias-name
```

### Code Structure
- **Package**: `com.aura.app`
- **Architecture**: MVVM with Repository pattern
- **DI**: Manual dependency injection
- **Async**: Coroutines + StateFlow/SharedFlow
- **UI**: Jetpack Compose with Material 3

### Contributing
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Support

For support and questions:
- Create an issue in this repository
- Check the troubleshooting guide in the documentation

## 🌟 Acknowledgments

- TensorFlow Lite for on-device ML inference
- Supabase for backend infrastructure
- Android Jetpack Compose for modern UI
- NodeMCU/ESP32 for hardware integration

---

**AURA — Transforming classroom management through AI-powered attention tracking** 📚🤖