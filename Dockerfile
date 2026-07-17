FROM ubuntu:latest

RUN apt update && apt install -y openjdk-17-jdk-headless sdkmanager

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

RUN sdkmanager --sdk_root=/opt/android-sdk "platform-tools" "build-tools;36.0.0" "platforms;android-36" "cmdline-tools;latest"
RUN yes | sdkmanager --licenses

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN chmod -R a+rX /opt/android-sdk

WORKDIR /project
CMD ["/bin/bash"]

