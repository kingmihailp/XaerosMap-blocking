#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec java -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    -Dgradle.user.home="$HOME/.gradle" \
    org.gradle.wrapper.GradleWrapperMain "$@"
