# PhoneyPot

This project was based on original source code from the SecureIt project available at: https://github.com/mziccard/secureit

# Application

Prototype Android application that serves as a monitoring service by leveraging on device sensors. Among sensors exploited we report:

-   **Accelerometer**: used to detect phone's motion and vibration
-   **Camera**: used to detect motion in the phone's surroundings 
-   **Microphone**: used to detected noises in the enviroment

# Building

The application can be built using Android Studio and Gradle. 

# Usage

## Main view

Application's main view allows the user to set which sensors to use and the corresponding level of sensitivity. A security code must be provided, needed to disable monitoring. A phone number can be set, if any of the sensors is triggered a message is sent to the specified number.

# Notifications

When one of the sensors is triggered (reaches the sensibility threshold) a notifications is sent through the following channels (if enabled).

- SMS: a message is sent to the number specified when monitoring started

Notifications are sent through a service running in background that is defined in class `MonitorService`.

# ATTRIBUTIONS

This project uses libsignal-service-java from Open Whisper Systems:

https://github.com/WhisperSystems/libsignal-service-java

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

This project also used signal-cli from AsamK:

https://github.com/AsamK/signal-cli

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

