package com.cutespeak.engine

/*
 * Пользовательские списки из настроек: свои слова, каомодзи, действия, эмодзи и защищённые слова.
 * Всё очищается от символов, которые ломают разметку Discord.
 */
class CustomData(
    /** слово (строчное) → варианты замены */
    val words: Map<String, List<String>> = emptyMap(),
    val actions: List<String> = emptyList(),
    val kaomoji: List<String> = emptyList(),
    val emoji: List<String> = emptyList(),
    /** слова, которые плагин никогда не трогает */
    val protectedWords: List<String> = emptyList(),
) {
    companion object {
        val EMPTY = CustomData()

        private val LINES = Regex("\r?\n|;;")
        private val FORBIDDEN = Regex("[`*_|\\\\<>]")
        private val CUSTOM_EMOJI = Regex("^<a?:\\w+:\\d+>$")

        private fun lines(s: String) = s.split(LINES).map { it.trim() }.filter { it.isNotEmpty() }
        private fun safe(s: String) = s.replace(FORBIDDEN, "").trim().take(120)
        private fun safeEmoji(s: String) = if (CUSTOM_EMOJI.matches(s.trim())) s.trim() else safe(s)

        fun parse(
            customWords: String = "",
            customActions: String = "",
            customKaomoji: String = "",
            customEmoji: String = "",
            protectedWords: String = "",
        ): CustomData {
            val words = HashMap<String, List<String>>()
            for (line in lines(customWords)) {
                val eq = line.indexOf('=')
                if (eq < 1) continue
                val key = line.substring(0, eq).trim().lowercase()
                val variants = line.substring(eq + 1).split("|").map { safe(it) }.filter { it.isNotEmpty() }
                if (key.isNotEmpty() && variants.isNotEmpty()) words[key] = variants
            }
            return CustomData(
                words = words,
                actions = lines(customActions).map { safe(it.trim('*')) }.filter { it.isNotEmpty() },
                kaomoji = lines(customKaomoji).map { safe(it) }.filter { it.isNotEmpty() },
                emoji = customEmoji.split(Regex("\\s+|;;")).map { safeEmoji(it) }.filter { it.isNotEmpty() },
                protectedWords = protectedWords.split(Regex("[\\s,;]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() },
            )
        }
    }
}
