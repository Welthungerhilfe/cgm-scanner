# Child Growth Monitor Scanner App
Mothers and governmental frontline workers often fail to detect severe malnutrition of children. As a result, they do not help the child in the right way. The magnitude of a nutrition crises (both in emergencies and chronic hunger situations) is often blurred. This hinders a determined response by emergency workers as well as policy makers.

We provide a game-changer in measurement and data processing for malnourished children under the age of 5 years. It is a fool proof solution based on a mobile app using augmented reality in combination with artificial intelligence. By determining weight and height through a 3D scan of children, the app can instantly detect malnutrition.

- [Child Growth Monitor Website](https://childgrowthmonitor.org)
- [GitHub main project](https://github.com/Welthungerhilfe/ChildGrowthMonitor/)
- info@childgrowthmonitor.org

## Branch policy

#### main branch

- Main branch contains source codes of the version on Google Play in Closed Alpha, Public Beta and Production tracks for Arengine camera and Intel RealSense camera
- We merge main-realsense into main after using by users for some period of time
#### main-realsense branch
- Main-realsense branch contains source codes of the version for Intel RealSense camera
- We merge develop into main-realsense after passing all tests
#### master branch
- Master branch contains source codes of the version on Google Play in Closed Alpha, Public Beta and Production tracks for Arengine camera
- We merge testing into master after using by users for some period of time
#### testing branch
- Testing branch contains source codes of the version on Google Play in Internal Testing and Demo/QA tracks for Arengine camera
- We merge develop into testing after passing all tests
#### develop branch
- Develop branch contains source codes of all features for Arengine camera
- Developers do all pull requests into develop
