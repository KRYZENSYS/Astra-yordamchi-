#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=========================================================="
echo "  ASTRAFOLIO • TERMUX ANDROID SDK & ARM64 AAPT2 SOZLASH   "
echo "=========================================================="

echo "[1/5] Termux paketlari, Python va ARM64 aapt2 o'rnatilmoqda..."
pkg update -y
pkg install -y git wget curl unzip zip python openjdk-21 gradle android-tools aapt2

SDK_DIR="$HOME/android-sdk"
CMDLINE_DIR="$SDK_DIR/cmdline-tools"

mkdir -p "$CMDLINE_DIR"

if [ ! -f "$CMDLINE_DIR/latest/bin/sdkmanager" ]; then
    echo "[2/5] Android Command-Line Tools yuklab olinmoqda..."
    cd "$CMDLINE_DIR"
    wget -O cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    rm -rf latest cmdline-tools
    unzip -q cmdline-tools.zip
    mv cmdline-tools latest
    rm -f cmdline-tools.zip
    cd - > /dev/null
else
    echo "[2/5] Android Command-Line Tools allaqachon mavjud."
fi

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

echo "[3/5] Android SDK litsenziyalari tasdiqlanmoqda..."
yes | sdkmanager --licenses > /dev/null || true

echo "[4/5] Android 36.1 / 36 Platform va Build-Tools o'rnatilmoqda..."
sdkmanager "platform-tools" "platforms;android-36.1" "platforms;android-36" "platforms;android-35" "build-tools;35.0.0" || \
sdkmanager "platform-tools" "platforms;android-36" "platforms;android-35" "build-tools;35.0.0"

if [ ! -f "$SDK_DIR/platforms/android-36.1/android.jar" ] && [ -d "$SDK_DIR/platforms/android-36" ]; then
    echo "[4.5/5] android-36.1 platform nusxasi yaratilmoqda..."
    cp -r "$SDK_DIR/platforms/android-36" "$SDK_DIR/platforms/android-36.1"
fi

if ! grep -q "ANDROID_SDK_ROOT" "$HOME/.bashrc" 2>/dev/null; then
    cat << 'EOF' >> "$HOME/.bashrc"
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
EOF
fi

echo "[5/5] Termux SDK tayyor! Endi './termux-build-apk.sh' ni bosing."
echo "=========================================================="
