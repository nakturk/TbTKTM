package com.tbtktm.parser

import com.tbtktm.model.KtmTurnIcon

object IconMatcher {

    /**
     * Metin içeriğinden veya manevra kelimelerinden en uygun KTM Turn-by-Turn ikonunu tespit eder.
     */
    fun matchFromText(instruction: String): KtmTurnIcon {
        val text = instruction.lowercase()

        return when {
            // Döner Kavşak
            text.contains("döner kavşak") || text.contains("roundabout") || text.contains("göbek") -> {
                when {
                    text.contains("1.") || text.contains("birinci") -> KtmTurnIcon.ROUNDABOUT_1
                    text.contains("2.") || text.contains("ikinci") -> KtmTurnIcon.ROUNDABOUT_2
                    text.contains("3.") || text.contains("üçüncü") -> KtmTurnIcon.ROUNDABOUT_3
                    text.contains("4.") || text.contains("dördüncü") -> KtmTurnIcon.ROUNDABOUT_4
                    else -> KtmTurnIcon.ROUNDABOUT_1
                }
            }

            // U Dönüşü
            text.contains("u dönüşü") || text.contains("u-turn") -> {
                if (text.contains("sağ")) KtmTurnIcon.UTURN_RIGHT else KtmTurnIcon.UTURN_LEFT
            }

            // Otoyol Giriş / Çıkış
            text.contains("otoyola gir") || text.contains("otoban") -> KtmTurnIcon.ENTER_HIGHWAY_RIGHT_LANE
            text.contains("çıkışından çık") || text.contains("çıkın") || text.contains("exit") -> KtmTurnIcon.LEAVE_HIGHWAY_RIGHT_LANE

            // Keskin Dönüş
            text.contains("keskin sağ") || text.contains("sharp right") -> KtmTurnIcon.HEAVY_RIGHT
            text.contains("keskin sol") || text.contains("sharp left") -> KtmTurnIcon.HEAVY_LEFT

            // Hafif Dönüş
            text.contains("hafif sağ") || text.contains("slight right") || text.contains("sağa doğru") -> KtmTurnIcon.LIGHT_RIGHT
            text.contains("hafif sol") || text.contains("slight left") || text.contains("sola doğru") -> KtmTurnIcon.LIGHT_LEFT

            // Sağda / Solda Kalın
            text.contains("sağda kalın") || text.contains("sağ şerit") || text.contains("keep right") -> KtmTurnIcon.KEEP_RIGHT
            text.contains("solda kalın") || text.contains("sol şerit") || text.contains("keep left") -> KtmTurnIcon.KEEP_LEFT

            // Standart Dönüşler
            text.contains("sağa") || text.contains("sağdan") || text.contains("turn right") -> KtmTurnIcon.QUITE_RIGHT
            text.contains("sola") || text.contains("soldan") || text.contains("turn left") -> KtmTurnIcon.QUITE_LEFT

            // Düz Devam
            text.contains("düz") || text.contains("devam") || text.contains("straight") || text.contains("ilerleyin") -> KtmTurnIcon.GO_STRAIGHT

            // Bitiş / Varış
            text.contains("vardınız") || text.contains("hedefinize") || text.contains("arrived") -> KtmTurnIcon.END

            else -> KtmTurnIcon.GO_STRAIGHT
        }
    }
}
