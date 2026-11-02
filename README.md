# StreakKeeper 🚀

StreakKeeper is a modern, dopamine-inducing daily streak counter built with Java Swing. It features a unique press-and-hold mechanic, smooth animations, dynamic color generation, and a strict MVC architecture.

## ✨ Features

- **Press-and-Hold Mechanic**: Increment your streak by holding down a large circular button for exactly 5 seconds. A circular progress arc tracks your hold in real-time.
- **Real-time Dopamine Colors**: The button generates vibrant, high-saturation colors in HSB space while you hold it. Even if you've already incremented today, you can still hold the button for the satisfying visual and particle effects.
- **Particle Celebration**: Completing a 5-second hold triggers a smooth, 60 FPS particle and ring explosion from the button.
- **Daily Limits & Anti-Cheat**: You can only increment once per calendar day. Changing your system clock backwards will not allow a second increment (guarded by `LocalDate` and an epoch millisecond timestamp).
- **Light & Dark Themes**: Seamlessly toggle between beautiful FlatLaf light and dark themes. Your choice is saved across restarts.
- **Streak History**: A 30-day heatmap at the bottom of the screen visualizes your consistency.
- **Milestone Celebrations**: Get a special toast notification at 7, 30, and 100-day streaks.
- **Export / Import**: Back up your data or move it between machines using JSON.
- **Responsive UI**: The layout scales gracefully, and the sidebar can be collapsed to save space.

## 🛠 Tech Stack

- **Java 17+**
- **Swing** (UI)
- **Maven** (Build tool)
- **FlatLaf** (Modern Look and Feel)
- **Gson** (JSON Persistence)

## 🚀 How to Run

### From the command line
Make sure you have Java 17+ and Maven installed.
```bash
# Build and package the executable JAR
mvn clean package

# Run the application
java -jar target/streakkeeper.jar