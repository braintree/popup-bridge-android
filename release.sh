#!/bin/bash

set -e

echo "1) Update the version, and versionCode in PopuBridge/build.gradle"
echo "2) Update the version in the CHANGELOG and make sure that all changes have been noted"
echo "3) Update the version in the README, include the SNAPSHOT version, make it 1 patch version higher than the released one (You'll use this version number at the end)"
echo "4) Make sure an Android emulator is running for integration tests"
echo
echo "Press enter when you are ready to release."
read

if [[ $(./gradlew :PopupBridge:properties | grep version) == *-SNAPSHOT ]]; then
  echo "Stopping release, the version is a snapshot"
  exit 1
fi

echo "Running gradle tasks, please wait..."
echo

./gradlew clean lint :PopupBridge:test :PopupBridgeExample:connectedCheck
./gradlew :PopupBridge:uploadArchives

./gradlew :PopupBridge:closeRepository
echo "Sleeping for one minute to allow PopupBridge module to close"
sleep 60
./gradlew :PopupBridge:promoteRepository

echo "Release complete. Be sure to commit, tag and push your changes."
echo "After the tag has been pushed, update the releases tab on GitHub with the changes for this release from the CHANGELOG."
echo "Remember to bump the version and add '-SNAPSHOT' to the version after the release."
echo "\n"
read
