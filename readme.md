NYU Bus Tracker
===============

[NYU Bus Tracker](http://www.nyubustracker.com/) is designed to combine the functionality of the NYU
Mobile and Transloc apps (maps with real time bus locations) with the [massive schedules]
(http://www.nyu.edu/life/travel-and-transportation/university-transportation.html) of the NYU bus system.

This app relies on the [Transloc API](api.transloc.com) for drawing the routes, getting real time
bus locations, and getting the list of stops with locations. The bus times were manually parsed (not
available from the API).

See the GitHub issues for a list of things that need to be done.

Design
------
* The data is modeled by Bus, Route, Stop, and Time classes. They are all managed by a singleton
BusManager. Routes have an in order list of Stops. Stops have a list of Routes that service that
stop and a list of Times corresponding to that particular route. Data accesses from the activities
go through BusManager. Tests for connection between stops generally go through BusManager, as well.

* Every JSON file (segments, routes, stops, times) is cached when retrieved (except for bus
locations). But, background network calls should be made after returning the cached data to ensure
data shown to the user is up to date.

* Adapters are required to display the list of stops and times. Stops can be selected as the start
or end, or favorited by the user (so it appears at the top of the list). Times display which route
they correspond to (since, sometimes, more than one route travels between stops A and B).

* There is only one activity: MainActivity. We could create more to provide a full listing of times
by route/stop instead of just between the selected two stops.