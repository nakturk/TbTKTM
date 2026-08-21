package com.tbtktm.i18n

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class AppStrings(
    // App Header & Tabs
    val appTitle: String,
    val appSubtitle: String,
    val tabDashboard: String,
    val tabMotorcycle: String,
    val tabHandlebar: String,
    
    // Connection Card
    val connectionCardTitle: String,
    val connectedStatus: String,
    val disconnectedStatus: String,
    val btnManage: String,
    val btnConnect: String,
    
    // VIN Card
    val vinCardTitle: String,
    val vinPlaceholder: String,
    val btnSaveVin: String,
    val btnSaved: String,
    
    // TFT Display Simulation Card
    val tftDisplayHeader: String,
    val tftGpsActive: String,
    val tftStandby: String,
    val tftNextTurn: String,
    val tftTargetDistance: String,
    val tftEta: String,
    val tftNavStandbyMessage: String,
    
    // Action Buttons
    val btnPushTestData: String,
    val btnStop: String,
    val btnSimulateMotorcycle: String,
    val btnTestWhatsAppTicker: String,
    
    // Live Logs
    val liveLogsTitle: String,
    val btnClearLogs: String,
    val noLogsYet: String,
    
    // Notification Permission
    val notifPermissionRequired: String,
    val notifPermissionGranted: String,
    val btnGrantPermission: String,
    
    // Handlebar Screen
    val handlebarSettingsTitle: String,
    val handlebarSettingsDesc: String,
    val actionNone: String,
    val actionPlayPause: String,
    val actionNextTrack: String,
    val actionPrevTrack: String,
    val actionVolumeUp: String,
    val actionVolumeDown: String,
    val actionVoiceAssistant: String,
    val actionRecenterMap: String,
    val actionAnswerCall: String,
    
    // Device Scan Screen
    val scanScreenTitle: String,
    val scanScreenSubtitle: String,
    val btnStartScan: String,
    val btnStopScan: String,
    val pairedDevicesHeader: String,
    val discoveredDevicesHeader: String,
    val noDiscoveredDevices: String,
    val btnDisconnect: String,
    val unknownDevice: String,
    val pairedTag: String,
    
    // Language Dialog
    val languageSelectorTitle: String
)

object AppLanguageManager {

    private const val PREFS_NAME = "tbtktm_i18n_prefs"
    private const val KEY_LANG = "selected_language_code"

    private lateinit var prefs: SharedPreferences

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _strings = MutableStateFlow(getStringsForLanguage(AppLanguage.ENGLISH))
    val strings: StateFlow<AppStrings> = _strings.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString(KEY_LANG, null)

        val initialLang = if (savedLangCode != null) {
            AppLanguage.fromCode(savedLangCode)
        } else {
            detectDefaultLanguage()
        }

        setLanguage(initialLang)
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        _strings.value = getStringsForLanguage(lang)
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_LANG, lang.code).apply()
        }
    }

    private fun detectDefaultLanguage(): AppLanguage {
        val sysLang = Locale.getDefault().language.lowercase()
        return when (sysLang) {
            "tr" -> AppLanguage.TURKISH
            "it" -> AppLanguage.ITALIAN
            "es" -> AppLanguage.SPANISH
            "el" -> AppLanguage.GREEK
            else -> AppLanguage.ENGLISH
        }
    }

    fun getStringsForLanguage(lang: AppLanguage): AppStrings {
        return when (lang) {
            AppLanguage.TURKISH -> AppStrings(
                appTitle = "KTM 1290 TBT NAV & BİLDİRİM",
                appSubtitle = "KTMconnect KMRC RFCOMM • Donanım Sürümü",
                tabDashboard = "Gösterge",
                tabMotorcycle = "Motosiklet",
                tabHandlebar = "Gidon",
                connectionCardTitle = "Motosiklet Bağlantısı",
                connectedStatus = "RFCOMM Aktif • TBT Hazır",
                disconnectedStatus = "Bağlantı Yok • Motosiklete bağlanmak için dokunun",
                btnManage = "Yönet",
                btnConnect = "Bağlan",
                vinCardTitle = "KTM 1290 ŞASİ NUMARASI (VIN) & GİRİŞ",
                vinPlaceholder = "Şasi No (VIN) veya Şifre",
                btnSaveVin = "Kaydet & Eşleş",
                btnSaved = "Kaydedildi",
                tftDisplayHeader = "KTM 1290 TFT GÖSTERGE",
                tftGpsActive = "GPS AKTİF",
                tftStandby = "BEKLEMEDE",
                tftNextTurn = "SONRAKİ DÖNÜŞ",
                tftTargetDistance = "HEDEF MESAFESİ",
                tftEta = "VARIŞ",
                tftNavStandbyMessage = "TFT Navigasyon Beklemede\nGoogle Maps'te bir rota başlatın",
                btnPushTestData = "Test Verisi Bas",
                btnStop = "Durdur",
                btnSimulateMotorcycle = "🧪 Sanal Motosiklet Doğrulama Testi Yap",
                btnTestWhatsAppTicker = "💬 WhatsApp Kayan Yazı Testi Gönder (TFT)",
                liveLogsTitle = "CANLI BİLDİRİM & BLE LOGLARI",
                btnClearLogs = "Temizle",
                noLogsYet = "Henüz log kaydı yok...\nGoogle Maps'te rota açın veya Test Verisi Bas'a tıklayın.",
                notifPermissionRequired = "BİLDİRİM ERİŞİMİ GEREKLİ",
                notifPermissionGranted = "Bildirim Erişimi Açık",
                btnGrantPermission = "İzin Ver",
                handlebarSettingsTitle = "KTM GİDON TUŞLARI YAPILANDIRMASI",
                handlebarSettingsDesc = "Motosikletin sol kütüğündeki butonlara basıldığında telefonun gerçekleştireceği eylemleri seçin.",
                actionNone = "İşlev Yok",
                actionPlayPause = "Müziği Çal / Durdur",
                actionNextTrack = "Sonraki Şarkı",
                actionPrevTrack = "Önceki Şarkı",
                actionVolumeUp = "Ses Artır",
                actionVolumeDown = "Ses Azalt",
                actionVoiceAssistant = "Sesli Asistan (Google)",
                actionRecenterMap = "Haritayı Ortala",
                actionAnswerCall = "Aramayı Cevapla",
                scanScreenTitle = "KTM BLUETOOTH EŞLEŞTİRME",
                scanScreenSubtitle = "KTM 1290 CCU / TFT Bluetooth Cihazını Seçin",
                btnStartScan = "Cihazları Tara",
                btnStopScan = "Taramayı Durdur",
                pairedDevicesHeader = "EŞLEŞMİŞ CİHAZLAR",
                discoveredDevicesHeader = "BULUNAN BLUETOOTH CİHAZLAR",
                noDiscoveredDevices = "Henüz cihaz bulunamadı. Taramayı başlatın.",
                btnDisconnect = "Bağlantıyı Kes",
                unknownDevice = "Bilinmeyen Cihaz",
                pairedTag = "Eşleşti",
                languageSelectorTitle = "Dil Seçimi"
            )
            AppLanguage.ENGLISH -> AppStrings(
                appTitle = "KTM 1290 TBT NAV & NOTIFIER",
                appSubtitle = "KTMconnect KMRC RFCOMM • Hardware Edition",
                tabDashboard = "Dashboard",
                tabMotorcycle = "Motorcycle",
                tabHandlebar = "Handlebar",
                connectionCardTitle = "Motorcycle Connection",
                connectedStatus = "RFCOMM Active • TBT Ready",
                disconnectedStatus = "Not Connected • Tap to connect to motorcycle",
                btnManage = "Manage",
                btnConnect = "Connect",
                vinCardTitle = "KTM 1290 VIN & LOGIN CONFIGURATION",
                vinPlaceholder = "VIN or Bluetooth PIN",
                btnSaveVin = "Save & Pair",
                btnSaved = "Saved",
                tftDisplayHeader = "KTM 1290 TFT DISPLAY",
                tftGpsActive = "GPS ACTIVE",
                tftStandby = "STANDBY",
                tftNextTurn = "NEXT TURN",
                tftTargetDistance = "DESTINATION DISTANCE",
                tftEta = "ETA",
                tftNavStandbyMessage = "TFT Navigation Standby\nStart a route in Google Maps",
                btnPushTestData = "Send Test Data",
                btnStop = "Stop",
                btnSimulateMotorcycle = "🧪 Run Virtual Motorcycle Verification Test",
                btnTestWhatsAppTicker = "💬 Send WhatsApp Marquee Ticker Test (TFT)",
                liveLogsTitle = "LIVE NOTIFICATION & BLE LOGS",
                btnClearLogs = "Clear",
                noLogsYet = "No logs yet...\nStart a route in Google Maps or tap Send Test Data.",
                notifPermissionRequired = "NOTIFICATION LISTENER PERMISSION REQUIRED",
                notifPermissionGranted = "Notification Access Granted",
                btnGrantPermission = "Grant Access",
                handlebarSettingsTitle = "KTM HANDLEBAR SWITCHES CONFIGURATION",
                handlebarSettingsDesc = "Select actions for your phone when left handlebar switch buttons are pressed.",
                actionNone = "No Action",
                actionPlayPause = "Play / Pause Media",
                actionNextTrack = "Next Track",
                actionPrevTrack = "Previous Track",
                actionVolumeUp = "Volume Up",
                actionVolumeDown = "Volume Down",
                actionVoiceAssistant = "Voice Assistant (Google)",
                actionRecenterMap = "Recenter Map",
                actionAnswerCall = "Answer Call",
                scanScreenTitle = "KTM BLUETOOTH PAIRING",
                scanScreenSubtitle = "Select your KTM 1290 CCU / TFT Bluetooth Device",
                btnStartScan = "Scan Devices",
                btnStopScan = "Stop Scan",
                pairedDevicesHeader = "PAIRED DEVICES",
                discoveredDevicesHeader = "DISCOVERED BLUETOOTH DEVICES",
                noDiscoveredDevices = "No devices found yet. Tap Scan Devices to search.",
                btnDisconnect = "Disconnect",
                unknownDevice = "Unknown Device",
                pairedTag = "Paired",
                languageSelectorTitle = "Select Language"
            )
            AppLanguage.ITALIAN -> AppStrings(
                appTitle = "KTM 1290 NAV TBT & NOTIFICHE",
                appSubtitle = "KTMconnect KMRC RFCOMM • Versione Hardware",
                tabDashboard = "Cruscotto",
                tabMotorcycle = "Moto",
                tabHandlebar = "Manubrio",
                connectionCardTitle = "Connessione Moto",
                connectedStatus = "RFCOMM Attivo • TBT Pronto",
                disconnectedStatus = "Non Connesso • Tocca per connettere la moto",
                btnManage = "Gestisci",
                btnConnect = "Connetti",
                vinCardTitle = "CONFIGURAZIONE VIN KTM 1290 & LOGIN",
                vinPlaceholder = "VIN o PIN Bluetooth",
                btnSaveVin = "Salva & Abbina",
                btnSaved = "Salvato",
                tftDisplayHeader = "DISPLAY TFT KTM 1290",
                tftGpsActive = "GPS ATTIVO",
                tftStandby = "STANDBY",
                tftNextTurn = "PROSSIMA SVOLTA",
                tftTargetDistance = "DISTANZA DESTINAZIONE",
                tftEta = "ARRIVO",
                tftNavStandbyMessage = "Navigazione TFT in Standby\nAvvia un percorso su Google Maps",
                btnPushTestData = "Invia Dati di Prova",
                btnStop = "Ferma",
                btnSimulateMotorcycle = "🧪 Esegui Test Simulatore Moto Virtuale",
                btnTestWhatsAppTicker = "💬 Test Testo Scorrevole WhatsApp (TFT)",
                liveLogsTitle = "LOG IN TEMPO REALE & BLE",
                btnClearLogs = "Cancella",
                noLogsYet = "Nessun log presente...\nAvvia un percorso in Google Maps o premi Invia Dati di Prova.",
                notifPermissionRequired = "ACCESSO ALLE NOTIFICHE RICHIESTO",
                notifPermissionGranted = "Accesso Notifiche Attivo",
                btnGrantPermission = "Consenti",
                handlebarSettingsTitle = "CONFIGURAZIONE PULSANTI MANUBRIO KTM",
                handlebarSettingsDesc = "Seleziona le azioni del telefono quando vengono premuti i pulsanti al manubrio.",
                actionNone = "Nessuna Azione",
                actionPlayPause = "Riproduci / Pausa Musica",
                actionNextTrack = "Traccia Successiva",
                actionPrevTrack = "Traccia Precedente",
                actionVolumeUp = "Alza Volume",
                actionVolumeDown = "Abbassa Volume",
                actionVoiceAssistant = "Assistente Vocale (Google)",
                actionRecenterMap = "Ricentra Mappa",
                actionAnswerCall = "Rispondi alla Chiamata",
                scanScreenTitle = "ACCOPPIAMENTO BLUETOOTH KTM",
                scanScreenSubtitle = "Seleziona il dispositivo Bluetooth KTM 1290 CCU / TFT",
                btnStartScan = "Scansiona Dispositivi",
                btnStopScan = "Ferma Scansione",
                pairedDevicesHeader = "DISPOSITIVI ACCOPPIATI",
                discoveredDevicesHeader = "DISPOSITIVI BLUETOOTH RILEVATI",
                noDiscoveredDevices = "Nessun dispositivo rilevato. Avvia la scansione.",
                btnDisconnect = "Disconnetti",
                unknownDevice = "Dispositivo Sconosciuto",
                pairedTag = "Accoppiato",
                languageSelectorTitle = "Seleziona Lingua"
            )
            AppLanguage.SPANISH -> AppStrings(
                appTitle = "KTM 1290 NAV TBT & NOTIFICACIONES",
                appSubtitle = "KTMconnect KMRC RFCOMM • Edición Hardware",
                tabDashboard = "Panel",
                tabMotorcycle = "Moto",
                tabHandlebar = "Manillar",
                connectionCardTitle = "Conexión con la Moto",
                connectedStatus = "RFCOMM Activo • TBT Listo",
                disconnectedStatus = "No Conectado • Toca para conectar con la moto",
                btnManage = "Gestionar",
                btnConnect = "Conectar",
                vinCardTitle = "CONFIGURACIÓN DE VIN KTM 1290 & LOGIN",
                vinPlaceholder = "VIN o PIN Bluetooth",
                btnSaveVin = "Guardar & Vincular",
                btnSaved = "Guardado",
                tftDisplayHeader = "PANTALLA TFT KTM 1290",
                tftGpsActive = "GPS ACTIVO",
                tftStandby = "EN ESPERA",
                tftNextTurn = "SIGUIENTE GIRO",
                tftTargetDistance = "DISTANCIA AL DESTINO",
                tftEta = "LLEGADA",
                tftNavStandbyMessage = "Navegación TFT en Espera\nInicia una ruta en Google Maps",
                btnPushTestData = "Enviar Datos de Prueba",
                btnStop = "Detener",
                btnSimulateMotorcycle = "🧪 Ejecutar Prueba de Simulación de Moto",
                btnTestWhatsAppTicker = "💬 Prueba de Texto Desplazable WhatsApp (TFT)",
                liveLogsTitle = "REGISTROS EN VIVO & BLE",
                btnClearLogs = "Borrar",
                noLogsYet = "No hay registros aún...\nInicia una ruta en Google Maps o pulsa Enviar Datos de Prueba.",
                notifPermissionRequired = "PERMISO DE ACCESO A NOTIFICACIONES REQUERIDO",
                notifPermissionGranted = "Acceso a Notificaciones Concedido",
                btnGrantPermission = "Conceder Permiso",
                handlebarSettingsTitle = "CONFIGURACIÓN DE MANDOS DEL MANILLAR KTM",
                handlebarSettingsDesc = "Selecciona las acciones del teléfono al pulsar los botones de la piña izquierda.",
                actionNone = "Sin Acción",
                actionPlayPause = "Reproducir / Pausar Música",
                actionNextTrack = "Siguiente Pista",
                actionPrevTrack = "Pista Anterior",
                actionVolumeUp = "Subir Volumen",
                actionVolumeDown = "Bajar Volumen",
                actionVoiceAssistant = "Asistente de Voz (Google)",
                actionRecenterMap = "Recentrar Mapa",
                actionAnswerCall = "Responder Llamada",
                scanScreenTitle = "EMPAREJAMIENTO BLUETOOTH KTM",
                scanScreenSubtitle = "Selecciona tu dispositivo Bluetooth KTM 1290 CCU / TFT",
                btnStartScan = "Buscar Dispositivos",
                btnStopScan = "Detener Búsqueda",
                pairedDevicesHeader = "DISPOSITIVOS VINCULADOS",
                discoveredDevicesHeader = "DISPOSITIVOS BLUETOOTH DETECTADOS",
                noDiscoveredDevices = "No se encontraron dispositivos aún. Pulsa Buscar Dispositivos.",
                btnDisconnect = "Desconectar",
                unknownDevice = "Dispositivo Desconocido",
                pairedTag = "Vinculado",
                languageSelectorTitle = "Seleccionar Idioma"
            )
            AppLanguage.GREEK -> AppStrings(
                appTitle = "KTM 1290 TBT NAV & ΕΙΔΟΠΟΙΗΣΕΙΣ",
                appSubtitle = "KTMconnect KMRC RFCOMM • Έκδοση Υλικού",
                tabDashboard = "Ταμπλό",
                tabMotorcycle = "Μοτοσυκλέτα",
                tabHandlebar = "Τιμόνι",
                connectionCardTitle = "Σύνδεση Μοτοσυκλέτας",
                connectedStatus = "RFCOMM Ενεργό • TBT Έτοιμο",
                disconnectedStatus = "Μη Συνδεδεμένο • Πατήστε για σύνδεση",
                btnManage = "Διαχείριση",
                btnConnect = "Σύνδεση",
                vinCardTitle = "ΡΥΘΜΙΣΗ VIN & ΣΥΝΔΕΣΗΣ KTM 1290",
                vinPlaceholder = "VIN ή PIN Bluetooth",
                btnSaveVin = "Αποθήκευση & Σύζευξη",
                btnSaved = "Αποθηκεύτηκε",
                tftDisplayHeader = "ΟΘΟΝΗ TFT KTM 1290",
                tftGpsActive = "GPS ΕΝΕΡΓΟ",
                tftStandby = "ΑΝΑΜΟΝΗ",
                tftNextTurn = "ΕΠΟΜΕΝΗ ΣΤΡΟΦΗ",
                tftTargetDistance = "ΑΠΟΣΤΑΣΗ ΠΡΟΟΡΙΣΜΟΥ",
                tftEta = "ΑΦΙΞΗ",
                tftNavStandbyMessage = "Πλοήγηση TFT σε Αναμονή\nΞεκινήστε διαδρομή στο Google Maps",
                btnPushTestData = "Αποστολή Δοκιμαστικών Δεδομένων",
                btnStop = "Διακοπή",
                btnSimulateMotorcycle = "🧪 Εκτέλεση Ελέγχου Προσομοιωτή",
                btnTestWhatsAppTicker = "💬 Δοκιμή Κυλιόμενου Κειμένου WhatsApp (TFT)",
                liveLogsTitle = "ΖΩΝΤΑΝΑ ΑΡΧΕΙΑ ΚΑΤΑΓΡΑΦΗΣ & BLE",
                btnClearLogs = "Καθαρισμός",
                noLogsYet = "Δεν υπάρχουν αρχεία καταγραφής...\nΞεκινήστε διαδρομή στο Google Maps ή πατήστε Αποστολή.",
                notifPermissionRequired = "ΑΠΑΙΤΕΙΤΑΙ ΑΔΕΙΑ ΠΡΟΣΒΑΣΗΣ ΕΙΔΟΠΟΙΗΣΕΩΝ",
                notifPermissionGranted = "Πρόσβαση Ειδοποιήσεων Ενεργή",
                btnGrantPermission = "Παραχώρηση Άδειας",
                handlebarSettingsTitle = "ΔΙΑΜΟΡΦΩΣΗ ΔΙΑΚΟΠΤΩΝ ΤΙΜΟΝΙΟΥ KTM",
                handlebarSettingsDesc = "Επιλέξτε ενέργειες του τηλεφώνου όταν πατιούνται τα κουμπιά του αριστερού διακόπτη.",
                actionNone = "Καμία Ενέργεια",
                actionPlayPause = "Αναπαραγωγή / Παύση Μουσικής",
                actionNextTrack = "Επόμενο Κομμάτι",
                actionPrevTrack = "Προηγούμενο Κομμάτι",
                actionVolumeUp = "Αύξηση Έντασης",
                actionVolumeDown = "Μείωση Έντασης",
                actionVoiceAssistant = "Φωνητικός Βοηθός (Google)",
                actionRecenterMap = "Επανακεντράρισμα Χάρτη",
                actionAnswerCall = "Απάντηση Κλήσης",
                scanScreenTitle = "ΣΥΖΕΥΞΗ BLUETOOTH KTM",
                scanScreenSubtitle = "Επιλέξτε τη συσκευή Bluetooth KTM 1290 CCU / TFT",
                btnStartScan = "Αναζήτηση Συσκευών",
                btnStopScan = "Διακοπή Αναζήτησης",
                pairedDevicesHeader = "ΣΥΖΕΥΓΜΕΝΕΣ ΣΥΣΚΕΥΕΣ",
                discoveredDevicesHeader = "ΣΥΣΚΕΥΕΣ BLUETOOTH ΠΟΥ ΒΡΕΘΗΚΑΝ",
                noDiscoveredDevices = "Δεν βρέθηκαν συσκευές ακόμα. Πατήστε Αναζήτηση.",
                btnDisconnect = "Αποσύνδεση",
                unknownDevice = "Άγνωστη Συσκευή",
                pairedTag = "Συζευγμένο",
                languageSelectorTitle = "Επιλογή Γλώσσας"
            )
        }
    }
}
