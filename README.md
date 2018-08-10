[![Build Status](https://travis-ci.org/guardianproject/haven.svg)](https://travis-ci.org/guardianproject/haven)

# About Haven

Haven is for people who need a way to protect their personal spaces and possessions without compromising their own privacy. It is an Android application that leverages on-device sensors to provide monitoring and protection of physical spaces. Haven turns any Android phone into a motion, sound, vibration and light detector, watching for unexpected guests and unwanted intruders. We designed Haven for investigative journalists, human rights defenders, and people at risk of forced disappearance to create a new kind of herd immunity. By combining the array of sensors found in any smartphone, with the world's most secure communications technologies, like Signal and Tor, Haven prevents the worst kind of people from silencing citizens without getting caught in the act.

<img src="https://raw.githubusercontent.com/guardianproject/haven/master/fastlane/android/metadata/en-US/images/phoneScreenshots/havenob1.png" width="25%"> 
<img src="https://raw.githubusercontent.com/guardianproject/haven/master/fastlane/android/metadata/en-US/images/phoneScreenshots/havenob2.png" width="25%"> 
<img src="https://raw.githubusercontent.com/guardianproject/haven/master/fastlane/android/metadata/en-US/images/phoneScreenshots/havenob3.png" width="25%"> 

View our full [Haven App Overview](https://guardianproject.github.io/haven/docs/preso/) presentation for more about the origins and goals of the project.

## Announcement and Public Beta

We are announcing Haven today, as an open-source project, along with a public beta release of the app. We are looking for contributors who understand that physical security is as important as digital, and who have an understanding and compassion for the kind of threats faced by the users and communities we want to support. We also think it is really cool, cutting edge, and making use of encrypted messaging and onion routing in whole new ways. We believe Haven points the way to a more sophisticated approach to securing communication within networks of things and home automation system.

Learn more about the story of this project at the links below:

* [Haven: Building the Most Secure Baby Monitor Ever?](https://guardianproject.info/2017/12/22/haven-building-the-most-secure-baby-monitor-ever/)
* [Snowden’s New App Uses Your Smartphone To Physically Guard Your Laptop](https://theintercept.com/2017/12/22/snowdens-new-app-uses-your-smartphone-to-physically-guard-your-laptop/)
* [Snowden's New App Turns Your Phone Into a Home Security System](https://www.wired.com/story/snowden-haven-app-turns-phone-into-home-security-system/)

## Project Team

Haven was developed through a collaboration between [Freedom of the Press Foundation](https://freedom.press) and [Guardian Project](https://guardianproject.info). Prototype funding was generously provided by FPF, and donations to support continuing work can be contributed through their site: https://freedom.press/donate-support-haven-open-source-project/

![Freedom of the Press Foundation](https://raw.githubusercontent.com/guardianproject/haven/master/art/logos/fopflogo.png)
![Guardian Project](https://raw.githubusercontent.com/guardianproject/haven/master/art/logos/gplogo.png)

## Safety through Sensors

Haven only saves images and sound when triggered by motion or volume, and stores everything locally on the device. You can position the device's camera to capture visible motion, or set your phone somewhere discreet to just listen for noises. Get secure notifications of intrusion events instantly and access the logs remotely or anytime later.

<img src="https://raw.githubusercontent.com/guardianproject/haven/master/fastlane/android/metadata/en-US/images/phoneScreenshots/haven-sound-config.png" width="25%"> 
<img src="https://raw.githubusercontent.com/guardianproject/haven/master/fastlane/android/metadata/en-US/images/phoneScreenshots/haven-event-media.png" width="25%"> 
<img src="https://raw.githubusercontent.com/guardianproject/haven/master/fastlane/android/metadata/en-US/images/phoneScreenshots/haven-event-list.png" width="25%"> 

The following sensors are monitored for a measurable change, and then recorded to an event log on the device:

-   **Accelerometer**: phone's motion and vibration
-   **Camera**: motion in the phone's visible surroundings from front or back camera
-   **Microphone**: noises in the environment
-   **Light**: change in light from ambient light sensor
-   **Power**: detect device being unplugged or power loss  

## Building

The application can be built using Android Studio and Gradle. It relies on a number of third-party dependencies, all of which are free, open-source, and listed at the end of this document.

## Install

You can currently get the Haven BETA release in one of three ways:

* Download [Haven from Google Play](https://play.google.com/store/apps/details?id=org.havenapp.main)
* First, [install F-Droid](https://f-droid.org) the open-source app store, and second, add our Haven Nightly "Bleeding Edge" repository by scanning the QR Code below:

<img src="https://guardianproject.github.io/haven-nightly/icon.png" width="50%"/> 

or add this repository manually in F-Droid's Settings->Repositories: [https://guardianproject.github.io/haven-nightly/fdroid/repo/](https://guardianproject.github.io/haven-nightly/fdroid/repo/)

* Grab the APK files from the [GitHub releases page](https://github.com/guardianproject/haven/releases)

You can, of course, build the app yourself, from source.

If you are an Android developer, you can learn more about how you can make use of F-Droid in your development workflow, for nightly builds, testing, reproducibility and more here: [F-Droid Documentation](https://f-droid.org/en/docs/)

## Why no iPhone Support?

While we hope to support a version of Haven that runs directly on iOS devices in the future, iPhone users can still benefit from Haven today. You can purchase an inexpensive Android phone for less than $100, and use that as your "Haven Device", that you leave behind, while you keep your iPhone with you. If you run Signal on your iPhone, you can configure Haven on Android to send encrypted notifications, with photos and audio, directly to you. If you enable the "Tor Onion Service" feature in Haven (requires installing "Orbot" app as well), you can remotely access all Haven log data from your iPhone, using the Onion Browser app.

So, no, iPhone users, we didn't forget about you, and we hope you'll pick up an inexpensive Android burner today!

## Usage

Haven is meant to provide an easy onboarding experience, that walks users through configuring the sensors on their device to best detect intrusions into their environment.  (The current implementation has some of this implemented, but we are looking to improve this user experience dramatically.)

### Main view

Application's main view allows the user to select which sensors to use along with their corresponding levels of sensitivity. A security code must be provided, which will be needed later to disable monitoring. A phone number can be set, to which a message will be sent if any of the sensors is triggered.

### Notifications

When one of the sensors is triggered (reaches the configured sensitivity threshold), notifications are sent through the following channels (if enabled):

- SMS: a message is sent to the number specified when monitoring started
- Signal: if configured, can send end-to-end encryption notifications via Signal

Note that it's not necessary to install the Signal app on the device that runs Haven. Doing so may invalidate the app's previous Signal registration and safety numbers. Haven uses normal APIs to communicate via Signal.

Notifications are sent through a service running in the background that is defined in class `MonitorService`.

### Remote Access

All event logs and captured media can be remotely accessed through a [Tor Onion Service](https://www.torproject.org/docs/onion-services). Haven must be configured as an Onion Service and requires the device to also have [Orbot: Tor for Android](https://guardianproject.info/apps/orbot) installed and running. 

## ATTRIBUTIONS

This project contains source code or library dependencies from the following projects:

* SecureIt project available at: https://github.com/mziccard/secureit Copyright (c) 2014 Marco Ziccardi (Modified BSD)
* libsignal-service-java from Open Whisper Systems: https://github.com/WhisperSystems/libsignal-service-java (GPLv3)
* signal-cli from AsamK: https://github.com/AsamK/signal-cli (GPLv3)
* Sugar ORM from chennaione: https://github.com/chennaione/sugar/ (MIT)
* Square's Picasso: https://github.com/square/picasso (Apache 2)
* JayDeep's AudioWife: https://github.com/jaydeepw/audio-wife (MIT)
* AppIntro: https://github.com/apl-devs/AppIntro (Apache 2)
* Guardian Project's NetCipher: https://guardianproject.info/code/netcipher/ (Apache 2)
* NanoHttpd: https://github.com/NanoHttpd/nanohttpd (BSD)
* MaterialDateTimePicker from wdullaer: https://github.com/wdullaer/MaterialDateTimePicker (Apache 2)
* Fresco Image Viewer: https://github.com/stfalcon-studio/FrescoImageViewer (Apache 2)
* Facebook Fresco Image Library: https://github.com/facebook/fresco (BSD)
* Audio Waveform Viewer: https://github.com/derlio/audio-waveform (Apache 2)
* FireZenk's AudioWaves: https://github.com/FireZenk/AudioWaves (MIT)
* MaxYou's SimpleWaveform: https://github.com/maxyou/SimpleWaveform (MIT)
