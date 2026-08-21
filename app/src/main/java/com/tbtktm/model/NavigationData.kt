package com.tbtktm.model

enum class NavSource(val displayName: String) {
    NONE("Aktif Değil"),
    GOOGLE_MAPS("Google Maps"),
    YANDEX_NAVI("Yandex Navigasyon")
}

/**
 * Anlık navigasyon ve TFT gösterge durumunu temsil eden veri modeli.
 */
data class NavigationData(
    val isActive: Boolean = false,
    val source: NavSource = NavSource.NONE,
    val turnIcon: KtmTurnIcon = KtmTurnIcon.GO_STRAIGHT,
    val distanceToTurn: String = "",        // Örn: "250 m"
    val turnInfo: String = "",              // Örn: "Sağa dönün"
    val roadName: String = "",              // Örn: "Bağdat Caddesi"
    val eta: String = "",                   // Örn: "18:45"
    val distanceToDestination: String = "", // Örn: "14 km"
    val notificationText: String = "",      // Örn: "Rota güncelleniyor"
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val isKtmDevice: Boolean = false
)
