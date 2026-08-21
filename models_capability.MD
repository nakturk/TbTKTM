# KTM & Husqvarna Motosiklet Modelleri Donanım ve Özellik Uyumluluk Kataloğu (Models Capability Matrix)

Bu belge, **KTMconnect (v4.0.2)** kaynak kodları, `bmdb-data` (Bike Model Database), `DASH_Meter_Info_1104.java` (`DASH_Variant` kodları), `CUKT_bCCU.json` ve `CUKTCUO_master.json` telemetri şemalarının tersine mühendislik analizine dayanan **resmi model yetenekleri ve protokol uyumluluk matrisidir**.

---

## 1. Genel Model & Özellik Uyumluluk Matrisi

| Model Ailesi | Gösterge / Ünite | TBT Plus Navigasyon (`0xCC4D`) | Kayan Yazı Bildirim (Marquee) | Gidon Kumandası (`RCM / SCA`) | 6-Eksen IMU (Yatış / Pitch Açısı) | Fren Hidrolik Basıncı (`ABS Bar`) | Lastik Basıncı (`TPMS mbar`) | CUO Motor Haritası & WP Telemetri |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **KTM 1290 Super Adventure S / R / T** *(2017-2024+)* | 7" TFT / BCCU | ✅ **Tam Uyumlu** | ✅ **Tam Uyumlu** | ✅ **8 Buton (Tam Kütük + C1/C2)** | ✅ **Bosch 6D IMU (0x12F)** | ✅ **Ön Fren (0x290)** | ✅ **Ön / Arka (0x310)** | ❌ *(Cadde Modeli)* |
| **KTM 1290 Super Duke R / GT & 1390 Super Duke** | 5"-7" TFT / BCCU | ✅ **Tam Uyumlu** | ✅ **Tam Uyumlu** | ✅ **8 Buton (Tam Kütük)** | ✅ **Bosch 6D IMU (0x12F)** | ✅ **Ön Fren (0x290)** | ✅ **GT Standart / R Ops.** | ❌ *(Cadde Modeli)* |
| **KTM 890 / 790 Adventure (R/Rally) & Norden 901** | 5" TFT / BCCU | ✅ **Tam Uyumlu** | ✅ **Tam Uyumlu** | 🟡 **6 Buton (SET/BACK/D-Pad)** | ✅ **Bosch 6D IMU (0x12F)** | 🟡 **Seçili Versiyonlar** | 🟡 **Opsiyonel** | ❌ *(Cadde Modeli)* |
| **KTM 890 / 790 / 990 Duke** | 5" TFT / BCCU | ✅ **Tam Uyumlu** | ✅ **Tam Uyumlu** | 🟡 **6 Buton (SET/BACK/D-Pad)** | ✅ **Bosch 6D IMU (0x12F)** | 🟡 **Seçili Versiyonlar** | ❌ | ❌ *(Cadde Modeli)* |
| **KTM 390 Adventure / 250 Adventure** *(2020-2024+)* | 5" TFT / bCCU | ✅ **Uyumlu** | ✅ **Uyumlu** | 🟡 **4 Buton (D-Pad)** | ❌ *(IMU Yok)* | ❌ | ❌ | ❌ *(Cadde Modeli)* |
| **KTM 125 / 250 / 390 Duke & RC 125/200/390** | TFT / bCCU | ✅ **Uyumlu** | ✅ **Uyumlu** | 🟡 **4 Buton (D-Pad)** | ❌ *(IMU Yok)* | ❌ | ❌ | ❌ *(Cadde Modeli)* |
| **Husqvarna Svartpilen / Vitpilen 125/250/401/701** | TFT / bCCU | ✅ **Uyumlu** | ✅ **Uyumlu** | 🟡 **4 Buton (D-Pad)** | 🟡 *(Sadece 701)* | ❌ | ❌ | ❌ *(Cadde Modeli)* |
| **KTM 250/350/450 SX-F, XC-F & Husq. FC/FX** | CUO Ünitesi (TFT Yok) | ❌ *(TFT Gösterge Yok)* | ❌ | ❌ | ✅ *(10Hz GNSS + IMU)* | ❌ | ❌ | ✅ **Tam Uyumlu (Wi-Fi/BT)** |

---

## 2. Model Gruplarına Göre Detaylı İnceleme

### 🏍️ Grup 1: Amiral Gemisi & Seyahat (KTM 1290 / 1390 Serisi)
*Kaynak Kod Tanımlayıcıları: `DASH_Variant` 604, 607, 609, 614, 615, 616, 617, 619*

* **Kapsanan Modeller:**
  - KTM 1290 Super Adventure S / R / T (2017 – 2024+)
  - KTM 1290 Super Duke R / Super Duke GT (2017 – 2024+)
  - KTM 1390 Super Duke R / Evo (2024+)
* **Donanım Altyapısı:** 7 inç geniş renkli TFT HMI, tam donanımlı BCCU Bluetooth ünitesi, Bosch 6-Eksen Ataletsel Ölçüm Ünitesi (IMU), Cornering ABS (C-ABS), Motor Çekiş Kontrolü (MTC), Lastik Basınç İzleme Sistemi (TPMS).
* **Desteklenen Özellikler:**
  - **TBT Plus & Kayan Yazı:** Geniş TFT ekranda 57 manevra ikonu, sayaç geri sayımı, sokak/cadde isimleri, ETA ve WhatsApp/Telegram/SMS kayan yazı bildirimleri (Marquee Ticker).
  - **8 Butonlu Gidon Kütüğü (RCM):** `SET`, `BACK`, `UP`, `DOWN`, `LEFT`, `RIGHT` ve programlanabilir **`C1`**, **`C2`** kısayol tuşları.
  - **Derin CAN Telemetrisi:**
    - `0x12F`: Dinamik Yatış Açısı (`ABS_Lean_Angle` - Derece), Pitch Açısı (`ABS_Pitch_Angle` - Wheelie tespiti).
    - `0x290`: Ön Fren Hidrolik Basıncı (`ABS_Front_Wheel_Press` - Bar).
    - `0x120` & `0x129`: Motor Devri (RPM), Gaz Açıklığı (TPS %), Vites (`0=N`, `1..6`), Quickshifter+ durumu, Hız Sabitleyici (Cruise Control) hedef hızı.
    - `0x310`: Ön ve Arka Lastik Basıncı (`TPMS_Pressure_Front/Rear` - mbar) ve lastik içi hava sıcaklığı (°C).

---

### 🧭 Grup 2: Orta Segment Macera & Naked (KTM 790 / 890 / 990 & Husqvarna 901)
*Kaynak Kod Tanımlayıcıları: `DASH_Variant` 635, 636, 637, 641, Husqvarna Norden 901*

* **Kapsanan Modeller:**
  - KTM 790 Adventure / 790 Adventure R / Rally
  - KTM 890 Adventure / 890 Adventure R / Rally
  - KTM 790 Duke / 890 Duke / 890 Duke R
  - KTM 990 Duke (2024+)
  - Husqvarna Norden 901 / Norden 901 Expedition
* **Donanım Altyapısı:** 5 inç TFT gösterge, standart BCCU ünitesi, Bosch 6D IMU ve C-ABS.
* **Desteklenen Özellikler:**
  - **TBT Plus & Marquee Ticker:** 5 inç ekranda tam KMRC navigasyon ve mesaj akışı.
  - **Gidon Kumandası:** Sol elcikte `C1/C2` bulunmaz; 4 yönlü D-Pad (`UP`, `DOWN`, `LEFT`, `RIGHT`) ve `SET`, `BACK` olmak üzere 6 buton aktiftir.
  - **Telemetri:** Yatış Açısı, Pitch Açısı, TPS, Devir, Hız, Vites ve Motor Hararet telemetrisi eksiksiz okunur.

---

### 🛵 Grup 3: Hafif & Giriş Segmenti (KTM 125 / 250 / 390 Duke, RC & 390 Adventure)
*Kaynak Kod Tanımlayıcıları: `DASH_Variant` 930, 933, 935, 937, 940, 943, 945, 946, 947, 948, 957, 958, 960, 962, 963, 281, 284, 285, 286, 288, 289*

* **Kapsanan Modeller:**
  - KTM 390 Adventure / 250 Adventure (MY19+)
  - KTM 125 Duke / 250 Duke / 390 Duke (MY17+)
  - KTM RC 125 / RC 200 / RC 250 / RC 390 (MY16, MY21+)
  - Husqvarna Svartpilen 125/250/401 & Vitpilen 125/250/401
* **Donanım Altyapısı:** bCCU (Lite BLE ünitesi) ve TFT / LCD gösterge. Bu modellerde 6-eksen IMU ve hidrolik fren basınç sensörü **bulunmaz**.
* **Desteklenen Özellikler:**
  - **Navigasyon:** Turn-by-Turn yönlendirme okları, mesafeler ve cadde adları göstergede başarıyla görüntülenir.
  - **Gidon Kumandası:** 4 yönlü D-Pad butonlarıyla temel medya kontrolleri (Müzik çal/duraklat, sonraki parça, ses) yönetilebilir.
  - **Telemetri:** Motor Devri (RPM), Hız, Vites (`ECU_Gear_Position`), Soğutma Suyu Sıcaklığı ve Kalan Yakıt verileri okunabilir; **Yatış Açısı ve Fren Basıncı sinyalleri donanımsal sensör eksikliği nedeniyle mevcut değildir**.

---

### 🏁 Grup 4: Motokros ve Enduro Grubu (KTM SX-F / XC-F & Husqvarna FC / FX Serisi)
*Kaynak Kod Şeması: `CUKTCUO_masterv91.00.json` & `bmdb-data/bikemodels`*

* **Kapsanan Modeller:**
  - KTM 250 SX-F / 350 SX-F / 450 SX-F (2020 – 2024+)
  - KTM 250 XC-F / 350 XC-F / 450 XC-F (2021 – 2024+)
  - Husqvarna FC 250 / FC 350 / FC 450 & FX 350 / FX 450 (2020 – 2024+)
  - GASGAS MC 250F / MC 450F (Factory Edition)
* **Donanım Altyapısı:** CUO (Connectivity Unit Offroad) Wi-Fi & Bluetooth modülü. Motosiklet üzerinde TFT gösterge ekranı **bulunmaz**.
* **Özel Yetenekler:**
  - **10Hz Hassas GNSS:** Tur zamanlama, pist çizgi analizi, hızlanma ve frenleme bölgeleri analizi.
  - **WP Süspansiyon Telemetrisi:** Ön Amortisör Hava Basıncı (ID `45` - mbar) ve Arka Salıncak Sag Voltajı (ID `46` - mV).
  - **ECU Motor Haritalama (Kablosuz):** Akıllı telefondan ateşleme zamanlaması (Avans), yakıt püskürtme haritaları, Launch Control ve Quickshifter kesme süreleri (`qsIGCut`) programlama.

---

## 3. Donanım Mimarisi: BCCU vs bCCU vs CUO Karşılaştırması

| Özellik / Bileşen | BCCU (Street / Adventure / Travel) | bCCU (Duke 390 / RC 390 / Adv 390) | CUO (Offroad / Motocross / Enduro) |
| :--- | :---: | :---: | :---: |
| **Bağlantı Türü** | Bluetooth Classic (RFCOMM) + BLE | Bluetooth Low Energy (BLE) | Wi-Fi 2.4GHz + Bluetooth |
| **Ana Navigasyon Kanalı** | `EXTENDED_TBT` (Port 52301 / `0xCC4D`) | TBT GATT Service (`0x0700`) | Yok (TFT Ekranı Yok) |
| **Harita Akışı Desteği** | `MAPNAV_STREAMING` (Port 52410) | Yok | Yok |
| **Gidon Tuş Yakalama** | SCA / RCM GATT (`0x0100` / `0x0103`) | SCA GATT (`0x0100`) | Yok (Gidon Kütüğü Yok) |
| **Telemetri Veri Yolu** | CAN-Bus ➔ RFCOMM Port 52070 (pRPC) | CAN-Bus ➔ BLE GATT | Wi-Fi TCP Port 52070 + 10Hz GNSS |
| **IMU / Yatış Açısı** | Var (Bosch 6D Sensör) | Yok | Var (Pist Analiz Sensörü) |
| **ECU Harita Ayarı** | Yok (Fabrika Kilitli) | Yok (Fabrika Kilitli) | Var (Kullanıcı Ayarlı Ateşleme/Yakıt) |

---

## 4. `DASH_Variant` Kaynak Kod Tablosu Referansı

Aşağıdaki liste `com.ktm.mob.can.messages.generated.DASH_Meter_Info_1104` içerisinden doğrudan çözümlenen fabrika varyant kodlarıdır:

```
[ID 1]  603 R - 1190 Adventure R
[ID 2]  603 T - 1190 Adventure
[ID 3]  604   - 1290 Super Adventure (MY15-16)
[ID 5]  614   - 1290 Super Duke GT
[ID 16] 616   - 1290 SuperDuke R (MY17)
[ID 17] 641   - 790 Duke
[ID 18] 607   - 1290 Adventure (MY17)
[ID 19] 607 R - 1290 Adventure R (MY17)
[ID 20] 609   - 1290 Super Adventure (MY18)
[ID 24] 635   - 790 Adventure
[ID 25] 635 R - 790 Adventure R
[ID 26] 615   - 1290 Super Adventure
[ID 27] 958   - 390 Adventure (MY19)
[ID 29] 617   - 1290 Super Duke R (MY20)
[ID 41] 637   - 890 Adventure S
[ID 42] 619   - 1290 Super Adventure S (MY21+)
[ID 43] 619 R - 1290 Super Adventure R (MY21+)
[ID 45] 637 R - 890 Adventure R
[ID 50] 636   - 890 Duke (MY20+)
[ID 51] 963   - 390 Duke / 960 - 125 Duke (MY24)
```

---

## 5. Sonuç ve TbTKTM Uygulaması İçin Çıkarımlar

1. **Evrensel TFT Desteği:** **TbTKTM** uygulaması, 125cc'den 1390cc'ye kadar **TFT ekrana sahip olan istisnasız tüm KTM ve Husqvarna modellerinde** Turn-by-Turn navigasyon ve mesaj kaydırma (Marquee Ticker) işlevini eksiksiz yerine getirir.
2. **Maksimum Donanım Verimi:** `C1/C2` özel fonksiyon tuşları, 6-eksen IMU yatış açısı ve fren hidrolik basıncı telemetrisi en zengin şekilde **KTM 1290 Super Adventure / Super Duke** ve **890 Adventure / Norden 901** modellerinde çalışmaktadır.
