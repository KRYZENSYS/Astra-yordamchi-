# 🛠️ Termux Orqali Astrafolio APK Qilish Qo'llanmasi (O'zbek Tilida)

Ushbu qo'llanma orqali **Astrafolio** Android loyihasini kompyutersiz, to'g'ridan-to'g'ri Android telefoningizdagi **Termux** terminalida `.apk` fayl qilib yig'ib olishingiz mumkin.

---

## 1-Qadam: Termux'ni Tayyorlash va Paketlarni O'rnatish

Termux ilovasini oching va quyidagi buyruqlarni ketma-ket kiriting:

```bash
termux-setup-storage
pkg update -y && pkg upgrade -y
pkg install -y git wget curl unzip zip openjdk-21 gradle android-tools aapt2
```

> **Muhim eslatma (ARM64 `aapt2` haqida):** Android telefonlar ARM64 (`aarch64`) protsessorida ishlaydi. Standart Google Maven ichidagi `aapt2` esa `x86_64` kompyuterlar uchun chiqarilgan. Shu sababli Termux'da `pkg install aapt2` orqali telefon protsessoriga mos `aapt2` o'rnatiladi va Gradle'ga `-Pandroid.aapt2FromMavenOverride=$(which aapt2)` parametri beriladi (`termux-build-apk.sh` buni avtomatik bajaradi).

---

## 2-Qadam: Android SDK Command-Line Tools O'rnatish (Avtomatik)

Repozitoriy ichidagi tayyor `termux-setup-sdk.sh` skriptini ishga tushiring:

```bash
chmod +x termux-setup-sdk.sh
./termux-setup-sdk.sh
```

Yoki qo'lda o'rnatmoqchi bo'lsangiz:

```bash
mkdir -p $HOME/android-sdk/cmdline-tools
cd $HOME/android-sdk/cmdline-tools
wget -O cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip

export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "platforms;android-35" "build-tools;35.0.0"
```

---

## 3-Qadam: Termux'da APK Yig'ish (`termux-build-apk.sh`)

Loyiha papkasiga qaytib, APK yig'ish skriptini ishga tushiring:

```bash
chmod +x termux-build-apk.sh
./termux-build-apk.sh
```

Yoki bevosita Gradle buyrug'i bilan:

```bash
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"

gradle assembleDebug \
  -Pandroid.aapt2FromMavenOverride="$(which aapt2)" \
  --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8"
```

---

## 4-Qadam: Tayyor APK'ni Telefonning `Download` Papkasiga Olish

Yig'ish tugagach (`BUILD SUCCESSFUL`), APK fayl quyidagi manzilda hosil bo'ladi:
- `app/build/outputs/apk/debug/app-debug.apk`

Uni telefonning **Download** papkasiga ko'chirish:

```bash
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/Astrafolio-Jarvis.apk
echo "Tayyor APK: /sdcard/Download/Astrafolio-Jarvis.apk"
```

Shundan so'ng telefonning **Fayl Menejeri (Files) -> Download** bo'limiga kirib, **`Astrafolio-Jarvis.apk`** ustiga bosib ilovani o'rnatishingiz mumkin!
