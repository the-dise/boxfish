
#!/bin/bash

# Install the necessary dependencies 
sudo dnf install -y golang java-17-openjdk git android-tools java-17-openjdk-devel

# Environment variables
WORKSPACE=$(pwd)
XRAY_CORE_VERSION=${1:-"main"}

# Install gomobile
go install golang.org/x/mobile/cmd/gomobile@latest
export PATH=$(go env GOPATH)/bin:$PATH

# Configuring the Android SDK (if not configured)
if [ -z "$ANDROID_HOME" ]; then
  echo "Устанавливаем Android SDK"
  ANDROID_HOME="$HOME/Android/Sdk"
  mkdir -p $ANDROID_HOME
  export ANDROID_HOME
  export PATH=$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH

  sdkmanager "platform-tools" "platforms;android-30" "build-tools;30.0.3"
fi

# Create a directory for the build
mkdir -p "$WORKSPACE/build"
cd "$WORKSPACE/build"

# Clone the AndroidLibXrayLite repository
git clone --depth=1 -b main https://github.com/2dust/AndroidLibXrayLite.git
cd AndroidLibXrayLite

# Getting the right version of Xray Core
go get github.com/xtls/xray-core@$XRAY_CORE_VERSION || true

# Initialize gomobile and prepare the modules
gomobile init
go mod tidy -v

# Build the library and copy the .aar files to the libs directory
gomobile bind -v -androidapi 29 -ldflags='-s -w' ./
mkdir -p "$WORKSPACE/Boxfish/app/libs/"
cp *.aar "$WORKSPACE/Boxfish/app/libs/"

echo “The library build is complete and saved to $WORKSPACE/Boxfish/app/libs/”

