# Requires keystore.jks in app/ folder

FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update && apt-get install -y unzip git && rm -rf /var/lib/apt/lists/*

# 12266719 is from https://github.com/android-actions/setup-android/blob/9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407/action.yml#L9
#   which is used in release action already
ARG ANDROID_SDK_URL=https://dl.google.com/android/repository/commandlinetools-linux-12266719_latest.zip
ENV ANDROID_API_LEVEL=android-36
ENV ANDROID_BUILD_TOOLS_VERSION=36.0.0
ARG ANDROID_HOME=/usr/local/android-sdk-linux
ENV ANDROID_VERSION=36
ENV PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

#ADD --unpack $ANDROID_SDK_URL $ANDROID_HOME no zip support :-(
RUN curl -o /tmp/sdk.zip $ANDROID_SDK_URL && mkdir -p $ANDROID_HOME && cd $ANDROID_HOME && unzip /tmp/sdk.zip && rm /tmp/sdk.zip

RUN yes | $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --update
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME "build-tools;30.0.3" \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools"

# Volume with repo here
WORKDIR /build

ENV GRADLE_OPTS=-Dorg.gradle.daemon=false
ENV ANDROID_HOME=${ANDROID_HOME}

# Suppress warning
SHELL ["/bin/sh", "-c"]
CMD ./gradlew :app:assembleRelease && mkdir -p /out && cp app/build/outputs/apk/release/app-release.apk /out
