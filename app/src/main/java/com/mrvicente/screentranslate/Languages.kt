package com.mrvicente.screentranslate

object Languages {

    data class Lang(val tag: String, val name: String)

    val supported = listOf(
        Lang("af", "Afrikáans"),
        Lang("ar", "Árabe"),
        Lang("bg", "Búlgaro"),
        Lang("bn", "Bengalí"),
        Lang("ca", "Catalán"),
        Lang("cs", "Checo"),
        Lang("cy", "Galés"),
        Lang("da", "Danés"),
        Lang("de", "Alemán"),
        Lang("el", "Griego"),
        Lang("en", "Inglés"),
        Lang("eo", "Esperanto"),
        Lang("es", "Español"),
        Lang("et", "Estonio"),
        Lang("fa", "Persa"),
        Lang("fi", "Finés"),
        Lang("fr", "Francés"),
        Lang("ga", "Irlandés"),
        Lang("gl", "Gallego"),
        Lang("gu", "Gujarati"),
        Lang("he", "Hebreo"),
        Lang("hi", "Hindi"),
        Lang("hr", "Croata"),
        Lang("hu", "Húngaro"),
        Lang("id", "Indonesio"),
        Lang("is", "Islandés"),
        Lang("it", "Italiano"),
        Lang("ja", "Japonés"),
        Lang("ka", "Georgiano"),
        Lang("kn", "Canarés"),
        Lang("ko", "Coreano"),
        Lang("lt", "Lituano"),
        Lang("lv", "Letón"),
        Lang("mk", "Macedonio"),
        Lang("mr", "Maratí"),
        Lang("ms", "Malayo"),
        Lang("mt", "Maltés"),
        Lang("nl", "Neerlandés"),
        Lang("no", "Noruego"),
        Lang("pl", "Polaco"),
        Lang("pt", "Portugués"),
        Lang("ro", "Rumano"),
        Lang("ru", "Ruso"),
        Lang("sk", "Eslovaco"),
        Lang("sl", "Esloveno"),
        Lang("sq", "Albanés"),
        Lang("sr", "Serbio"),
        Lang("sv", "Sueco"),
        Lang("sw", "Suajili"),
        Lang("ta", "Tamil"),
        Lang("te", "Telugu"),
        Lang("th", "Tailandés"),
        Lang("tl", "Tagalo"),
        Lang("tr", "Turco"),
        Lang("uk", "Ucraniano"),
        Lang("ur", "Urdu"),
        Lang("vi", "Vietnamita"),
        Lang("zh", "Chino")
    )

    val sourceOptions: List<Lang> = listOf(Lang(Constants.AUTO, "Detectar automáticamente")) + supported

    val targetOptions: List<Lang> = supported

    fun nameOf(tag: String): String =
        supported.firstOrNull { it.tag == tag }?.name ?: tag
}
