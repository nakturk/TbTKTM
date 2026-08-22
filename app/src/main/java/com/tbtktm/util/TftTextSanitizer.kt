package com.tbtktm.util

/**
 * KTM TFT ekranları Türkçe ve bazı genişletilmiş UTF-8 karakter setlerini desteklemez.
 * TFT'ye gönderilen metinleri TFT'nin desteklediği standart ASCII karakterlere dönüştüren yardımcı araç.
 */
object TftTextSanitizer {

    /**
     * Verilen metindeki Türkçe ve özel karakterleri TFT ekranının doğru görüntüleyebileceği
     * standart Latin/ASCII karşılıklarıyla değiştirir.
     */
    fun sanitize(text: String?): String {
        if (text.isNullOrEmpty()) return ""

        val sb = StringBuilder(text.length)
        for (char in text) {
            when (char) {
                // Türkçe Küçük Harfler
                'ç' -> sb.append('c')
                'ğ' -> sb.append('g')
                'ı' -> sb.append('i')
                'ö' -> sb.append('o')
                'ş' -> sb.append('s')
                'ü' -> sb.append('u')

                // Türkçe Büyük Harfler
                'Ç' -> sb.append('C')
                'Ğ' -> sb.append('G')
                'İ' -> sb.append('I')
                'Ö' -> sb.append('O')
                'Ş' -> sb.append('S')
                'Ü' -> sb.append('U')

                // Şapkalı / Aksanlı Harfler
                'â', 'á', 'à', 'ä', 'ã', 'å' -> sb.append('a')
                'Â', 'Á', 'À', 'Ä', 'Ã', 'Å' -> sb.append('A')
                'ê', 'é', 'è', 'ë' -> sb.append('e')
                'Ê', 'É', 'È', 'Ë' -> sb.append('E')
                'î', 'í', 'ì', 'ï' -> sb.append('i')
                'Î', 'Í', 'Ì', 'Ï' -> sb.append('I')
                'ô', 'ó', 'ò', 'õ' -> sb.append('o')
                'Ô', 'Ó', 'Ò', 'Õ' -> sb.append('O')
                'û', 'ú', 'ù' -> sb.append('u')
                'Û', 'Ú', 'Ù' -> sb.append('U')
                'ñ' -> sb.append('n')
                'Ñ' -> sb.append('N')

                // Özel Tipografik Karakterler
                '’', '‘', '`', '´' -> sb.append('\'')
                '“', '”', '„', '«', '»' -> sb.append('"')
                '–', '—', '−' -> sb.append('-')
                '…' -> sb.append("...")

                else -> sb.append(char)
            }
        }
        return sb.toString()
    }
}

/**
 * Kolay kullanım için String extension fonksiyonu
 */
fun String?.toTftCleanText(): String {
    return TftTextSanitizer.sanitize(this)
}
