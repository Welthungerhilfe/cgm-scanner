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

APP_CONSTANT_FILE=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/helper/AppConstants.java
APP_MANIFEST_FILE=$BUILD_REPOSITORY_LOCALPATH/app/src/main/AndroidManifest.xml
APP_SPLASH_ACTIVITY=$BUILD_REPOSITORY_LOCALPATH/app/src/main/java/de/welthungerhilfe/cgm/scanner/ui/activities/SplashActivity.java

echo "$APP_CONSTANT_FILE"

if [ -e "$APP_CONSTANT_FILE" ]
then
    echo "Updating AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY"

    # Key variable uses slashes /, therefore a different command seperator | is needed for sed
    sed -i '' "s|{AZURE_ACCOUNT_NAME}|$AZURE_ACCOUNT_NAME|g" $APP_CONSTANT_FILE
    sed -i '' "s|{AZURE_ACCOUNT_KEY}|$AZURE_ACCOUNT_KEY|g" $APP_CONSTANT_FILE

    sed -i '' "s|{GOOGLE_MAPS_KEY}|$GOOGLE_MAPS_KEY|g" $APP_MANIFEST_FILE

    sed -i '' "s|{APP_CENTER_KEY}|$APP_CENTER_KEY|g" $APP_SPLASH_ACTIVITY

    echo "File content:"
    cat $APP_CONSTANT_FILE

else
    echo "$APP_CONSTANT_FILE is not found"
fi