# ASTRAFOLIO • 5-AI Ovozli Jarvis, AQ. Multi-API Token Klaster va Lokal Server

**Astrafolio** — o'zbek tilida to'liq ovoz bilan telefonni boshqaruvchi, 24/7 orqa fon rejimida ishlovchi, kiruvchi qo'ng'iroqlarga avtomatik AI kotib sifatida javob beruvchi, `AQ.` bilan boshlanuvchi bir nechta AI Studio API kalitlarini ulab token sarfini hisoblovchi hamda **Ichki Lokal/Internet HTTP Server (Port 8080)** bilan jihozlangan Android ilova.

---

## 📱 Termux Orqali Telefonning O'zida APK Yig'ish (To'liq Qo'llanma)

Kompyutersiz, to'g'ridan-to'g'ri Android telefoningizdagi **Termux** ilovasi orqali ushbu repozitoriyadan tayyor `.apk` fayl yig'ib olish uchun maxsus avtomatlashtirilgan skriptlar qo'shilgan:

- `termux-setup-sdk.sh` — Termux ichiga `openjdk-21`, `gradle`, `aapt2` (ARM64) va Android SDK (`platforms;android-36`, `build-tools;35.0.0`) ni avtomatik o'rnatadi.
- `termux-build-apk.sh` — Birgina buyruq bilan `.apk` faylni yig'adi va uni telefonning **Yuklanmalar (`/sdcard/Download/Astrafolio-Jarvis.apk`)** papkasiga nusxalab beradi.
- Batafsil qo'llanma: [`TERMUX_APK_BUILD_UZ.md`](./TERMUX_APK_BUILD_UZ.md)

### ⚡ Tezkor 3 Qadam (Termux ichida)

```bash
# 1. Xotiraga ruxsat berish va kerakli paketlarni yangilash
termux-setup-storage
pkg update -y && pkg upgrade -y
pkg install -y git wget unzip openjdk-21 gradle android-tools aapt2

# 2. Repozitoriyani klonlash (yoki yuklab olingan papkaga kirish)
git clone <SIZNING_REPO_MANZILINGIZ> astrafolio
cd astrafolio

# 3. Avto-sozlash va APK yig'ish skriptini ishga tushirish
chmod +x termux-setup-sdk.sh termux-build-apk.sh
./termux-setup-sdk.sh
./termux-build-apk.sh
```

Yig'ish yakunlangach, tayyor APK faylni telefoningizning **Fayllar -> Download -> `Astrafolio-Jarvis.apk`** manzilidan topib o'rnatishingiz mumkin!

---

## 🌐 Lokal va Internet Server Bilan Ishlash (`Port 8080`)

Ilova ishga tushishi bilan telefonning o'zida lokal HTTP va AI Proxy server (`http://<telefon-ip>:8080` va `http://127.0.0.1:8080`) faollashadi:

- **Web HUD Boshqaruv Paneli**: `http://127.0.0.1:8080/`
- **AI Proxy (`AQ.` API Key bilan)**: `POST http://127.0.0.1:8080/api/generate`
- **Masofaviy Buyruq Yuborish**: `GET http://127.0.0.1:8080/api/command?q=Fonarikni+yoq`
- **Token va Qurilma Holati JSON**: `GET http://127.0.0.1:8080/api/status`
