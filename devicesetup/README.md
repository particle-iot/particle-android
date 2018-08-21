<p align="center" >
    <img src="http://oi60.tinypic.com/116jd51.jpg" alt="Particle logo" title="Particle">
</p>

<!---
(WIP Update link once we have CI in place)
[![Build Status](https://travis-ci.org/AFNetworking/AFNetworking.svg)](https://travis-ci.org/Spark-SDK/Spark-SDK)
-->

# Particle Device Setup library

The Particle Device Setup library provides everything you need to offer your
users a simple initial setup process for Particle-powered devices.  This includes
all the necessary device communication code, an easily customizable UI, and a
simple developer API.

The setup UI can be easily customized by a modifying Android XML resource files.
Available customizations include: look & feel, colors, fonts, custom brand logos
and more.  Customization isn't required for a nice looking setup process,
though: good defaults are used throughout, with styling generally following
Google's Material Design guidelines.

With the Device Setup library, you only need to make one simple call from
your app, and the Particle setup process UI launches to guide the user
through the device setup process.  When that process finishes, the user is
returned to the Activity where they were left off, and a broadcast intent
is sent out with the ID of the device she just set up and claimed.

The wireless setup process for the Photon uses very different underlying
technology from the Core.  The Core used _SmartConfig_, while the Photon
uses what we call a “soft AP” mode: during setup, the Photon advertises
itself as a Wi-Fi network.  The mobile app configures the Android device to
connect to this soft AP network, and using this connection, it can provide
the Particle device with the credentials it needs for the Wi-Fi network
you want the to Photon to use.


## Getting started, getting help, and everything else

Full documentation, including a getting started guide, lots of API examples, support & feedback links, and more are all available from our documentation page: https://docs.particle.io/reference/android/


## Maintainers

- Julius Skripkauskas [Github](https://github.com/cityvibes) | [Twitter](https://www.twitter.com/azemar)
- Jens Knutson [Github](https://github.com/jensck/) | [Google+](https://google.com/+JensKnutson)
- Ido Kleinman [Github](https://www.github.com/idokleinman) | [Twitter](https://www.twitter.com/idokleinman)

## License

The Particle Device Setup library is available under the Apache License 2.0.
See the `LICENSE` file for the complete text of the license.
