#!/bin/bash
set -e

# Proje kök dizinini belirle
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Kaynak release APK yolu
SOURCE_APK=""
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    SOURCE_APK="app/build/outputs/apk/release/app-release.apk"
else
    # En son değiştirilen release APK'yı bul
    SOURCE_APK=$(find app/build/outputs/apk/release -name "*.apk" -type f 2>/dev/null | head -n 1)
fi

if [ -z "$SOURCE_APK" ] || [ ! -f "$SOURCE_APK" ]; then
    echo "❌ Hata: Release APK bulunamadı!"
    echo "Lütfen önce APK'yı derleyin: ./gradlew assembleRelease"
    exit 1
fi

# Hedef builds klasörü
BUILDS_DIR="$SCRIPT_DIR/builds"
mkdir -p "$BUILDS_DIR"

# Günün tarihi formatı (Örn: 22AUG26)
DATE_STR=$(LC_ALL=C date +"%d%b%y" | tr '[:lower:]' '[:upper:]')

# app/build.gradle.kts içerisinden versionCode al veya 1 kullan
VERSION_CODE=$(grep -E 'versionCode\s*=' app/build.gradle.kts 2>/dev/null | head -n 1 | awk '{print $NF}' | tr -d '";')
[ -z "$VERSION_CODE" ] && VERSION_CODE=1

# Tarihli dosya adını belirle (aynı gün yeni derleme varsa _v1, _v2 şeklinde ilerler)
V_NUM=$VERSION_CODE
TARGET_DATED_NAME=""

while [ -f "$BUILDS_DIR/TbTKTM_${DATE_STR}_v${V_NUM}.apk" ]; do
    # Eğer mevcut dosya ile kaynak dosya birebir aynı ise aynı dosyayı kullan
    if cmp -s "$SOURCE_APK" "$BUILDS_DIR/TbTKTM_${DATE_STR}_v${V_NUM}.apk"; then
        TARGET_DATED_NAME="TbTKTM_${DATE_STR}_v${V_NUM}.apk"
        break
    fi
    V_NUM=$((V_NUM + 1))
done

if [ -z "$TARGET_DATED_NAME" ]; then
    TARGET_DATED_NAME="TbTKTM_${DATE_STR}_v${V_NUM}.apk"
fi

DATED_APK="$BUILDS_DIR/$TARGET_DATED_NAME"
LATEST_APK="$BUILDS_DIR/TbTKTM_latest.apk"

# Kopyalama işlemleri
cp "$SOURCE_APK" "$DATED_APK"
cp "$SOURCE_APK" "$LATEST_APK"

FILE_SIZE=$(du -h "$SOURCE_APK" | cut -f1)

echo "=========================================="
echo "✅ APK kopyalama işlemi tamamlandı!"
echo "📦 Kaynak : $SOURCE_APK ($FILE_SIZE)"
echo "📅 Tarihli: builds/$TARGET_DATED_NAME"
echo "⭐ Latest : builds/TbTKTM_latest.apk"
echo "=========================================="
