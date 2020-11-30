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

APP_MANIFEST_FILE=$BUILD_REPOSITORY_LOCALPATH/app/src/main/AndroidManifest.xml
APP_SPLASH_ACTIVITY=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/ui/activities/SplashActivity.java
APP_LOGIN_ACTIVITY=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/ui/activities/LoginActivity.java
APP_NETWORK_MODULE=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/di/module/NetworkModule.java
APP_OAUTH_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_single_account.json

echo "Updating API KEYS"

sed -i '' "s|{GOOGLE_MAPS_KEY}|$GOOGLE_MAPS_KEY|g" $APP_MANIFEST_FILE
sed -i '' "s|{OAUTH_PATH}|$OAUTH_PATH|g" $APP_MANIFEST_FILE

sed -i '' "s|{OAUTH_CLIENT_ID}|$OAUTH_CLIENT_ID|g" $APP_OAUTH_JSON
sed -i '' "s|{OAUTH_PATH}|$OAUTH_PATH|g" $APP_OAUTH_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL}|$OAUTH_REDIRECT_URL|g" $APP_OAUTH_JSON
sed -i '' "s|{OAUTH_URL}|$OAUTH_URL|g" $APP_OAUTH_JSON

sed -i '' "s|{OAUTH_SCOPE}|$OAUTH_SCOPE|g" $APP_NETWORK_MODULE
sed -i '' "s|{OAUTH_SCOPE}|$OAUTH_SCOPE|g" $APP_LOGIN_ACTIVITY
sed -i '' "s|{APP_CENTER_KEY}|$APP_CENTER_KEY|g" $APP_SPLASH_ACTIVITY