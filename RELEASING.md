# Making releases

This is documentation for Android library & app maintainers at Particle for doing consistent 
releases.  If this isn't you, you can safely ignore these docs!


## Making official releases: Cloud SDK

If you were releasing version `2.4.2`, you'd do the following:

1. Pull from origin to ensure you have the latest upstream changes
2. Make sure CHANGELOG is current
3. Update the `version` field in `cloudsdk/build.gradle` to `'2.4.2'`
4. Build a release and publish it to JCenter.  From the `cloudsdk` dir, 
do: `../gradlew clean build install bintrayUpload`
5. Submit a PR to the Particle docs site updating the version code in `android.md` to `2.4.2`
6. Update the SDK example app to pull the new SDK version from JCenter, do a clean build of the 
    example app and then run it as a final smoke test.
7. Commit and push the previous two changes
8. Tag the release: `git tag cloudsdk-2.4.2`
9. Push the tag: `git push origin cloudsdk-2.4.2`
10. Create a GitHub release with a title of "Cloud SDK 2.4.2"


## Making official releases: device (Photon) setup library

If you were releasing version `2.4.2`, you'd do the following:

1. Pull from origin to ensure you have the latest upstream changes
2. Make sure CHANGELOG is current
3. Update the `version` field in `devicesetup/build.gradle` to `'2.4.2'`
4. Build a release and publish it to JCenter.  From the `devicesetup` dir, 
    do: `../gradlew clean build install bintrayUpload`
5. Submit a PR to the Particle docs site updating the version code in `android.md` to `2.4.2`
6. Update the device setup example app to pull the new setup lib version from JCenter, do a clean 
    build of the example app and then run it as a final smoke test.
7. Commit and push the above changes to the changelog & version fields
8. Tag the release: `git tag devicesetup-2.4.2`
9. Push the tag: `git push origin devicesetup-2.4.2`
10. Create a GitHub release with a title of "Device Setup library 2.4.2"


## Making official releases: Tinker app

If you were releasing version `2.4.2`, you'd do the following:

1. Pull from origin to ensure you have the latest upstream changes
2. Write user-facing release notes for the Play Store with a high-level description of the changes 
    in this release
3. Get product management signoff on the Play Store release notes
4. Update CHANGELOG with a more detailed, technical description of these changes
5. Update the `versionCode` field in `app/build.gradle` to `102040201`[1], and update the 
    `versionName` field to `2.4.2 (1)` (if this will only be released to alpha or beta channels) 
    or just `2.4.2` (if it will be pushed to production)
6. Uncomment the release-only gradle plugins in `app/build.gradle` (fabric, google services) so 
    they're active for the release build
7. Build a signed release APK
8. Publish the APK to the appropriate channel (internal, alpha, or beta)
9. Announce the release internally (i.e.: via Slack) and ask for testers in the appropriate channels
10. Commit and push the above changes to the changelog & version fields
11. Tag the release: `git tag app-2.4.2`
12. Push the tag: `git push origin app-2.4.2`
13. Create a GitHub release with a title of "Tinker app 2.4.2"
14. Once the beta (etc) release has been tested appropriately, promote the build to production
15. Announce production build availability on Slack and if appropriate, in the community 
    forum: https://goo.gl/nEsGgK



## Making official releases: Mesh lib

(TBD)



[1] the versionCode scheme here is:
EPOCH (single digit)
MAJOR VERSION (2 digits)
MINOR VERSION (2 digits)
PATCH VERSION (2 digits)
BUILD VERSION (2 digits)

So, the first released build of version 2.4.2 becomes:
1 02 04 02 01
