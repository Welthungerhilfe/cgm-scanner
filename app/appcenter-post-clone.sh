#!/usr/bin/env bash

echo "Building"
echo " - PROJECT_NUMBER: $PROJECT_NUMBER"

# Updating google-services.json

gsFile=$BUILD_REPOSITORY_LOCALPATH/app/src/release/google-services.json

sed -i '' "s/PROJECT_NUMBER/$PROJECT_NUMBER/g" $gsFile
sed -i '' "s/PROJECT_ID/$PROJECT_ID/g" $gsFile
sed -i '' "s/MOBILESDK_APP_ID/$MOBILESDK_APP_ID/g" $gsFile
sed -i '' "s/API_KEY/$API_KEY/g" $gsFile

# Print out file for reference
cat $gsFile

echo "Updated id!"

echo "Setting Gradle version to 8.0"

# Set the Gradle version in gradle-wrapper.properties
GRADLE_VERSION="8.0"
WRAPPER_PROPERTIES="gradle/wrapper/gradle-wrapper.properties"

# Ensure the gradle/wrapper directory exists
mkdir -p gradle/wrapper

# Download the specific Gradle version
GRADLE_DISTRIBUTION_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-all.zip"

# Update the gradle-wrapper.properties file with the specified Gradle version
echo "distributionUrl=${GRADLE_DISTRIBUTION_URL}" > $WRAPPER_PROPERTIES

# Download the specified Gradle distribution
curl -L -o gradle-wrapper.zip $GRADLE_DISTRIBUTION_URL
unzip -q gradle-wrapper.zip -d gradle/wrapper
rm gradle-wrapper.zip

# Ensure gradlew is executable
chmod +x gradlew

# Print Gradle version to confirm
./gradlew --version

APP_MANIFEST_FILE=$BUILD_REPOSITORY_LOCALPATH/app/src/main/AndroidManifest.xml
APP_OAUTH_SANDBOX_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_sandbox.json
APP_OAUTH_QA_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_demoqa.json
APP_OAUTH_PRODUCTION_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_inbmz.json
APP_OAUTH_NAMIBIA_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_namibia.json
APP_OAUTH_NEPAL_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_nepal.json
APP_OAUTH_UGANDA_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_uganda.json
APP_OAUTH_BANGLADESH_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_bangladesh.json
APP_OAUTH_MALAWI_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_malawi.json
APP_OAUTH_SIERRA_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_sierra.json


APP_CONSTANTS=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/AppConstants.java
APP_SPLASH_ACTIVITY=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/ui/activities/SplashActivity.java


echo "Updating API KEYS"
sed -i '' "s|{APP_CENTER_KEY}|$APP_CENTER_KEY|g" $APP_CONSTANTS
sed -i '' "s|{APP_CENTER_KEY}|$APP_CENTER_KEY|g" $APP_SPLASH_ACTIVITY
sed -i '' "s|{GOOGLE_MAPS_KEY}|$GOOGLE_MAPS_KEY|g" $APP_MANIFEST_FILE
sed -i '' "s|{OAUTH_PATH}|$OAUTH_PATH|g" $APP_MANIFEST_FILE

echo "Updating API sandbox KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_SANDBOX}|$OAUTH_CLIENT_ID_SANDBOX|g" $APP_OAUTH_SANDBOX_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_SANDBOX_JSON
sed -i '' "s|{OAUTH_URL_SANDBOX}|$OAUTH_URL_SANDBOX|g" $APP_OAUTH_SANDBOX_JSON
sed -i '' "s|{OAUTH_SCOPE_SANDBOX}|$OAUTH_SCOPE_SANDBOX|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_SANDBOX}|$API_URL_SANDBOX|g" $APP_CONSTANTS

echo "Updating API demoqa KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_QA}|$OAUTH_CLIENT_ID_QA|g" $APP_OAUTH_QA_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_QA_JSON
sed -i '' "s|{OAUTH_URL_QA}|$OAUTH_URL_QA|g" $APP_OAUTH_QA_JSON
sed -i '' "s|{OAUTH_SCOPE_QA}|$OAUTH_SCOPE_QA|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_QA}|$API_URL_QA|g" $APP_CONSTANTS

echo "Updating API inbmz KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_PRODUCTION}|$OAUTH_CLIENT_ID_PRODUCTION|g" $APP_OAUTH_PRODUCTION_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_PRODUCTION_JSON
sed -i '' "s|{OAUTH_URL_PRODUCTION}|$OAUTH_URL_PRODUCTION|g" $APP_OAUTH_PRODUCTION_JSON
sed -i '' "s|{OAUTH_SCOPE_PRODUCTION}|$OAUTH_SCOPE_PRODUCTION|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_PRODUCTION}|$API_URL_PRODUCTION|g" $APP_CONSTANTS

echo "Updating API namibia KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_NAMIBIA}|$OAUTH_CLIENT_ID_NAMIBIA|g" $APP_OAUTH_NAMIBIA_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_NAMIBIA_JSON
sed -i '' "s|{OAUTH_URL_NAMIBIA}|$OAUTH_URL_NAMIBIA|g" $APP_OAUTH_NAMIBIA_JSON
sed -i '' "s|{OAUTH_SCOPE_NAMIBIA}|$OAUTH_SCOPE_NAMIBIA|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_NAMIBIA}|$API_URL_NAMIBIA|g" $APP_CONSTANTS

echo "Updating API nepal KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_NEPAL}|$OAUTH_CLIENT_ID_NEPAL|g" $APP_OAUTH_NEPAL_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_NEPAL_JSON
sed -i '' "s|{OAUTH_URL_NEPAL}|$OAUTH_URL_NEPAL|g" $APP_OAUTH_NEPAL_JSON
sed -i '' "s|{OAUTH_SCOPE_NEPAL}|$OAUTH_SCOPE_NEPAL|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_NEPAL}|$API_URL_NEPAL|g" $APP_CONSTANTS

echo "Updating API uganda KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_UGANDA}|$OAUTH_CLIENT_ID_UGANDA|g" $APP_OAUTH_UGANDA_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_UGANDA_JSON
sed -i '' "s|{OAUTH_URL_UGANDA}|$OAUTH_URL_UGANDA|g" $APP_OAUTH_UGANDA_JSON
sed -i '' "s|{OAUTH_SCOPE_UGANDA}|$OAUTH_SCOPE_UGANDA|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_UGANDA}|$API_URL_UGANDA|g" $APP_CONSTANTS

echo "Updating API bangladesh KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_BAN}|$OAUTH_CLIENT_ID_BAN|g" $APP_OAUTH_BANGLADESH_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_BANGLADESH_JSON
sed -i '' "s|{OAUTH_URL_BAN}|$OAUTH_URL_BAN|g" $APP_OAUTH_BANGLADESH_JSON
sed -i '' "s|{OAUTH_SCOPE_BAN}|$OAUTH_SCOPE_BAN|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_BAN}|$API_URL_BAN|g" $APP_CONSTANTS

echo "Updating API bangladesh KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_MALAWI}|$OAUTH_CLIENT_ID_MALAWI|g" $APP_OAUTH_MALAWI_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_MALAWI_JSON
sed -i '' "s|{OAUTH_URL_MALAWI}|$OAUTH_URL_MALAWI|g" $APP_OAUTH_MALAWI_JSON
sed -i '' "s|{OAUTH_SCOPE_MALAWI}|$OAUTH_SCOPE_MALAWI|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_MALAWI}|$API_URL_MALAWI|g" $APP_CONSTANTS

echo "Updating API sierra KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_SIERRA}|$OAUTH_CLIENT_ID_SIERRA|g" $APP_OAUTH_SIERRA_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_SIERRA_JSON
sed -i '' "s|{OAUTH_URL_SIERRA}|$OAUTH_URL_SIERRA|g" $APP_OAUTH_SIERRA_JSON
sed -i '' "s|{OAUTH_SCOPE_SIERRA}|$OAUTH_SCOPE_SIERRA|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_SIERRA}|$API_URL_SIERRA|g" $APP_CONSTANTS