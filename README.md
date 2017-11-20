# Haven (aka Phoneypot)

## About

Haven is an android application that serves as a monitoring service by leveraging on-device sensors. The follow sensors are monitored for a measurable change, and then recorded to an event log on the device:

-   **Accelerometer**: phone's motion and vibration
-   **Camera**: motion in the phone's visible surroundings from front or back camera
-   **Microphone**: noises in the enviroment
-   **Light**: change in light from ambient light sensor
-   **Power**: detect device being unplugged or power loss  

## Building

The application can be built using Android Studio and Gradle. 

## Usage

### Main view

Application's main view allows the user to set which sensors to use and the corresponding level of sensitivity. A security code must be provided, needed to disable monitoring. A phone number can be set, if any of the sensors is triggered a message is sent to the specified number.

### Notifications

When one of the sensors is triggered (reaches the sensibility threshold) a notifications is sent through the following channels (if enabled).

- SMS: a message is sent to the number specified when monitoring started
- Signal: if configured, can send end-to-end encryption notifications via Signal

Notifications are sent through a service running in background that is defined in class `MonitorService`.

### Remote Access

All event logs and captured media can be remotely accessed through a Tor Onion Service. Haven must be configured as an Onion Service, and requires the device to also have Orbot: Tor for Android installed and running. 

## ATTRIBUTIONS

This project contains source code from the SecureIt project available at: https://github.com/mziccard/secureit Copyright (c) 2014 Marco Ziccardi

This project uses libsignal-service-java from Open Whisper Systems:

https://github.com/WhisperSystems/libsignal-service-java

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

This project also used signal-cli from AsamK:

https://github.com/AsamK/signal-cli

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

