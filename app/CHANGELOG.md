3.1.2
======
- Required updates to target API level 30


3.1.1
======
- Fixes for event handling

3.1.0
======
- B5 SoM support
- Begin deprecating mesh features

3.0.2
======
- Crash fixes
- Remove some unnecessary resources, cutting down APK size
- Remove textviewrichdrawable library now that jetpack supports XML compound drawables
- Make the login screen the default if anyone has ever been logged in before
- Enable x86_64 builds

3.0.1
======
- Properly ellipsize long device names
- Crash fixes from 3.0.0

3.0.0
======
- Control Panel — rename devices, set notes, or even unclaim, all from a single section of the mobile app.
- Device Info Screen — view device ID, serial number, Device OS version; plus, you can ping, shout rainbows (signal mode), rename, and edit device notes.
- Device list filtering — sort and filter your fleet based on your needs right from the main screen of the app.
- UI refresh — improved information density and readability, plus multiple UI element tweaks across the app.
- Fixes firmware update and setup flow issues

2.3.13
======
- Fix BLE pairing bug in mesh setup with devices that have new/unknown serial numbers

2.3.12
======
- Argon/A Series and Boron/B Series devices can now join mesh networks during setup instead of just create them
- Fix firmware update loop bug with A/B/X Series devices 

2.3.11
======
- Fix Photon claiming issue
- Don't show unknown "product" devices as Photons, just show them as "other"/unknown 

2.3.10
======
- Automatic device type detection when starting setup
- Support for SoM devices (For now, A and B Series devices will appear as Argons and Borons respectively, but the setup process will work as expected.)
- Support new and unknown device serial numbers by getting product type from the API when necessary

2.3.0
=====
* Mesh setup support!
* Begin move to a monorepo!

2.2.2
======
* New feature for extra security - Two-step authentication.

2.2.0
======
* Fixed Raspberry Pi shown as Photons bug.
* Search and filter devices in main device list via search menu item in top action bar.
* Enabled publishing events from the app via menu item in top action bar.

2.1.2
======
* NullPointerException fix on event unsubscription in EventsFragment.

2.1.1
======
* NullPointerException fix on setup cancellation.

2.1.0
======
* Android & Particle libraries updated.
* Removing retrolambda, native Java 8 support.
* Moving view binding to ButterKnife.
* Removing unnecessary manifest entries.
* Analog Write value display fix.

2.0.1
======
* Android libraries updated
* NullPointerException and RejectedExecutionException
* MultiDex enabled

2.0.0
======
* Device inspector: interact with your device's functions, variables and see its published events
* UI Redesign, new color scheme
* Location services disabled alert
* Bug fixes
* Target api 26

1.7.0
======
* Upgrade cloud sdk and setup library to latest versions
* Lambdas
* Electron setup directed to browser as temporary fix
* Remove dependency on commons and guava
* Target api 25
* Forgot password fix
* Core pin functionality fix
* Fix related to Wifi configuration issues
* Added account info fields on sign up
* Analytics
* Device naming at the end of setup process

1.6.3
======
* Workaround for Electron setup on Kit Kat and below

1.6.0
======
* Electron support


1.5.1
======
* Update to latest SDK and device setup lib
* Fix crash on state restore
