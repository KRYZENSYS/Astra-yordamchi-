#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=========================================================="
echo "   ASTRAFOLIO • TERMUX TEZKOR HOTFIX VA APK YIG'UVCHI     "
echo "=========================================================="

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

if ! command -v python3 >/dev/null 2>&1; then
    pkg install -y python
fi

# 1. Fix AuthScreen.kt (Unresolved reference 'default_web_client_id')
python3 - << 'PYEOF'
import os

auth_file = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
if os.path.exists(auth_file):
    text = open(auth_file, "r", encoding="utf-8").read()
    old = "context.getString(R.string.default_web_client_id)"
    new = 'run { val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName); if (id != 0) context.getString(id) else "753907752916-ih2sgl5quph35jsuepodusthn74pnh6v.apps.googleusercontent.com" }'
    if old in text:
        text = text.replace(old, new)
        open(auth_file, "w", encoding="utf-8").write(text)
        print("✔ AuthScreen.kt default_web_client_id xatoligi tuzatildi!")
    else:
        print("✔ AuthScreen.kt allaqachon tuzatilgan.")
PYEOF

# 2. Ensure platforms/android-36.1 exists and patch android.jar for Termux ARM64 aapt2
mkdir -p "$ANDROID_HOME/platforms"
if [ ! -f "$ANDROID_HOME/platforms/android-36.1/android.jar" ]; then
    if [ -f "$ANDROID_HOME/platforms/android-36/android.jar" ]; then
        echo "📦 android-36 -> android-36.1 nusxalanmoqda..."
        cp -r "$ANDROID_HOME/platforms/android-36" "$ANDROID_HOME/platforms/android-36.1"
    elif [ -f "$ANDROID_HOME/platforms/android-35/android.jar" ]; then
        echo "📦 android-35 -> android-36.1 nusxalanmoqda..."
        cp -r "$ANDROID_HOME/platforms/android-35" "$ANDROID_HOME/platforms/android-36.1"
    fi
fi

python3 - "$ANDROID_HOME/platforms/android-36.1/android.jar" << 'PYEOF'
import sys, os, zipfile, struct

jar_path = sys.argv[1]
if not os.path.exists(jar_path):
    print("❌ Topilmadi:", jar_path)
    sys.exit(1)

print("🔧 android-36.1/android.jar Termux AAPT2 uchun moslashtirilmoqda...")
with zipfile.ZipFile(jar_path, "r") as zin:
    arsc = zin.read("resources.arsc")
    t, hlen, size, pkg_count = struct.unpack("<HHIi", arsc[:12])
    pos = hlen
    new_top_chunks = []
    kept_pkgs = 0

    while pos < len(arsc):
        ct, chlen, csize = struct.unpack("<HHI", arsc[pos:pos+8])
        if ct == 0x0001:
            new_top_chunks.append(arsc[pos:pos+csize])
        elif ct == 0x0200 and kept_pkgs == 0:
            pkg_header = arsc[pos:pos+chlen]
            ppos = pos + chlen
            sub_chunks = []
            spec_counts = {}
            while ppos < pos + csize:
                pct, pchlen, pcsize = struct.unpack("<HHI", arsc[ppos:ppos+8])
                if pct == 0x0206:
                    pass
                elif pct == 0x0202:
                    tid, res0, res1, entry_count = struct.unpack("<BBHI", arsc[ppos+8:ppos+16])
                    spec_counts[tid] = entry_count
                    sub_chunks.append(arsc[ppos:ppos+pcsize])
                elif pct == 0x0201:
                    tid, flags, res1, entry_count, entries_start = struct.unpack("<BBHII", arsc[ppos+8:ppos+20])
                    if flags == 2:
                        offs_raw = arsc[ppos+pchlen : ppos+pchlen + entry_count*2]
                        offs16 = struct.unpack(f"<{entry_count}H", offs_raw)
                        offs32 = [0xFFFFFFFF if o == 0xFFFF else (o * 4) for o in offs16]
                        new_offs_bytes = struct.pack(f"<{entry_count}I", *offs32)
                        ents_bytes = arsc[ppos+entries_start : ppos+pcsize]
                        new_entries_start = pchlen + len(new_offs_bytes)
                        new_pcsize = new_entries_start + len(ents_bytes)
                        new_type_hdr = (
                            struct.pack("<HHI", pct, pchlen, new_pcsize)
                            + struct.pack("<BBHII", tid, 0, res1, entry_count, new_entries_start)
                            + arsc[ppos+20 : ppos+pchlen]
                        )
                        sub_chunks.append(new_type_hdr + new_offs_bytes + ents_bytes)
                    elif flags == 1:
                        total_count = spec_counts.get(tid, entry_count)
                        offs32 = [0xFFFFFFFF] * total_count
                        sparse_raw = arsc[ppos+pchlen : ppos+pchlen + entry_count*4]
                        for i in range(entry_count):
                            idx, off16 = struct.unpack("<HH", sparse_raw[i*4:(i+1)*4])
                            if idx < total_count:
                                offs32[idx] = off16 * 4
                        new_offs_bytes = struct.pack(f"<{total_count}I", *offs32)
                        ents_bytes = arsc[ppos+entries_start : ppos+pcsize]
                        new_entries_start = pchlen + len(new_offs_bytes)
                        new_pcsize = new_entries_start + len(ents_bytes)
                        new_type_hdr = (
                            struct.pack("<HHI", pct, pchlen, new_pcsize)
                            + struct.pack("<BBHII", tid, 0, res1, total_count, new_entries_start)
                            + arsc[ppos+20 : ppos+pchlen]
                        )
                        sub_chunks.append(new_type_hdr + new_offs_bytes + ents_bytes)
                    else:
                        sub_chunks.append(arsc[ppos:ppos+pcsize])
                else:
                    sub_chunks.append(arsc[ppos:ppos+pcsize])
                ppos += pcsize
            sub_data = b"".join(sub_chunks)
            new_pkg_size = chlen + len(sub_data)
            new_pkg_header = struct.pack("<HHI", ct, chlen, new_pkg_size) + pkg_header[8:]
            new_top_chunks.append(new_pkg_header + sub_data)
            kept_pkgs += 1
        pos += csize

    body = b"".join(new_top_chunks)
    new_arsc = struct.pack("<HHIi", t, hlen, hlen + len(body), 1) + body

    tmp_jar = jar_path + ".termux.tmp"
    with zipfile.ZipFile(tmp_jar, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename == "resources.arsc":
                zi = zipfile.ZipInfo("resources.arsc")
                zi.compress_type = zipfile.ZIP_STORED
                zout.writestr(zi, new_arsc)
            else:
                zout.writestr(item, zin.read(item.filename))

os.replace(tmp_jar, jar_path)
print("✔ android-36.1/android.jar muvaffaqiyatli moslashtirildi!")
PYEOF

# 3. Ensure debug.keystore exists
if [ ! -f "debug.keystore" ]; then
    echo "🔑 debug.keystore yaratilmoqda..."
    keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

# 4. Run Gradle assembleDebug without stale configuration cache
gradle assembleDebug \
    -Pandroid.aapt2FromMavenOverride="$(command -v aapt2)" \
    --no-daemon \
    --no-configuration-cache \
    -Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8"

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/Astrafolio-Jarvis.apk 2>/dev/null || true
    echo "✅ APK TAYYOR: /sdcard/Download/Astrafolio-Jarvis.apk"
fi
