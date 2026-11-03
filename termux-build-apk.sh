#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=========================================================="
echo "       ASTRAFOLIO • TERMUX AVTOMATIK APK YIG'UVCHI        "
echo "=========================================================="

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

AAPT2_BIN="$(command -v aapt2 || true)"
AAPT2_ARG=""

if [ -n "$AAPT2_BIN" ]; then
    echo "✔ ARM64 aapt2 topildi: $AAPT2_BIN"
    AAPT2_ARG="-Pandroid.aapt2FromMavenOverride=$AAPT2_BIN"
else
    echo "ℹ Standart aapt2 ishlatiladi (Termux'da bo'lsangiz: pkg install aapt2)"
fi

echo "🚀 Gradle orqali Astrafolio APK yig'ilmoqda..."
gradle assembleDebug \
    $AAPT2_ARG \
    --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "✅ APK muvaffaqiyatli yig'ildi: $APK_PATH"
    if [ -d "/sdcard/Download" ]; then
        cp "$APK_PATH" "/sdcard/Download/Astrafolio-Jarvis.apk"
        echo "📲 Nusxa olindi: /sdcard/Download/Astrafolio-Jarvis.apk"
        echo "Endi telefonning 'Download' papkasidan Astrafolio-Jarvis.apk ni o'rnatishingiz mumkin!"
    fi
else
    echo "❌ APK fayl topilmadi. Xatoliklarni tekshiring."
    exit 1
fi
