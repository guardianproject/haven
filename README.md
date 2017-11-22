# Haven: Protect Yourself
<img src="https://raw.githubusercontent.com/guardianproject/haven/master/art/screens/havenob1.png" width="25%"> <img src="https://raw.githubusercontent.com/guardianproject/haven/master/art/screens/havenob2.png" width="25%"> <img src="https://raw.githubusercontent.com/guardianproject/haven/master/art/screens/havenob3.png" width="25%">


## About

Haven is for people who need a way to protect their personal spaces and possessions without compromising their own privacy. It is an Android application that leverages on-device sensors to provide monitoring and protection of physical spaces. Haven turns any Android phone into a motion, sound, vibration and light detector, watching for unexpected guests and 
unwanted intruders. Designed originally to defeat the infamous “evil maid” attack, Haven can provide protection in a wide variety of situations: protecting sensitive computers, monitoring offices and homes, detecting "wild life", capturing evidence of human rights violations and disappearances, as well as common vandalism and harassment.

Haven only saves images and sound when triggered by motion or volume, and stores everything locally on the device. You can position the device's camera to capture visible motion, or set your phone somewhere discreet to just listen for noises. Get secure notifications of intrusion events instantly and access the logs remotely or anytime later.

View our current presentation deck on the project at: https://github.com/guardianproject/phoneypot/blob/master/docs/Haven%20App%20Presentation.pdf

### Sensors


<img src="https://raw.githubusercontent.com/guardianproject/haven/master/art/screens/haven-sound-config.png" width="25%"> <img src="https://raw.githubusercontent.com/guardianproject/haven/master/art/screens/haven-event-media.png" width="25%"> <img src="https://raw.githubusercontent.com/guardianproject/haven/master/art/screens/haven-event-list.png" width="25%">

The follow sensors are monitored for a measurable change, and then recorded to an event log on the device:

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

