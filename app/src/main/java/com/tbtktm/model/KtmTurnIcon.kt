package com.tbtktm.model

import com.tbtktm.i18n.AppLanguage

/**
 * KTM TFT ekranında yerleşik olarak bulunan Turn-by-Turn manevra ikon kodları.
 * Türkçe, İngilizce, İtalyanca, İspanyolca ve Yunanca tam dil desteği.
 */
enum class KtmTurnIcon(val code: Int, val description: String) {
    UNDEFINED(1, "Tanımsız"),
    GO_STRAIGHT(2, "Düz Devam Edin"),
    UTURN_RIGHT(3, "Sağa U Dönüşü"),
    UTURN_LEFT(4, "Sola U Dönüşü"),
    KEEP_RIGHT(5, "Sağdan Devam Edin"),
    LIGHT_RIGHT(6, "Sağa Hafif Dönüş"),
    QUITE_RIGHT(7, "Sağa Dönüş"),
    HEAVY_RIGHT(8, "Sağa Keskin Dönüş"),
    KEEP_MIDDLE(9, "Ortadan Devam Edin"),
    KEEP_LEFT(10, "Soldan Devam Edin"),
    LIGHT_LEFT(11, "Sola Hafif Dönüş"),
    QUITE_LEFT(12, "Sola Dönüş"),
    HEAVY_LEFT(13, "Sola Keskin Dönüş"),
    ENTER_HIGHWAY_RIGHT_LANE(14, "Otoyola Giriş (Sağ)"),
    ENTER_HIGHWAY_LEFT_LANE(15, "Otoyola Giriş (Sol)"),
    LEAVE_HIGHWAY_RIGHT_LANE(16, "Otoyoldan Çıkış (Sağ)"),
    LEAVE_HIGHWAY_LEFT_LANE(17, "Otoyoldan Çıkış (Sol)"),
    HIGHWAY_KEEP_RIGHT(18, "Otoyolda Sağda Kalın"),
    HIGHWAY_KEEP_LEFT(19, "Otoyolda Solda Kalın"),
    START(20, "Başlangıç"),
    END(21, "Hedefe Ulaşıldı"),
    FERRY(22, "Feribot"),
    PASS_STATION(23, "İstasyon Geçişi"),
    HEAD_TO(24, "Yönelim"),
    CHANGE_LINE(25, "Şerit Değiştir"),
    
    // Döner Kavşak (1-8 Çıkışlar)
    ROUNDABOUT_1(26, "Döner Kavşak 1. Çıkış"),
    ROUNDABOUT_2(27, "Döner Kavşak 2. Çıkış"),
    ROUNDABOUT_3(28, "Döner Kavşak 3. Çıkış"),
    ROUNDABOUT_4(29, "Döner Kavşak 4. Çıkış"),
    ROUNDABOUT_5(30, "Döner Kavşak 5. Çıkış"),
    ROUNDABOUT_6(31, "Döner Kavşak 6. Çıkış"),
    ROUNDABOUT_7(32, "Döner Kavşak 7. Çıkış"),
    ROUNDABOUT_8(33, "Döner Kavşak 8. Çıkış");

    fun getLocalizedDescription(lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.TURKISH -> description
            AppLanguage.ENGLISH -> when (this) {
                UNDEFINED -> "Undefined"
                GO_STRAIGHT -> "Continue Straight"
                UTURN_RIGHT -> "U-Turn Right"
                UTURN_LEFT -> "U-Turn Left"
                KEEP_RIGHT -> "Keep Right"
                LIGHT_RIGHT -> "Slight Right"
                QUITE_RIGHT -> "Turn Right"
                HEAVY_RIGHT -> "Sharp Right"
                KEEP_MIDDLE -> "Keep Middle"
                KEEP_LEFT -> "Keep Left"
                LIGHT_LEFT -> "Slight Left"
                QUITE_LEFT -> "Turn Left"
                HEAVY_LEFT -> "Sharp Left"
                ENTER_HIGHWAY_RIGHT_LANE -> "Enter Highway (Right)"
                ENTER_HIGHWAY_LEFT_LANE -> "Enter Highway (Left)"
                LEAVE_HIGHWAY_RIGHT_LANE -> "Exit Highway (Right)"
                LEAVE_HIGHWAY_LEFT_LANE -> "Exit Highway (Left)"
                HIGHWAY_KEEP_RIGHT -> "Keep Right on Highway"
                HIGHWAY_KEEP_LEFT -> "Keep Left on Highway"
                START -> "Start Point"
                END -> "Arrived at Destination"
                FERRY -> "Ferry"
                PASS_STATION -> "Transit Station"
                HEAD_TO -> "Head Towards"
                CHANGE_LINE -> "Change Lane"
                ROUNDABOUT_1 -> "Roundabout 1st Exit"
                ROUNDABOUT_2 -> "Roundabout 2nd Exit"
                ROUNDABOUT_3 -> "Roundabout 3rd Exit"
                ROUNDABOUT_4 -> "Roundabout 4th Exit"
                ROUNDABOUT_5 -> "Roundabout 5th Exit"
                ROUNDABOUT_6 -> "Roundabout 6th Exit"
                ROUNDABOUT_7 -> "Roundabout 7th Exit"
                ROUNDABOUT_8 -> "Roundabout 8th Exit"
            }
            AppLanguage.ITALIAN -> when (this) {
                UNDEFINED -> "Non definito"
                GO_STRAIGHT -> "Continua dritto"
                UTURN_RIGHT -> "Inversione a destra"
                UTURN_LEFT -> "Inversione a sinistra"
                KEEP_RIGHT -> "Mantieni la destra"
                LIGHT_RIGHT -> "Gira leggermente a destra"
                QUITE_RIGHT -> "Gira a destra"
                HEAVY_RIGHT -> "Curva a gomito a destra"
                KEEP_MIDDLE -> "Mantieni il centro"
                KEEP_LEFT -> "Mantieni la sinistra"
                LIGHT_LEFT -> "Gira leggermente a sinistra"
                QUITE_LEFT -> "Gira a sinistra"
                HEAVY_LEFT -> "Curva a gomito a sinistra"
                ENTER_HIGHWAY_RIGHT_LANE -> "Entra in autostrada (DX)"
                ENTER_HIGHWAY_LEFT_LANE -> "Entra in autostrada (SX)"
                LEAVE_HIGHWAY_RIGHT_LANE -> "Esci dall'autostrada (DX)"
                LEAVE_HIGHWAY_LEFT_LANE -> "Esci dall'autostrada (SX)"
                HIGHWAY_KEEP_RIGHT -> "Resta a destra in autostrada"
                HIGHWAY_KEEP_LEFT -> "Resta a sinistra in autostrada"
                START -> "Punto di partenza"
                END -> "Destinazione raggiunta"
                FERRY -> "Traghetto"
                PASS_STATION -> "Stazione di transito"
                HEAD_TO -> "Dirigiti verso"
                CHANGE_LINE -> "Cambia corsia"
                ROUNDABOUT_1 -> "Rotatoria 1ª uscita"
                ROUNDABOUT_2 -> "Rotatoria 2ª uscita"
                ROUNDABOUT_3 -> "Rotatoria 3ª uscita"
                ROUNDABOUT_4 -> "Rotatoria 4ª uscita"
                ROUNDABOUT_5 -> "Rotatoria 5ª uscita"
                ROUNDABOUT_6 -> "Rotatoria 6ª uscita"
                ROUNDABOUT_7 -> "Rotatoria 7ª uscita"
                ROUNDABOUT_8 -> "Rotatoria 8ª uscita"
            }
            AppLanguage.SPANISH -> when (this) {
                UNDEFINED -> "Indefinido"
                GO_STRAIGHT -> "Continúa recto"
                UTURN_RIGHT -> "Cambio de sentido (Derecha)"
                UTURN_LEFT -> "Cambio de sentido (Izquierda)"
                KEEP_RIGHT -> "Mantente a la derecha"
                LIGHT_RIGHT -> "Gira ligeramente a la derecha"
                QUITE_RIGHT -> "Gira a la derecha"
                HEAVY_RIGHT -> "Giro cerrado a la derecha"
                KEEP_MIDDLE -> "Mantente en el centro"
                KEEP_LEFT -> "Mantente a la izquierda"
                LIGHT_LEFT -> "Gira ligeramente a la izquierda"
                QUITE_LEFT -> "Gira a la izquierda"
                HEAVY_LEFT -> "Giro cerrado a la izquierda"
                ENTER_HIGHWAY_RIGHT_LANE -> "Incorporación autopista (Der)"
                ENTER_HIGHWAY_LEFT_LANE -> "Incorporación autopista (Izq)"
                LEAVE_HIGHWAY_RIGHT_LANE -> "Salida de autopista (Der)"
                LEAVE_HIGHWAY_LEFT_LANE -> "Salida de autopista (Izq)"
                HIGHWAY_KEEP_RIGHT -> "Mantente a la der en autopista"
                HIGHWAY_KEEP_LEFT -> "Mantente a la izq en autopista"
                START -> "Punto de inicio"
                END -> "Destino alcanzado"
                FERRY -> "Ferry"
                PASS_STATION -> "Estación de tránsito"
                HEAD_TO -> "Dirígete hacia"
                CHANGE_LINE -> "Cambia de carril"
                ROUNDABOUT_1 -> "Rotonda 1ª salida"
                ROUNDABOUT_2 -> "Rotonda 2ª salida"
                ROUNDABOUT_3 -> "Rotonda 3ª salida"
                ROUNDABOUT_4 -> "Rotonda 4ª salida"
                ROUNDABOUT_5 -> "Rotonda 5ª salida"
                ROUNDABOUT_6 -> "Rotonda 6ª salida"
                ROUNDABOUT_7 -> "Rotonda 7ª salida"
                ROUNDABOUT_8 -> "Rotonda 8ª salida"
            }
            AppLanguage.GREEK -> when (this) {
                UNDEFINED -> "Μη καθορισμένο"
                GO_STRAIGHT -> "Συνεχίστε ευθεία"
                UTURN_RIGHT -> "Αναστροφή δεξιά"
                UTURN_LEFT -> "Αναστροφή αριστερά"
                KEEP_RIGHT -> "Μείνετε δεξιά"
                LIGHT_RIGHT -> "Ελαφρώς δεξιά"
                QUITE_RIGHT -> "Στρίψτε δεξιά"
                HEAVY_RIGHT -> "Κλειστή στροφή δεξιά"
                KEEP_MIDDLE -> "Μείνετε στη μέση"
                KEEP_LEFT -> "Μείνετε αριστερά"
                LIGHT_LEFT -> "Ελαφρώς αριστερά"
                QUITE_LEFT -> "Στρίψτε αριστερά"
                HEAVY_LEFT -> "Κλειστή στροφή αριστερά"
                ENTER_HIGHWAY_RIGHT_LANE -> "Είσοδος αυτοκινητοδρόμου (Δεξιά)"
                ENTER_HIGHWAY_LEFT_LANE -> "Είσοδος αυτοκινητοδρόμου (Αριστερά)"
                LEAVE_HIGHWAY_RIGHT_LANE -> "Έξοδος αυτοκινητοδρόμου (Δεξιά)"
                LEAVE_HIGHWAY_LEFT_LANE -> "Έξοδος αυτοκινητοδρόμου (Αριστερά)"
                HIGHWAY_KEEP_RIGHT -> "Μείνετε δεξιά στον αυτοκινητόδρομο"
                HIGHWAY_KEEP_LEFT -> "Μείνετε αριστερά στον αυτοκινητόδρομο"
                START -> "Σημείο εκκίνησης"
                END -> "Άφιξη στον προορισμό"
                FERRY -> "Πορθμείο"
                PASS_STATION -> "Σταθμός διέλευσης"
                HEAD_TO -> "Κατευθυνθείτε προς"
                CHANGE_LINE -> "Αλλαγή λωρίδας"
                ROUNDABOUT_1 -> "Κυκλικός κόμβος 1η έξοδος"
                ROUNDABOUT_2 -> "Κυκλικός κόμβος 2η έξοδος"
                ROUNDABOUT_3 -> "Κυκλικός κόμβος 3η έξοδος"
                ROUNDABOUT_4 -> "Κυκλικός κόμβος 4η έξοδος"
                ROUNDABOUT_5 -> "Κυκλικός κόμβος 5η έξοδος"
                ROUNDABOUT_6 -> "Κυκλικός κόμβος 6η έξοδος"
                ROUNDABOUT_7 -> "Κυκλικός κόμβος 7η έξοδος"
                ROUNDABOUT_8 -> "Κυκλικός κόμβος 8η έξοδος"
            }
        }
    }

    companion object {
        fun fromCode(code: Int): KtmTurnIcon {
            return entries.find { it.code == code } ?: GO_STRAIGHT
        }
    }
}
