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
APP_OAUTH_SANDBOX_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_sandbox.json
APP_OAUTH_QA_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_qa.json
APP_OAUTH_PRODUCTION_JSON=$BUILD_REPOSITORY_LOCALPATH/app/src/main/res/raw/auth_config_production.json
APP_CONSTANTS=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/AppConstants.java
APP_SPLASH_ACTIVITY=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/ui/activities/SplashActivity.java

echo "Updating API KEYS"
sed -i '' "s|{APP_CENTER_KEY}|$APP_CENTER_KEY|g" $APP_SPLASH_ACTIVITY
sed -i '' "s|{GOOGLE_MAPS_KEY}|$GOOGLE_MAPS_KEY|g" $APP_MANIFEST_FILE
sed -i '' "s|{OAUTH_PATH}|$OAUTH_PATH|g" $APP_MANIFEST_FILE

echo "Updating API sandbox KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_SANDBOX}|$OAUTH_CLIENT_ID_SANDBOX|g" $APP_OAUTH_SANDBOX_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL_SANDBOX}|$OAUTH_REDIRECT_URL_SANDBOX|g" $APP_OAUTH_SANDBOX_JSON
sed -i '' "s|{OAUTH_URL_SANDBOX}|$OAUTH_URL_SANDBOX|g" $APP_OAUTH_SANDBOX_JSON
sed -i '' "s|{OAUTH_SCOPE_SANDBOX}|$OAUTH_SCOPE_SANDBOX|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_SANDBOX}|$API_URL_SANDBOX|g" $APP_CONSTANTS

echo "Updating API qa KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_QA}|$OAUTH_CLIENT_ID_QA|g" $APP_OAUTH_QA_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL_QA}|$OAUTH_REDIRECT_URL_QA|g" $APP_OAUTH_QA_JSON
sed -i '' "s|{OAUTH_URL_QA}|$OAUTH_URL_QA|g" $APP_OAUTH_QA_JSON
sed -i '' "s|{OAUTH_SCOPE_QA}|$OAUTH_SCOPE_QA|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_QA}|$API_URL_QA|g" $APP_CONSTANTS

echo "Updating API production KEYS"
sed -i '' "s|{OAUTH_CLIENT_ID_PRODUCTION}|$OAUTH_CLIENT_ID_PRODUCTION|g" $APP_OAUTH_PRODUCTION_JSON
sed -i '' "s|{OAUTH_REDIRECT_URL_PRODUCTION}|$OAUTH_REDIRECT_URL_PRODUCTION|g" $APP_OAUTH_PRODUCTION_JSON
sed -i '' "s|{OAUTH_URL_PRODUCTION}|$OAUTH_URL_PRODUCTION|g" $APP_OAUTH_PRODUCTION_JSON
sed -i '' "s|{OAUTH_SCOPE_PRODUCTION}|$OAUTH_SCOPE_PRODUCTION|g" $APP_CONSTANTS
sed -i '' "s|{API_URL_PRODUCTION}|$API_URL_PRODUCTION|g" $APP_CONSTANTS