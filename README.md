# Shishu Sneh - Baby's First Year Guide 🍼

Shishu Sneh is a comprehensive, privacy-first Android application designed to help parents track their baby's growth and manage vaccination schedules seamlessly. Built with modern Android development practices, it acts as a digital health companion for your little one's crucial early years.

## 🌟 Key Features

*   **Baby Profile Management**: Easily manage profiles for one or multiple babies. Switch between profiles effortlessly.
*   **Growth Tracking**: Record and monitor weight, height, and head circumference.
*   **Growth Charts & BMI**: Visualize your baby's growth trends with beautiful, interactive charts and automatic BMI calculation.
*   **Vaccination Reminders**: Keep track of the immunization schedule (based on the India National Immunization Schedule). Get timely notifications for upcoming and overdue vaccines.
*   **Measurement History**: View a complete log of all past measurements.
*   **Data Export & Backup**: Export growth data to CSV for easy sharing with pediatricians, and backup/restore full app data securely via JSON.
*   **Offline First & Private**: All data is stored locally on your device using Room Database. No cloud account required.
*   **Dark Mode Support**: Beautifully themed for both light and dark environments.

## 🛠️ Tech Stack & Architecture

This project is built using modern Android development tools and follows the **MVVM (Model-View-ViewModel)** architecture pattern for robust and maintainable code.

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **UI Toolkit:** Android Views with ViewBinding & [Material Design 3](https://m3.material.io/)
*   **Local Database:** [Room](https://developer.android.com/training/data-storage/room) with KSP
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (for reliable vaccination notifications)
*   **Charting:** [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
*   **File Sharing:** `FileProvider` for secure CSV/JSON exports

## 📱 Screenshots

*(Consider adding screenshots of the Onboarding, Dashboard, Growth Chart, and Vaccination Schedule here)*

## 🚀 Getting Started

To build and run this project locally:

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/Shishu-sneh.git
    ```
2.  **Open the project** in Android Studio (Giraffe or newer recommended).
3.  **Sync Gradle** to download all dependencies.
4.  **Run the app** on an emulator or physical Android device (API Level 24 / Android 7.0 or higher required).

## 💡 Development Phases

The development of Shishu Sneh was divided into structured phases:
*   **Phase 1**: Onboarding architecture, Room DB setup, and basic profile creation.
*   **Phase 2**: Dashboard implementation, Add Measurement flow, Growth Charts, and CSV Export.
*   **Phase 3**: Comprehensive Vaccination Schedule and WorkManager-based Background Reminders.
*   **Phase 4/5**: App Polish, Multi-profile support, Dark Mode tuning, and full JSON Backup/Restore.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
