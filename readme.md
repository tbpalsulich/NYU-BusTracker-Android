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
[Bus](../blob/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/models/Bus.java),
[Route](../blob/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/models/Route.java),
[Stop](../blob/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/models/Stop.java), and
[Time](../blob/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/models/Time.java)
classes. They are all managed by a singleton
[BusManager](../blob/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/helpers/BusManager.java)
class. Routes have an in order list of Stops. Stops have a list of Routes that service that stop and
a list of Times corresponding to that particular route. Data accesses from the activities go through
BusManager. Tests for connection between stops generally go through BusManager, as well.

* Every JSON file (segments, routes, stops, times) is cached when retrieved (except for bus
locations). Background network calls are made to ensure data is up to date.

* [Adapters](../tree/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/adapters)
are required to display the list of stops and times. Stops can be selected as the start or end, or
favorited by the user (so it appears at the top of the list). Times display which route they
correspond to (sometimes, more than one route travels between stops A and B).

* There is only one activity: [MainActivity]
(../blob/master/NYUBusTracker/src/main/java/com/palsulich/nyubustracker/activities/MainActivity.java).
In the future, we may add additional activities to access times in an alternate format (like by
route + stop, instead of stop + stop).

Install
-------
1. Install and open [Android Studio](https://developer.android.com/sdk/installing/studio.html).
2. Open the SDK Manager (Tools > Android > SDK Manager) from Android Studio.
3. Install API 20, Android Support Repository, Android Support Library, 
Google Analytics App Tracking SDK, Google Play Services, and Google Repository. You can then close 
the SDK Manager.
4. Set your API keys in the 
[API keys](../blob/master/NYUBusTracker/src/main/res/values/api-keys.xml) file. You will need:
  * A [Google Analytics](http://www.google.com/analytics/) application with a production and debug property.
  * A [Google Developer](https://console.developers.google.com) project consuming the Maps API,
  following the directions 
  [here](https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key).
  * A [Mashape](https://www.mashape.com) application consuming the Transloc OpenAPI 1.2 API.
5. Connect an Android phone or an emulator. In my opinion, a physical phone is easier to use. But,
if you don't have one, you can try the built in Android emulator or a (much faster) 
[Genymotion](http://www.genymotion.com/) emulator. As a warning, you may have to do some extra work 
to get maps working in an emulator.

Please see the LICENSE file for license information.