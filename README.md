# EcoPot - Smart Garden IoT System

Automated plant care system with Android mobile app, ESP32 hardware, and intelligent plant assistant.

## 🌱 Features

### 📱 Mobile App (Kotlin + Android)
- **Real-time Monitoring**: Live soil moisture, plant status
- **Smart Control**: Manual & automated pump control
- **Scheduling**: Customize watering times & frequency
- **History Tracking**: View all watering events with timestamps
- **Notifications**: Get alerts for plant status changes
- **EcoBot Assistant**: Chatbot for plant care tips & information

### 🔧 Hardware Integration (ESP32)
- Soil moisture sensors
- Water pump automation
- WiFi connectivity
- Live data sync to mobile app every few seconds automatically.

## 💻 Tech Stack
- **Frontend**: Kotlin, Android Studio, Jetpack Compose/XML
- **Backend**: ESP32 microcontroller
- **Communication**: WiFi from main ESP-32
- **Local Storage**: SharedPreferences + device cache

## 🎯 How It Works
1. ESP32 continuously reads soil moisture sensors
2. Sends sensor data to Android app via WiFi
3. App displays real-time plant health status
4. User can manually control pumps or set automated schedules
5. EcoBot provides plant care tips & guidance
6. Push notifications alert users to important events

## ✅ Current Features
- ✅ Real-time sensor monitoring
- ✅ Manual & automatic pump control  
- ✅ Flexible watering schedules (daily/weekly)
- ✅ Complete watering history with analytics
- ✅ Push notifications for status changes
- ✅ Plant care chatbot with predefined responses
- ✅ Multi-plant support

## 🚀 Future Improvements
ML-based optimal watering predictions if possible

## 📚 Learning Outcomes
This project taught me:
- Kotlin + Android architecture patterns
- IoT device communication & integration
- Hardware-software integration
- Real-time data handling
- Mobile notifications
- Sensor data processing

## 📦 Installation

### Prerequisites
- Android Studio (latest)
- Android 8.0+ device/emulator
- ESP32 with firmware flashed

### Steps
1. Clone the repository
2. Open in Android Studio
3. Configure ESP32 WiFi credentials
4. Build and run on device

## 🔧 Configuration
Update WiFi settings in the app:
- Go to Settings
- Enter ESP32 network SSID & password
- App will auto-connect on restart

## 📝 Project Structure
```
EcoPot-App/
├── app/
│   ├── src/main/kotlin/
│   │   ├── com.example.ecopot/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ChatbotActivity.kt
│   │   │   ├── AnalyticsActivity.kt
│   │   │   ├── HistoryActivity.kt
│   │   │   └── ScheduleActivity.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   └── values/
│   └── build.gradle
└── README.md
```

## 🤖 EcoBot Chatbot
The chatbot assistant provides:
- Plant care tips
- Watering guidelines
- Plant status interpretation
- Troubleshooting advice

*Current implementation uses predefined responses. Future updates will include ML-based recommendations(still learning ML)*

## 🎓 What I Learned Building This
- Started with AI-generated code → spent weeks understanding **why** it works
- Built something that actually **controls real hardware**
- Integrated 2 different technologies (Android, ESP32)
- The best learning comes from asking "why?" not just copying


## 👨‍💻 Author
Built as a multidisciplinary learning project combining mobile development, IoT, and embedded systems.

## 🤝 Contributing
Open to contributions! Especially interested in:
- ML model implementations for watering prediction
- Plant species database expansion
- UI/UX improvements
- Testing on different plant types

--- 🌿
