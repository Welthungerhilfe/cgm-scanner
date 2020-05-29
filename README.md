# Child Growth Monitor Scanner App
Mothers and governmental frontline workers often fail to detect severe malnutrition of children. As a result, they do not help the child in the right way. The magnitude of a nutrition crises (both in emergencies and chronic hunger situations) is often blurred. This hinders a determined response by emergency workers as well as policy makers.

We provide a game-changer in measurement and data processing for malnourished children under the age of 5 years. It is a fool proof solution based on a mobile app using augmented reality in combination with artificial intelligence. By determining weight and height through a 3D scan of children, the app can instantly detect malnutrition.

- [Child Growth Monitor Website](https://childgrowthmonitor.org)
- [GitHub main project](https://github.com/Welthungerhilfe/ChildGrowthMonitor/)
- info@childgrowthmonitor.org

## App development

We are going to bring our app on new devices. The app is going to target Google ARCore and also Google Tango. To make this possible we have to do some major changes in our repository. Not all changes could be done immediatelly and that's why we defined current and future branch policies.

## Current branch policy

#### master branch
- Contains only Google Tango version
- We review all pull requests
- We do not merge develop into master

#### develop branch
- Contains Google ARCore and Google Tango versions
- We merge only ARCore specific features into develop
- We merge master into develop regularly

## Future branch policy

#### master branch
- Contains Google ARCore and Google Tango versions
- Master branch contains source codes of the version on Google Play
- We merge develop into master after passing all tests

#### develop branch
- Contains Google ARCore and Google Tango versions
- Developers do all pull requests into develop
