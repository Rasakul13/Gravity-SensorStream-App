# 📱 Gravity SensorStream App

This Android application streams gravity sensor data from your android smartphone. It enables real-time interaction with with an end device for multiple purposes.

The applciation was developet in the scope of developing a Serious Game System including a Unity-based serious game running on a PC or Mac and a smartphone for caputring and streaming the gravity sensor data to the PC/Mac.

---

## 🎮 Use Case: Serious Game for Rehabilitation

The app is used as an input method in a serious game designed to support physical rehabilitation exercises. Players control on-screen elements by shifting their balance, which is captured by the phone's gravity sensor and transmitted over the local network.

---

## 📦 Download

- [Download Gravity SensorStream APK](https://github.com/Rasakul13/Gravity-App/releases/tag/v1.0.0)

Make sure to allow installation from unknown sources on your Android phone.

---

## 🌐 Setup Instructions

To connect the app with the game running on your PC/Mac:

1. Ensure both your **Android phone** and **PC/Mac** are connected to the **same Wi-Fi network**.
2. Install and launch the **Gravity SensorStream App** on your Android phone.
3. Press the ➕ **Add button** (bottom right corner).
4. Wait for the app to scan for devices and select your **PC/Mac from the list (by IP address)**.
5. Press **OK** to confirm.
6. Hit **Start Streaming** to begin sending sensor data.
7. Switch to the game — enjoy playing... or, well, training! 💪
8. When you're done, hit **Stop Streaming**.

---

## ⚙️ Troubleshooting & Advanced Settings

-  **No connection?** Make sure the Unity game has been allowed to access the network — a pop-up may appear the first time you start a level.
-  **Custom UDP Port:** The default port is `5555`, but this can be changed in both:
  - Gravity App: **Settings Menu** (top right corner)
  - Unity Game: **Settings Menu**
  
  Just make sure both ports match!

---

## 💡 Related Repositories

-  [ToadallyBalanced Serious Game Unity Project](https://github.com/Rasakul13/FruitGrind)

---

## 💬 Contact

For questions, feedback, or collaboration ideas, feel free to reach out:

**Lukas Rast**  
📧 lukas.rast@tuwien.ac.at  
