# 🏍️ TbTKTM - KTM Turn-by-Turn & TFT Dashboard Controller

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%20%2F%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Protocol-KTM%20KMRC%20%2F%20RFCOMM%20%2F%20BLE-FF6600?style=for-the-badge" alt="KTM Protocol" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License" />
</p>

**TbTKTM**, üçüncü taraf harita uygulamalarından (**Google Maps**, **Yandex Navigasyon** vb.) gelen canlı rota ve manevra verilerini yakalayarak **KTM 1290 Super Adventure** (ve Euro 5 BCCU ünitesine sahip tüm KTM / Husqvarna / GASGAS modelleri) TFT gösterge paneline anlık yansıtan, gelen mesajları kayan yazı (**Marquee Ticker**) olarak gösteren ve motosikletin **sol gidon kütüğündeki 8 fiziksel tuş** ile telefon kontrollerini (Medya, Ses, Asistan) yönetmeyi sağlayan yeni nesil Android uygulamasıdır.

---

## ✨ Öne Çıkan Özellikler

### 🧭 1. Gelişmiş Navigasyon Entegrasyonu (TBT Plus)
- **Google Maps & Yandex Navigasyon Desteği:** Arka planda çalışan `UniversalNavParser` servisi ile bildirimlerden dönüş yönü, sonraki dönüş mesafesi, cadde/sokak adı, tahmini varış süresi (ETA) ve hedefe kalan toplam mesafe anlık ayrıştırılır.
- **57 Resmi KTM Manevra İkonu:** Döner kavşak çıkışları (1-8), otoyol giriş/çıkışları, keskin/hafif dönüşler ve U dönüşleri dahil KTM TFT ekranının 57 yerleşik ikonuyla (`KtmTurnIcon`) tam eşleşme.
- **Otomatik Metin Temizleme:** Yol ve yönelim ekleri ("... yönünde", "... üzerinde") akıllıca ayıklanarak TFT ekranına en okunaklı formatta gönderilir.

### 📜 2. TFT Kayan Yazı Bildirim Sistemi (Marquee Ticker)
- **Desteklenen Uygulamalar:** WhatsApp, Telegram, SMS, Gmail, Yahoo Mail ve Outlook.
- **Akıllı Kayan Yazı:** Gelen mesajlar TFT ekranında 32 karakterlik kayar pencere ve 800ms adımlarla dinamik olarak gösterilir.
- **Güvenlik ve Önceliklendirme:** Mesaj akışı sürerken kritik bir dönüş manevrası algılandığında bildirim otomatik olarak iptal edilir ve harita talimatı ön plana alınır; bildirim bittiğinde harita görünümüne otomatik dönülür.

### 🌍 3. 5 Dilde Tam Yerelleştirme (i18n)
- **Desteklenen Diller:** Türkçe (🇹🇷), İngilizce (🇬🇧), İtalyanca (🇮🇹), İspanyolca (🇪🇸), Yunanca (🇬🇷).
- Manevra açıklamaları ve arayüz metinleri seçilen dilde dinamik olarak KTM TFT ekranına iletilir.

### 🕹️ 4. 8 Butonlu Gidon Tuşları Yönetimi (RCM)
- Motosikletin sol kütüğündeki **`SET`**, **`BACK`**, **`UP`**, **`DOWN`**, **`LEFT`**, **`RIGHT`**, **`C1`**, **`C2`** butonları Bluetooth üzerinden yakalanır.
- **Özelleştirilebilir Eylemler:**
  - ⏯️ Müzik Çal / Duraklat
  - ⏭️ / ⏮️ Sonraki / Önceki Parça
  - 🔊 / 🔉 Ses Seviyesi Artır / Azalt
  - 🎙️ Google Sesli Asistan
  - 🗺️ Haritayı Yeniden Ortalama

### ⚡ 5. Orijinal KMRC & RFCOMM Protokol Desteği
- **Tersine Mühendislik İle Doğrulanmış Mimari:** KTMconnect mobil uygulamasının kullandığı orijinal `EXTENDED_TBT` (`0xCC4D` / `cc4d1fb3-482e-4389-bdeb-57b7aac889ae`) RFCOMM kanalı üzerinden tam çift yönlü iletişim.
- **5-Bayt KMRC Taşıma Çerçevesi:** `[4 Bayt Big-Endian Uzunluk] + [1 Bayt MsgType (0x01)] + [JSON Payload]` framing motoru ile stabil soket iletişimi.
- **Otomatik Yeniden Bağlanma:** Motosiklet kontağı açıldığında veya Bluetooth menziline girildiğinde `BluetoothStateReceiver` ile otomatik el sıkışma ve bağlantı kurtarma.

### 🖥️ 6. Canlı TFT Gösterge Simülatörü
- Motosiklet yanında değilken bile arayüzü ve paket akışını test etmek için uygulama içinde gömülü KTM TFT HMI arayüz simülatörü ve Mock BCCU motoru (`KtmBccuSimulator`).

---

## 🏗️ Proje Mimarisi

```text
com.tbtktm/
├── ble/                                # Bluetooth & Donanım İletişim Katmanı
│   ├── KtmGattAttributes.kt            # BLE & RFCOMM Kanal UUID tanımları (0x0700, 0x0100, CC4D vb.)
│   ├── KtmProtoUtils.kt                # KMRC JSON paketleyici ve 5-bayt frame oluşturucu
│   ├── KtmFramingUtils.kt              # Salt + Padding BLE çerçeveleme algoritması
│   ├── KtmRfcommManager.kt             # Bluetooth Classic RFCOMM (EXTENDED_TBT) soket yöneticisi
│   ├── KtmBleManager.kt                # BLE GATT bağlantı, bildirim aboneliği ve komut kuyruğu
│   ├── KtmBleService.kt                # Kesintisiz arka plan Foreground Servisi
│   ├── KtmBccuSimulator.kt             # Testler için sanal motosiklet simülatörü
│   ├── KtmBccuAuthManager.kt           # BCCU el sıkışma ve kimlik doğrulama
│   └── BluetoothStateReceiver.kt       # Sistem Bluetooth durum dinleyicisi & otomatik yeniden bağlanma
├── parser/                             # Bildirim Dinleme ve Ayrıştırma Motoru
│   ├── NotificationParserService.kt    # Android NotificationListenerService tabanı
│   ├── UniversalNavParser.kt           # Google Maps & Yandex Navigasyon regex ayrıştırıcı
│   ├── AppNotificationParser.kt        # WhatsApp, Telegram, Mail vb. mesaj ayrıştırıcı
│   ├── GoogleMapsParser.kt             # Google Maps özel parser
│   ├── YandexNaviParser.kt             # Yandex Navigasyon özel parser
│   └── IconMatcher.kt                  # Metin -> KTM 57 İkon ID eşleştirme motoru
├── rcm/                                # Gidon Kumandası ve Eylem Tetikleyici
│   ├── HandlebarKeyManager.kt          # 8 yönlü tuş basış analizi ve event dağıtımı
│   └── ActionDispatcher.kt             # Medya, Ses ve Asistan Android Intent tetikleyicisi
├── ticker/                             # Kayan Yazı Motoru
│   └── TftMarqueeTicker.kt             # TFT ekranı için 32-karakter kayar pencere & öncelik yöneticisi
├── i18n/                               # Çoklu Dil Katmanı
│   ├── AppLanguage.kt                  # Desteklenen diller enum listesi (TR, EN, IT, ES, EL)
│   └── AppLanguageManager.kt           # Global dil durumu yöneticisi
├── model/                              # Veri Modelleri
│   ├── KtmTurnIcon.kt                  # 57 manevra ikonu ve 5 dilde açıklamaları
│   ├── HandlebarButton.kt              # Gidon butonları ve atanabilir eylemler
│   └── NavigationData.kt               # Canlı navigasyon ve telemetri durum modeli
├── util/                               # Yardımcı Araçlar
│   └── FileLogger.kt                   # Hata ayıklama ve canlı dosya loglayıcı
└── ui/                                 # Jetpack Compose Kullanıcı Arayüzü
    ├── screens/
    │   ├── DashboardScreen.kt          # Canlı gösterge, simülatör ve dil seçici ekranı
    │   ├── DeviceScanScreen.kt         # BLE / BT Classic cihaz tarama ve eşleştirme
    │   └── KeyMappingScreen.kt         # Gidon butonları özelleştirme menüsü
    ├── components/
    │   └── TftDashboardCard.kt         # KTM TFT HMI gösterge kartı bileşeni
    └── theme/                          # KTM Ready-to-Race tema ve renk paleti
```

---

## 📡 Protokol ve Teknik Detaylar

Detaylı donanım ve tersine mühendislik protokol dokümantasyonu için [Protocol_KTM.MD](file:///home/nakturk/Projects/KTMconnect_4.0.2.2026061701-release_APKPure/TbTKTM/Protocol_KTM.MD) dosyasına göz atabilirsiniz.

### KMRC Paket Yapısı:
```
+--------------------------+--------------------+---------------------------------------+
|  Length (4 Bytes Int32)  | MsgType (1 Byte)   |          JSON Payload (UTF-8)         |
|       Big-Endian         |       0x01         |     {"GuidanceUpdate": {...}, ...}    |
+--------------------------+--------------------+---------------------------------------+
|<----------------- 5 Bytes Header ------------>|<------------- N Bytes --------------->|
```

### Temel RFCOMM Kanalları:
| Kanal | Port (Hex) | RFCOMM Service UUID | Açıklama |
| :--- | :--- | :--- | :--- |
| **`EXTENDED_TBT`** | `0xCC4D` | `cc4d1fb3-482e-4389-bdeb-57b7aac889ae` | Turn-by-Turn Navigasyon ve Ekran Bildirimleri |
| **`CCUBASE`** | `0xCB2A` | `cb2a1fb3-482e-4389-bdeb-57b7aac889ae` | Temel Cihaz Durumu ve Giriş Handshake |
| **`TELEMETRY`** | `0xCB66` | `cb661fb3-482e-4389-bdeb-57b7aac889ae` | Gerçek Zamanlı Motosiklet Sensör Verileri |

---

## 📲 Kurulum ve Gereksinimler

### Gereksinimler
- **Android Sürümü:** Android 8.0 (API 26) veya üzeri (Android 12/13/14 tam uyumlu).
- **Geliştirme Ortamı:** Android Studio Hedgehog / Iguana / Jellyfish veya üzeri.
- **Derleme:** Kotlin 1.9+, Jetpack Compose (BOM 2024+), Gradle 8+.

### Gerekli Android İzinleri
1. **Bluetooth ve Konum İzinleri:** BLE tarama, bağlanma ve çevre cihaz keşfi için gereklidir (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`).
2. **Bildirim Erişimi (Notification Access):**
   - *Ayarlar ➔ Uygulamalar ➔ Özel Uygulama Erişimi ➔ Bildirim Erişimi* menüsünden **TbTKTM** uygulamasına yetki verin. (Google Maps, Yandex ve mesaj bildirimlerini okumak için zorunludur).
3. **Pil Optimizasyonunu Devre Dışı Bırakma:** Arka planda kesintisiz sürüş deneyimi için pil kısıtlamalarını "Kısıtlamasız" olarak ayarlayın.

---

## 🚀 Hızlı Başlangıç

1. Projeyi klonlayın ve Android cihazınıza derleyip yükleyin:
   ```bash
   git clone https://github.com/nakturk/TbTKTM.git
   ```
2. **TbTKTM** uygulamasını açın ve **Motosiklet** sekmesinden KTM motosikletinizle eşleşin.
3. **Gidon** sekmesinden sol kütük butonlarına dilediğiniz medya ve ses fonksiyonlarını atayın.
4. **Google Maps** veya **Yandex Navigasyon** üzerinden bir rota başlatın.
5. Motosikletinizin TFT ekranında canlı dönüş oklarının, sokak adlarının ve varış süresinin keyfini çıkarın! 🏍️💨

---

## 📄 Lisans

Bu proje açık kaynaklıdır ve [MIT Lisansı](LICENSE) altında sunulmaktadır.
KTM®, KTM Sportmotorcycle GmbH'nin tescilli ticari markasıdır. Bu projenin KTM AG ile doğrudan bir ticari bağı bulunmamaktadır.
