# Particle Android: Maintainer Info

This document is intended for mobile app maintainers at Particle.

## Publishing Our Libraries

### Background
Our libraries, like the Cloud SDK and Device Setup lib, are published to JCenter using a pair of plugins located at the root of the repo, `pom_generator_v1.gradle`, and `bintray_upload_v1.gradle`.  At some point, these plugins should probably be replaced by the [built-in support for Maven publishing](https://developer.android.com/studio/preview/features/#agp-3-6-0) coming to the Android Gradle plugin in version 3.6.

### Preparation
There are a few prerequisite steps for publishing updates to our libs:

1. Get a Bintray account on [Bintray.com](https://bintray.com/).
1. Join the Particle org on Bintray by going to [this page](https://bintray.com/particle) and clicking the "Join" button.
1. Get your Bintray API key by visiting [your profile page](https://bintray.com/profile/edit), and clicking on the API Key section on the left.
1. Create a file called `bintray_user_auth_secrets.properties` in the _parent_ folder of your API root.  (The file is located there to prevent Bintray secrets from accidentally being committed to the repo.)  The file should look contain two keys, `bintray.user` and `bintray.apikey`.  It should look like the following:
```properties
bintray.user=your_bintray_username
bintray.apikey=0123456789abcdef0123456789abcdef01234567
```

### Making releases

See [`RELEASING.md`](../RELEASING.md) at the root of the repo.


## Publishing App Updates

### Preparation

1. Make your Particle GSuite account into a Google Play developer account via the usual process.
1. Get your Google Play developer account added as an admin on the Google Play Console for the Tinker app.  Ask in the `#mobile` or `#engineering` Slack channels, or just ask your manager to find out who can help you with this.
1. Get your Particle GSuite account added to [the Firebase console](https://console.firebase.google.com/project/particle-app/).  (Whomever adds you to the Play Console should be able to do this, too.)
1. [Download the Google Services JSON file](https://support.google.com/firebase/answer/7015592) and put it in `app/google-services.json`.  This file should not be added to git, and is in the repo's `.gitignore`, since it contains API keys and other things which shouldn't be made public.
1. Download the app signing keystore from the Engineering password vault and store it somewhere _outside_ of the repo.
1. Download the `oauth_client_creds.xml` file from our private mobile assets repository, and put it in `app/src/main/res/values/oauth_client_creds.xml`.  Since it contains OAuth secrets, this file is also part of the repo's `.gitignore`.


### Making releases

See [`RELEASING.md`](../RELEASING.md) at the root of the repo.

