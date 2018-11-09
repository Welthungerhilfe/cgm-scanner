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

# set environment for multiple playstore deployments
echo "Changes for demo playstore deployment"
if [ -n "$DEPLOYMENT" ]; then
  echo "setting deployment to $DEPLOYMENT"
  find . -type f -exec sed -i '' "s/de.welthungerhilfe.cgm.scanner/de.welthungerhilfe.cgm.$DEPLOYMENT/g" {} \;
  find . -type f -exec sed -i '' "s/cgm\/scanner/cgm\/$DEPLOYMENT/g" {} \;
  find . -name scanner -type d -exec mv {} {}/../$DEPLOYMENT \;
else
  echo "building for production deployment"
fi
