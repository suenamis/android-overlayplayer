package com.example.overlay_player

private val NUR_ZIFFERN = Regex("""\d{5,}""")
private val HEX_KENNUNG = Regex("""[0-9a-fA-F]{16,}""")
private val BUCHSTABEN_ZIFFERN = Regex("""[0-9a-zA-Z]{16,}""")

/** Manche Quellen liefern statt des Dateinamens nur eine interne Kennung wie „msf:1000012345". */
fun siehtWieKennungAus(name: String): Boolean {
    val stamm = name.substringAfterLast(':').substringBeforeLast('.').trim()
    if (stamm.isEmpty()) return true
    if (NUR_ZIFFERN.matches(stamm)) return true
    val ohneTrenner = stamm.replace("-", "").replace("_", "")
    if (HEX_KENNUNG.matches(ohneTrenner)) return true
    if (!BUCHSTABEN_ZIFFERN.matches(stamm)) return false
    return stamm.count { it.isDigit() } * 4 >= stamm.length
}
