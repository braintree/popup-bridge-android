#!/bin/bash

set -e

echo "1) Make sure to update the version in build.gradle and the README."
echo "2) Make sure an Android emulator is running for integration tests"
echo
echo "Press enter when you are ready to release."
read

./gradlew clean lint :PopupBridge:test :PopupBridgeExample:connectedCheck
./gradlew :PopupBridge:uploadArchives :PopupBridge:closeAndPromoteRepository

echo "Release complete. Be sure to commit, tag and push your changes."
echo "After the tag has been pushed, update the releases tab on GitHub with the changes for this release."
echo "\n"
read
