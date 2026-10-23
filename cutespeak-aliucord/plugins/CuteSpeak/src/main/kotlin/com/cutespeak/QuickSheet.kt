package com.cutespeak

import android.os.Bundle
import android.view.View
import com.aliucord.utils.DimenUtils
import com.aliucord.widgets.BottomSheet

/** Быстрая панель по долгому тапу на сердечко: режим и стиль для ЭТОГО чата, сила и главные эффекты. */
class QuickSheet(private val core: CuteCore, private val channelId: Long) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = view.context
        val p = core.prefs
        setPadding(DimenUtils.defaultPadding)

        addView(Ui.header(ctx, "В этом чате"))
        val ov = p.override(channelId)
        addView(Ui.radios(ctx, listOf("" to "Как в настройках", "on" to "Всегда включено", "off" to "Выключено"), ov.mode.orEmpty()) { core.setMode(channelId, it.ifEmpty { null }) })
        addView(Ui.note(ctx, "Стиль в этом чате"))
        addView(Ui.radios(ctx, listOf("" to "Как в настройках") + STYLES, ov.style.orEmpty()) { core.setStyleHere(channelId, it.ifEmpty { null }) })

        addView(Ui.header(ctx, "Сила"))
        val presets = listOf("25" to "Лёгкая", "55" to "Средняя", "80" to "Сильная", "100" to "Максимум")
        val nearest = presets.minByOrNull { kotlin.math.abs(it.first.toInt() - p.intensity) }?.first ?: "55"
        addView(Ui.radios(ctx, presets, nearest) { p.intensity = it.toInt() })

        addView(Ui.header(ctx, "Эффекты"))
        addView(Ui.switch(ctx, "Каомодзи", null, p.kaomoji) { p.kaomoji = it })
        addView(Ui.switch(ctx, "Эмодзи", null, p.emoji) { p.emoji = it })
        addView(Ui.switch(ctx, "Действия в звёздочках", null, p.actions) { p.actions = it })
        addView(Ui.switch(ctx, "Заикание", null, p.stutter) { p.stutter = it })
        addView(Ui.switch(ctx, "Растягивание", null, p.stretch) { p.stretch = it })
        addView(Ui.switch(ctx, "Междометия", null, p.interjections) { p.interjections = it })
        addView(Ui.switch(ctx, "Милые слова", null, p.diminutives) { p.diminutives = it })
        addView(Ui.note(ctx, "Остальное — в настройках плагина."))
    }
}
