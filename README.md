# PhoneyPot

This project was based on original source code from the SecureIt project available at: https://github.com/mziccard/secureit

# Application

Sample Android application that serves as a monitoring service by leveraging on device sensors. Among sensors exploited we report:

-   **Accelerometer**: used to detect phone's motion
-   **Camera**: used to detect motion in the phone's surroundings 
-   **Microphone**: used to detected noises in the enviroment

# Building

The application can be built using Android Studio and Gradle. 

# Usage

## Main view

Application's main view allows the user to set which sensors to use and the corresponding level of sensitivity. A security code must be provided, needed to disable monitoring. A phone number can be set, if any of the sensors is triggered a message is sent to the specified number.

## Motion Detection

Motion detection is performed via an asynchronous task defined in `me.ziccard.secureit.async` that leverages on classes in `me.ziccard.secureit.motiondetection`.

## Microphone Fragment

The microphone page provides a two-bar histogram of sound levels being captured by the mic and is defined in class `MicrophoneFragment`
Mic data is captured via an asynchronous task (`me.ziccard.secureit.async.MicSamplerTask`) and the class 
`me.ziccard.secureit.codec.AudioCodec`.

# Notifications

When one of the sensors is triggered (reaches the sensibility threshold) a notifications is sent through the following channels (if enabled).

- SMS: a message is sent to the number specified when monitoring started

Notifications are sent through a service running in background that is defined in class `me.ziccard.secureit.serviceUploadService`.

