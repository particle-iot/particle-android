# photon-tinker-android
Photon Tinker app for Android

Getting started
===============

Requirements
------------
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


FAQ
===
Q: I'm getting an error at build time, "The type java.nio.file.OpenOption cannot be resolved. It
is indirectly referenced from required .class files"

A: You can ignore this error.  That class is never referred to, it's just the build system
complaining.  See https://github.com/square/okhttp/issues/1052 ,
 _"OkHttp is careful to not exercise that code (and SPDY) where it is unsupported."_


License
=======
Apache 2.0

