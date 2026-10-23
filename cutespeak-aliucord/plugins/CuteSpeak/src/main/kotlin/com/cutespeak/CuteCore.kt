package com.cutespeak

import com.aliucord.wrappers.ChannelWrapper
import com.cutespeak.engine.CuteEngine
import com.discord.stores.StoreStream

/** Логика «где включено и с каким стилем», история для отката и переколдовывания. Не зависит от интерфейса. */
class CuteCore(val prefs: Prefs) {
    private val originals = LinkedHashMap<String, String>()
    private val justSent = HashSet<String>()
    private val listeners = ArrayList<() -> Unit>()

    /** Подписка на изменения (кнопка в поле ввода перерисовывается). */
    fun onChange(l: () -> Unit) { listeners.add(l) }
    fun notifyChanged() = listeners.toList().forEach { runCatching(it) }
    fun clearListeners() = listeners.clear()

    fun isPrivate(channelId: Long): Boolean {
        val ch = StoreStream.getChannels().getChannel(channelId) ?: return false
        return ChannelWrapper(ch).guildId == 0L
    }

    class State(val enabled: Boolean, val style: String)

    /** Включён ли режим в этом чате и какой стиль использовать. */
    fun resolve(channelId: Long): State {
        val ov = prefs.override(channelId)
        val enabled = when (ov.mode) {
            "on" -> true
            "off" -> false
            else -> prefs.enabled && when (prefs.scope) {
                "dms" -> isPrivate(channelId)
                "servers" -> !isPrivate(channelId)
                else -> true
            }
        }
        return State(enabled, ov.style ?: prefs.style)
    }

    /** Режим «по умолчанию» для чата (без учёта переопределения). */
    fun defaultEnabled(channelId: Long): Boolean = prefs.enabled && when (prefs.scope) {
        "dms" -> isPrivate(channelId)
        "servers" -> !isPrivate(channelId)
        else -> true
    }

    /** Тап по сердечку: переключить режим именно для этого чата. */
    fun toggleHere(channelId: Long): Boolean {
        val next = !resolve(channelId).enabled
        val ov = prefs.override(channelId)
        val mode = if (next == defaultEnabled(channelId)) null else if (next) "on" else "off"
        prefs.setOverride(channelId, ov.copy(mode = mode))
        notifyChanged()
        return next
    }

    fun setMode(channelId: Long, mode: String?) { prefs.setOverride(channelId, prefs.override(channelId).copy(mode = mode)); notifyChanged() }
    fun setStyleHere(channelId: Long, style: String?) { prefs.setOverride(channelId, prefs.override(channelId).copy(style = style)); notifyChanged() }

    /* ── история ── */

    private fun key(channelId: Long, content: String) = "$channelId\u0000$content"

    fun remember(channelId: Long, cute: String, original: String) {
        if (cute == original) return
        originals[key(channelId, cute)] = original
        while (originals.size > 300) originals.remove(originals.keys.first())
    }

    fun originalOf(channelId: Long, content: String): String? = originals[key(channelId, content)]

    /** Содержимое, которое уже подготовила команда /cute, повторно не трогаем. */
    fun markPrepared(content: String) { justSent.add(content) }
    fun consumePrepared(content: String): Boolean = justSent.remove(content)

    /** Превращение для отправки. Префикс «//» отправляет сообщение как есть. */
    fun transform(content: String, styleKey: String?): String {
        val prefix = prefs.rawPrefix
        if (prefix.isNotEmpty() && content.startsWith(prefix)) return content.removePrefix(prefix).trimStart()
        return CuteEngine.cuteify(content, prefs.toOptions(styleKey))
    }
}
