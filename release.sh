#!/bin/bash

set -e

echo "1) Make sure to update the versions in build.gradle and the README."
echo "2) Make sure an Android emulator is running for integration tests"
echo
echo "Press enter when you are ready to release."
read

if [[ $(./gradlew :PopupBridge:properties | grep version) == *-SNAPSHOT ]]; then
  echo "Stopping release, the version is a snapshot"
  exit 1
fi

./gradlew clean lint :PopupBridge:test :PopupBridgeExample:connectedCheck
./gradlew :PopupBridge:uploadArchives :PopupBridge:closeAndPromoteRepository

echo "Release complete. Be sure to commit, tag and push your changes."
echo "After the tag has been pushed, update the releases tab on GitHub with the changes for this release."
echo "Remember to bump the version and add '-SNAPSHOT' to the version after the release."
echo "\n"
read
