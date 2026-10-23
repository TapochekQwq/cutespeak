package com.cutespeak

import com.aliucord.api.SettingsAPI
import com.cutespeak.engine.CuteOptions
import com.cutespeak.engine.CustomData
import com.cutespeak.engine.SeriousGuard
import com.cutespeak.engine.data.Vibe

/** Стили: (ключ, название). Ключ "auto" = подбирать под настроение и время суток. */
val STYLES: List<Pair<String, String>> = listOf(
    "auto" to "Авто",
    "shy" to "Застенчивый",
    "kawaii" to "Кавайный",
    "sleepy" to "Сонный",
    "chaos" to "Хаос",
    "neko" to "Неко",
    "tsundere" to "Цундере",
    "baby" to "Малыш",
)

val STYLE_HINT: Map<String, String> = mapOf(
    "auto" to "подбирать под настроение сообщения и время суток",
    "shy" to "заикается, краснеет, прячется",
    "kawaii" to "ня~, уменьшительные, много эмодзи",
    "sleepy" to "ммм, тянет слова, зевает",
    "chaos" to "всего и сразу",
    "neko" to "мяу, мур, пррр",
    "tsundere" to "«н-не то чтобы...», хмпф",
    "baby" to "ути-пути, картавит и шепелявит",
)

fun styleLabel(key: String) = STYLES.firstOrNull { it.first == key }?.second ?: key

/** Хранилище настроек плагина поверх SettingsAPI. Значения по умолчанию — те же, что в версии для Vencord. */
class Prefs(private val s: SettingsAPI) {
    // основное
    var enabled: Boolean get() = s.getBool("enabled", true); set(v) = s.setBool("enabled", v)
    var scope: String get() = s.getString("scope", "everywhere"); set(v) = s.setString("scope", v)
    var style: String get() = s.getString("style", "auto"); set(v) = s.setString("style", v)
    var intensity: Int get() = s.getInt("intensity", 55); set(v) = s.setInt("intensity", v)
    var stutterAmount: Int get() = s.getInt("stutterAmount", 100); set(v) = s.setInt("stutterAmount", v)
    var stretchAmount: Int get() = s.getInt("stretchAmount", 100); set(v) = s.setInt("stretchAmount", v)
    var decorAmount: Int get() = s.getInt("decorAmount", 100); set(v) = s.setInt("decorAmount", v)

    // речь
    var stutter: Boolean get() = s.getBool("stutter", true); set(v) = s.setBool("stutter", v)
    var stretch: Boolean get() = s.getBool("stretch", true); set(v) = s.setBool("stretch", v)
    var consonantRoll: Boolean get() = s.getBool("consonantRoll", true); set(v) = s.setBool("consonantRoll", v)
    var echo: Boolean get() = s.getBool("echo", true); set(v) = s.setBool("echo", v)
    var swapGK: Boolean get() = s.getBool("swapGK", true); set(v) = s.setBool("swapGK", v)
    var swapRL: Boolean get() = s.getBool("swapRL", false); set(v) = s.setBool("swapRL", v)
    var lisp: Boolean get() = s.getBool("lisp", false); set(v) = s.setBool("lisp", v)
    var phonetic: Boolean get() = s.getBool("phonetic", true); set(v) = s.setBool("phonetic", v)
    var diminutives: Boolean get() = s.getBool("diminutives", true); set(v) = s.setBool("diminutives", v)
    var names: Boolean get() = s.getBool("names", true); set(v) = s.setBool("names", v)
    var softenProfanity: Boolean get() = s.getBool("softenProfanity", false); set(v) = s.setBool("softenProfanity", v)
    var smileys: Boolean get() = s.getBool("smileys", true); set(v) = s.setBool("smileys", v)
    var lowercase: Boolean get() = s.getBool("lowercase", true); set(v) = s.setBool("lowercase", v)

    // украшения
    var kaomoji: Boolean get() = s.getBool("kaomoji", true); set(v) = s.setBool("kaomoji", v)
    var emoji: Boolean get() = s.getBool("emoji", true); set(v) = s.setBool("emoji", v)
    var actions: Boolean get() = s.getBool("actions", true); set(v) = s.setBool("actions", v)
    var interjections: Boolean get() = s.getBool("interjections", true); set(v) = s.setBool("interjections", v)
    var tilde: Boolean get() = s.getBool("tilde", true); set(v) = s.setBool("tilde", v)

    // умные штуки
    var timeAware: Boolean get() = s.getBool("timeAware", true); set(v) = s.setBool("timeAware", v)
    var seasonal: Boolean get() = s.getBool("seasonal", true); set(v) = s.setBool("seasonal", v)
    var seriousGuard: Boolean get() = s.getBool("seriousGuard", true); set(v) = s.setBool("seriousGuard", v)

    // свои списки
    var customWords: String get() = s.getString("customWords", ""); set(v) = s.setString("customWords", v)
    var customActions: String get() = s.getString("customActions", ""); set(v) = s.setString("customActions", v)
    var customKaomoji: String get() = s.getString("customKaomoji", ""); set(v) = s.setString("customKaomoji", v)
    var customEmoji: String get() = s.getString("customEmoji", ""); set(v) = s.setString("customEmoji", v)
    var protectedWords: String get() = s.getString("protectedWords", ""); set(v) = s.setString("protectedWords", v)

    // служебное
    var rawPrefix: String get() = s.getString("rawPrefix", "//"); set(v) = s.setString("rawPrefix", v)
    var maxLength: Int get() = s.getInt("maxLength", 2000); set(v) = s.setInt("maxLength", v)

    /** Переопределение для чата: «режим;стиль», где режим — on/off/пусто, стиль — ключ/пусто. */
    fun override(channelId: Long): Override {
        val raw = s.getString("ov_$channelId", "")
        val parts = raw.split(';')
        return Override(parts.getOrNull(0)?.ifEmpty { null }, parts.getOrNull(1)?.ifEmpty { null })
    }

    fun setOverride(channelId: Long, o: Override) {
        if (o.mode == null && o.style == null) s.remove("ov_$channelId")
        else s.setString("ov_$channelId", "${o.mode.orEmpty()};${o.style.orEmpty()}")
    }

    data class Override(val mode: String? = null, val style: String? = null)

    private var customCache: Pair<String, CustomData>? = null

    private fun custom(): CustomData {
        val key = listOf(customWords, customActions, customKaomoji, customEmoji, protectedWords).joinToString("\u0001")
        customCache?.let { if (it.first == key) return it.second }
        val d = CustomData.parse(customWords, customActions, customKaomoji, customEmoji, protectedWords)
        customCache = key to d
        return d
    }

    /** Настройки → опции движка. [styleKey] — стиль для конкретного чата (или null, чтобы взять общий). */
    fun toOptions(styleKey: String? = null): CuteOptions {
        val key = styleKey ?: style
        val vibe = if (key == "auto") null else Vibe.values().firstOrNull { it.name.equals(key, ignoreCase = true) }
        return CuteOptions(
            style = vibe,
            intensity = intensity,
            stutterAmount = stutterAmount,
            stretchAmount = stretchAmount,
            decorAmount = decorAmount,
            stutter = stutter,
            stretch = stretch,
            consonantRoll = consonantRoll,
            echo = echo,
            swapGK = swapGK,
            swapRL = swapRL,
            lisp = lisp,
            phonetic = phonetic,
            diminutives = diminutives,
            names = names,
            softenProfanity = softenProfanity,
            smileys = smileys,
            kaomoji = kaomoji,
            emoji = emoji,
            actions = actions,
            interjections = interjections,
            tilde = tilde,
            lowercase = lowercase,
            timeAware = timeAware,
            seasonal = seasonal,
            seriousGuard = if (seriousGuard) SeriousGuard.SKIP else SeriousGuard.OFF,
            maxLength = maxLength.coerceAtLeast(100),
            custom = custom(),
        )
    }
}
