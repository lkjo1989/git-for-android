#!/bin/bash
# Use local Gradle 8.8 and JDK 17 to build
export JAVA_HOME=/root/git-for-android/.jdk17
export ANDROID_HOME=/usr/local/android-sdk
export GRADLE_HOME=/usr/local/gradle-8.8
export PATH=$JAVA_HOME/bin:$GRADLE_HOME/bin:$PATH
exec "$@"
