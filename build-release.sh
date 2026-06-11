#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo " GitForAndroid Release APK Builder"
echo "========================================"

# --- JDK 17 ---
if [ -f "$SCRIPT_DIR/set-jdk17.sh" ]; then
    source "$SCRIPT_DIR/set-jdk17.sh"
else
    export JAVA_HOME="$SCRIPT_DIR/.jdk17"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "[1/4] Using JDK 17: $(java -version 2>&1 | head -1)"
fi

# --- Proxy ---
PROXY_HOST="192.168.56.1"
PROXY_PORT="7890"
export GRADLE_OPTS="-Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT"

# --- Keystore ---
KEYSTORE="$SCRIPT_DIR/gitforandroid.keystore"
STOREPASS="android123"
KEYPASS="android123"
ALIAS="gitforandroid"

if [ ! -f "$KEYSTORE" ]; then
    echo "[2/4] Generating keystore with JDK 17 keytool..."
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -storetype PKCS12 \
        -alias "$ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$STOREPASS" \
        -keypass "$KEYPASS" \
        -dname "CN=GitForAndroid, OU=Dev, O=GitForAndroid, L=Unknown, ST=Unknown, C=CN"
    echo "       Keystore created: $KEYSTORE"
else
    echo "[2/4] Using existing keystore: $KEYSTORE"
fi

# --- Build ---
echo "[3/4] Building release APK (this may take a while)..."
./gradlew assembleRelease --no-daemon

# --- Collect APK ---
APK_SRC="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
APK_DST="$SCRIPT_DIR/GitForAndroid-release.apk"

if [ -f "$APK_SRC" ]; then
    cp "$APK_SRC" "$APK_DST"
    SIZE=$(du -h "$APK_DST" | cut -f1)
    echo "[4/4] Done!"
    echo ""
    echo "  Release APK: $APK_DST"
    echo "  Size:        $SIZE"
    echo ""
    echo "  adb install $APK_DST"
else
    echo "[4/4] ERROR: APK not found at $APK_SRC"
    echo "       Check build output above for errors."
    exit 1
fi
