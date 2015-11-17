# Particle Tinker app for Android

[Install on your Android device from Google Play store](https://play.google.com/store/apps/details?id=io.particle.android.app)

Get connected and start building your Internet of Things project right away with Particle, the companion app to the Photon, a tiny but powerful Wi-Fi development board from the team at Particle. Your Photon and this app are all you need to get started with your project or prototype; the kit comes pre-loaded with our firmware libraries and is connected to the cloud out of the box.
Download Particle to easily connect your Photon to a Wi-Fi network in a few simple steps. No coding required. The app will take you through the entire set-up process step by step until the LED on your board is breathing cyan - indicating it's connected and ready to be programmed!

###Additional resources
* Read the getting started guide at http://particle.io/start
* Join the most active and helpful IoT community out there at http://community.particle.io
* Learn more about Particle syntax and hardware at http://docs.particle.io
* Start building your own code at http://particle.io/build
* Find answers to common questions at http://support.particle.io

## Open source project

###Requirements

1. [Android Studio 1.2](http://developer.android.com/sdk/index.html) or higher, and all the
latest SDK components (Just launch the SDK Manager, wait for the progress bar at the bottom
of the screen to finish, let it select all the updates, and hit the "Install X packages" button
in the lower right corner)
2. Once you have Android Studio installed, launch it, and from the "Welcome to 
Android Studio" screen, pick "Open an existing Android Studio project".  When it prompts 
you for the project location, point it at the top-level 'build.gradle' file in the repo.
3. Once AS has finished its initial cogitating\*, hit the "play" button on the toolbar 
to build and run the app on a device.


\* No, really, if you've just installed AS and are running it for the first time, you may want to
get a cup of coffee during this part.  And then drink that coffee slowly, perhaps while
reading [this documentation on how to speed up your Android Studio install](http://tools.android.com/tech-docs/configuration#TOC-Increasing-IDE-Memory).


###FAQ

Q: I'm getting an error at build time, "The type java.nio.file.OpenOption cannot be resolved. It
is indirectly referenced from required .class files"

A: You can ignore this error.  That class is never referred to, it's just the build system
complaining.  See https://github.com/square/okhttp/issues/1052 ,
 _"OkHttp is careful to not exercise that code (and SPDY) where it is unsupported."_

###License
Apache 2.0

