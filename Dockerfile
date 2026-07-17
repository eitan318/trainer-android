FROM ubuntu:latest

RUN apt update && apt install -y openjdk-17-jdk-headless sdkmanager && \
    sdkmanager --sdk_root=/opt/android-sdk "platform-tools" "build-tools;36.0.0" "platforms;android-36" && \
    yes | sdkmanager --licenses && \
    chmod -R a+rX /opt/android-sdk

ENV ANDROID_HOME=/opt/android-sdk
ENV GRADLE_USER_HOME=/gradle
ENV PATH=$PATH:$ANDROID_HOME/platform-tools

WORKDIR /src
