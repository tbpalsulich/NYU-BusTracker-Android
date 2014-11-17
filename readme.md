NYU Bus Tracker
===============

[NYU Bus Tracker](http://www.nyubustracker.com/) is designed to combine the functionality of the NYU
Mobile and Transloc apps (maps with real time bus locations) with the [massive schedules]
(http://www.nyu.edu/life/travel-and-transportation/university-transportation.html) of the NYU bus
system.

This app relies on the [Transloc API](http://www.api.transloc.com) for drawing the routes, getting 
real time bus locations, and getting the list of stops with locations. The bus times were manually 
parsed (not available from the API).

See the GitHub issues for a list of things that need to be done. All feature requests, bug reports,
pull requests are welcome! Please let me know if you have any issues or questions. Thanks!

Design
------
* The data is modeled by
[Bus](../master/NYUBusTracker/src/main/java/com/nyubustracker/models/Bus.java),
[Route](../master/NYUBusTracker/src/main/java/com/nyubustracker/models/Route.java),
[Stop](../master/NYUBusTracker/src/main/java/com/nyubustracker/models/Stop.java), and
[Time](../master/NYUBusTracker/src/main/java/com/nyubustracker/models/Time.java)
classes. They are all managed by a singleton
[BusManager](../master/NYUBusTracker/src/main/java/com/nyubustracker/helpers/BusManager.java)
class. Routes have an in order list of Stops. Stops have a list of Routes that service that stop and
a list of Times corresponding to that particular route. Data accesses from the activities go through
BusManager. Tests for connection between stops generally go through BusManager, as well.

* Every JSON file (segments, routes, stops, times) is cached when retrieved (except for bus
locations). Background network calls are made to ensure data is up to date.

* [Adapters](../../tree/master/NYUBusTracker/src/main/java/com/nyubustracker/adapters)
are required to display the list of stops and times. Stops can be selected as the start or end, or
favorited by the user (so it appears at the top of the list). Times display which route they
correspond to (sometimes, more than one route travels between stops A and B).

* There is only one activity: [MainActivity]
(../master/NYUBusTracker/src/main/java/com/nyubustracker/activities/MainActivity.java).
In the future, we may add additional activities to access times in an alternate format (like by
route + stop, instead of stop + stop).

Install
-------
1. Install and open [Android Studio](https://developer.android.com/sdk/installing/studio.html).
2. Open the SDK Manager (Tools > Android > SDK Manager) from Android Studio.
3. Install API level 20, Android Support Repository, Android Support Library, Google Analytics App 
Tracking SDK, Google Play Services, and Google Repository, then close the SDK Manager.
4. Fork this repository on GitHub by clicking the Fork button at the top right.
5. Clone your fork inside Android Studio (VCS > Checkout from Version Control > Log in to GitHub > 
Select your fork > Click clone).
6. Select use default gradle wrapper and click OK.
7. If needed, open the project view by hovering over the icon at the bottom left of Android Studio.
8. Set your API keys in the 
[API keys](../master/NYUBusTracker/src/main/res/values/api-keys.xml) file. You will need:
  * A [Google Developer](https://console.developers.google.com) project consuming the Maps API,
  following the directions 
  [here](https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key).
  * A [Mashape](https://www.mashape.com) application consuming the 
  [Transloc OpenAPI 1.2 API](https://www.mashape.com/transloc/openapi-1-2).
  * Optionally a [Google Analytics](http://www.google.com/analytics/) application with production 
  and debug properties.
  * Optionally a [Flurry Analytics](http://www.flurry.com/) company with production and debug 
  applications.
9. Connect an Android phone or an emulator. In my opinion, a physical phone is easier to use. But,
if you don't have one, you can try the built in Android emulator or a (much faster) 
[Genymotion](http://www.genymotion.com/) emulator. As a warning, you may have to do some extra work 
to get maps working in an emulator.
10. Run the app (green arrow at the top of Android Studio)!

Release
-------
Here is the release process, for when you're ready to push a new version to the Play Store.

1. Make sure `MainActivity.LOCAL_LOGV = false`. Run the app and make sure there is no logging.
2. Make sure the `DownloaderHelper.AMAZON_URL` is correct.
3. Make sure your API keys are correct.
4. Bump the release version in build.gradle.
5. Tag a release on GitHub.
6. ./gradlew assembleRelease
7. Run the app as a last minute check, to make sure everything is in working order.
8. Upload.

Please see the LICENSE file for license information.
