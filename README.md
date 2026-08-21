# TbTKTM - KTM 1290 Super Adventure Navigasyon & Gidon Kontrol Uygulaması

**TbTKTM**, **Google Maps** ve **Yandex Navigasyon** gibi üçüncü taraf harita uygulamalarından gelen canlı dönüş (Turn-by-Turn) verilerini yakalayarak **KTM 1290 Super Adventure** (ve BCCU ünitesine sahip tüm KTM/Husqvarna modelleri) TFT ekranına yansıtan ve motosikletin **sol kütüğündeki 8 fiziksel tuşla (SET, BACK, UP, DOWN, LEFT, RIGHT, C1, C2)** telefon kontrollerini (Müzik, Ses Seviyesi, Sesli Asistan vb.) sağlayan açık kaynaklı Android uygulamasıdır.

---

## 🏍️ Özellikler

1. **Google Maps & Yandex Navigasyon Entegrasyonu:**
   - Harita uygulamasında başlatılan rota için bir sonraki dönüş ikonu, dönüş mesafesi (örn: `"250 m"`), girilecek sokak/cadde adı, tahmini varış süresi (ETA) ve kalan toplam mesafe anlık olarak TFT ekranda gösterilir.
2. **KTM Turn-by-Turn Plus Protokolü:**
   - Orijinal KTM BLE GATT protokolü (`0x0700` TBT Servisi) ve 16-bayt framing/padding algoritmasıyla tam uyumlu çalışır.
3. **8 Yönlü Gidon Tuşları Yönetimi (RCM):**
   - `SET`, `BACK`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `C1`, `C2` tuşlarına istediğiniz eylemi atayın (Müzik Çal/Durdur, Sonraki/Önceki Parça, Ses Artır/Azalt, Sesli Asistan).
4. **Canlı TFT Simülatörü:**
   - Uygulama içinde KTM TFT ekranının birebir grafik önizlemesi bulunur; motosiklet yanında değilken bile simülasyon yapılabilir.
5. **Otomatik Arka Plan Servisi:**
   - Motosiklet kontağı açıldığında BLE üzerinden otomatik bağlanır ve harita açıldığında otomatik olarak ekrana yönlendirmeleri iletir.

---

## 📂 Proje Mimarisi

```text
com.tbtktm/
├── ble/
│   ├── KtmGattAttributes.kt      # TBT (0x0700), RCM (0x0100), Base (0x0000) UUID'leri
│   ├── KtmFramingUtils.kt        # 16-bayt Salt + Padding Çerçeveleme motoru
│   ├── KtmBleManager.kt          # BluetoothGatt bağlantı & komut kuyruk yöneticisi
│   └── KtmBleService.kt          # Arka plan Foreground Servisi
├── parser/
│   ├── NotificationParserService # Android NotificationListenerService
│   ├── GoogleMapsParser.kt       # Google Maps bildirim ayrıştırıcı
│   ├── YandexNaviParser.kt       # Yandex Navigasyon bildirim ayrıştırıcı
│   └── IconMatcher.kt            # Metin -> KTM 57 İkon ID eşleştirici
├── rcm/
│   ├── HandlebarKeyManager.kt     # Tuş basış analizcisi (8 Buton desteği)
│   └── ActionDispatcher.kt       # Medya / Ses seviyesi tetikleyici
├── model/
│   ├── KtmTurnIcon.kt            # KTM 57 manevra ikonu enum listesi
│   ├── HandlebarButton.kt        # 8 buton & atanabilir eylem modelleri
│   └── NavigationData.kt         # Canlı gösterge verisi
└── ui/
    ├── screens/
    │   ├── DashboardScreen.kt    # Canlı TFT önizleme ve durum ekranı
    │   ├── DeviceScanScreen.kt   # BLE cihaz arama ve tek dokunuşla bağlanma
    │   └── KeyMappingScreen.kt   # Gidon tuşları atama menüsü
    └── components/
        └── TftDashboardCard.kt   # KTM 1290 TFT HMI arayüz bileşeni
```

---

## 🛠️ Kurulum ve Derleme

1. Bu projeyi **Android Studio** (Giraffe / Hedgehog / Iguana veya üzeri) ile açın.
2. `build.gradle.kts` dosyalarının senkronizasyonunu (Gradle Sync) tamamlayın.
3. Android telefonunuza yükleyin (`Run 'app'`).

### Gerekli İzinler:
1. **Bluetooth ve Konum İzni:** Uygulama ilk açılışta sorar.
2. **Bildirim Erişimi (Notification Access):**
   - *Ayarlar -> Uygulamalar ve Bildirimler -> Özel Uygulama Erişimi -> Bildirim Erişimi* menüsünden **TbTKTM** uygulamasına izin verin. (Google Maps ve Yandex yönlendirmelerini okumak için şarttır).

---

## 🚀 Kullanım

1. **TbTKTM** uygulamasını açın ve **Motosiklet** sekmesinden KTM motosikletinizi seçip bağlanın.
2. **Gidon Tuşları** sekmesinden sol kütükteki tuşların ne yapmasını istediğinizi ayarlayın.
3. **Google Maps** veya **Yandex Navigasyon** uygulamasında bir rota başlatın.
4. KTM 1290 TFT ekranınızda dönüş okları, mesafeler ve cadde adları anlık olarak görünecektir!
