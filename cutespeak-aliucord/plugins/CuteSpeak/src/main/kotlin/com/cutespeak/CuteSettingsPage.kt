package com.cutespeak

import android.annotation.SuppressLint
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.cutespeak.engine.CuteEngine
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R

/**
 * Страница настроек для телефона: сверху «Примерка» (напиши фразу — увидишь 5 вариантов, тап по варианту копирует),
 * ниже — все настройки одним длинным списком с разделами.
 */
@SuppressLint("SetTextI18n")
class CuteSettingsPage(private val core: CuteCore) : SettingsPage() {
    private val p get() = core.prefs

    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("CuteSpeak")
        val ctx = requireContext()

        /* ── примерка ── */
        addHeader(ctx, "Примерка")
        addView(Ui.note(ctx, "Напиши фразу и посмотри, как она будет звучать. Тап по варианту — скопировать."))
        var sample = "Давай поговорим об этом вечером"
        addView(Ui.text(ctx, "Твоя фраза", sample) { sample = it })
        val results = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        addView(Button(ctx).apply {
            text = "Показать 5 вариантов"
            setOnClickListener {
                results.removeAllViews()
                val list = CuteEngine.variants(sample.ifBlank { "привет" }, 5, p.toOptions())
                list.forEachIndexed { i, v ->
                    results.addView(TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
                        text = "${i + 1}. $v"
                        textSize = 16f
                        setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
                        val pad = DimenUtils.dpToPx(12)
                        setPadding(pad, pad, pad, pad)
                        setOnClickListener {
                            Utils.setClipboard("CuteSpeak", v)
                            Utils.showToast("Скопировано")
                        }
                    })
                }
            }
        })
        addView(results)
        addDivider(ctx)

        /* ── основное ── */
        addHeader(ctx, "Основное")
        addView(Ui.switch(ctx, "Милый режим", "То же самое делает сердечко над полем ввода", p.enabled) { p.enabled = it; core.notifyChanged() })
        addView(Ui.note(ctx, "Где работает режим (для отдельного чата переопределяется долгим тапом по сердечку)"))
        addView(Ui.radios(ctx, listOf("everywhere" to "Везде", "dms" to "Только в личных сообщениях и группах", "servers" to "Только на серверах"), p.scope) { p.scope = it; core.notifyChanged() })
        addDivider(ctx)

        addHeader(ctx, "Стиль речи")
        addView(Ui.radios(ctx, STYLES, p.style, STYLE_HINT) { p.style = it; core.notifyChanged() })
        addView(Ui.slider(ctx, "Сила превращения", "Чем выше, тем чаще заикания, растягивания и украшения", p.intensity, 10, 100, 5, "") { p.intensity = it })
        addView(Ui.slider(ctx, "Заикание", "Насколько часто (от обычного)", p.stutterAmount, 0, 200, 25, "%") { p.stutterAmount = it })
        addView(Ui.slider(ctx, "Растягивание гласных", "Насколько часто (от обычного)", p.stretchAmount, 0, 200, 25, "%") { p.stretchAmount = it })
        addView(Ui.slider(ctx, "Украшения", "Каомодзи, эмодзи, действия (от обычного)", p.decorAmount, 0, 200, 25, "%") { p.decorAmount = it })
        addDivider(ctx)

        /* ── речь ── */
        addHeader(ctx, "Речь")
        addView(Ui.switch(ctx, "Заикание", "«д-д-давай», «э-э-это»", p.stutter) { p.stutter = it })
        addView(Ui.switch(ctx, "Растягивание гласных", "«привеееет», «пооокооворим»", p.stretch) { p.stretch = it })
        addView(Ui.switch(ctx, "Перекаты согласных", "«нннет», «мммможно»", p.consonantRoll) { p.consonantRoll = it })
        addView(Ui.switch(ctx, "Повторы слов", "«да-да», «нет-нет», «тихо-тихо»", p.echo) { p.echo = it })
        addView(Ui.switch(ctx, "Замена г → к", "«поговорим» → «поковорим»", p.swapGK) { p.swapGK = it })
        addView(Ui.switch(ctx, "Картавость", "«р» → «л». Стиль «Малыш» включает её сам", p.swapRL) { p.swapRL = it })
        addView(Ui.switch(ctx, "Шепелявость", "ш/щ → с, ж → з, ч → ц. Стиль «Малыш» включает её сам", p.lisp) { p.lisp = it })
        addView(Ui.switch(ctx, "Пишем как слышим", "«чего» → «чево», «улыбаться» → «улыбаца»", p.phonetic) { p.phonetic = it })
        addView(Ui.switch(ctx, "Милые слова", "«привет» → «приветик», «маме» → «мамочке», «красивый» → «красивенький»", p.diminutives) { p.diminutives = it })
        addView(Ui.switch(ctx, "Ласковые имена", "«Маша» → «Машенька» (только с большой буквы)", p.names) { p.names = it })
        addView(Ui.switch(ctx, "Смягчать мат", "«блин», «ёлки-палки», «капец», «нафиг»", p.softenProfanity) { p.softenProfanity = it })
        addView(Ui.switch(ctx, "Смайлики → каомодзи", "«привет:)», «как дела))», «<3», «:3»", p.smileys) { p.smileys = it })
        addView(Ui.switch(ctx, "Всё строчными", null, p.lowercase) { p.lowercase = it })
        addDivider(ctx)

        /* ── украшения ── */
        addHeader(ctx, "Украшения")
        addView(Ui.switch(ctx, "Каомодзи", "(⁄ ⁄•⁄ω⁄•⁄ ⁄)", p.kaomoji) { p.kaomoji = it })
        addView(Ui.switch(ctx, "Эмодзи в конце", "💕 🥺 🐾", p.emoji) { p.emoji = it })
        addView(Ui.switch(ctx, "Действия в звёздочках", "*краснеет*, *тихо мур*", p.actions) { p.actions = it })
        addView(Ui.switch(ctx, "Междометия", "«э-э...», «ммм...», «ну...»", p.interjections) { p.interjections = it })
        addView(Ui.switch(ctx, "Тильды и мягкая пунктуация", "«~», «!!», «?..»", p.tilde) { p.tilde = it })
        addDivider(ctx)

        /* ── умные штуки ── */
        addHeader(ctx, "Умные штуки")
        addView(Ui.switch(ctx, "Учитывать время суток", "Ночью в режиме «Авто» чаще выбирается сонный стиль", p.timeAware) { p.timeAware = it })
        addView(Ui.switch(ctx, "Сезонные эмодзи", "🎃 осенью, 🎄 зимой, 🌷 весной, 💘 в феврале", p.seasonal) { p.seasonal = it })
        addView(Ui.switch(ctx, "Защита серьёзных тем", "Сообщения про смерть, больницу, обстрелы, тревогу и т. п. уходят как есть", p.seriousGuard) { p.seriousGuard = it })
        addDivider(ctx)

        /* ── свои списки ── */
        addHeader(ctx, "Свои списки")
        addView(Ui.note(ctx, "Свои слова: по одной замене на строку, «слово = вариант1 | вариант2», например «кофе = кофеёк | кофеечек»"))
        addView(Ui.text(ctx, "Свои слова", p.customWords, multiline = true) { p.customWords = it })
        addView(Ui.text(ctx, "Свои действия (по одному на строку, без звёздочек)", p.customActions, multiline = true) { p.customActions = it })
        addView(Ui.text(ctx, "Свои каомодзи (по одному на строку)", p.customKaomoji, multiline = true) { p.customKaomoji = it })
        addView(Ui.text(ctx, "Свои эмодзи через пробел", p.customEmoji) { p.customEmoji = it })
        addView(Ui.text(ctx, "Никогда не трогать слова (через пробел)", p.protectedWords) { p.protectedWords = it })
        addDivider(ctx)

        /* ── служебное ── */
        addHeader(ctx, "Служебное")
        addView(Ui.note(ctx, "Сообщение, начинающееся с этого префикса, уйдёт как есть (префикс удаляется). Пусто — отключить."))
        addView(Ui.text(ctx, "Префикс «без превращения»", p.rawPrefix) { p.rawPrefix = it })
        addView(Ui.switch(ctx, "Лимит 4000 символов (Nitro)", "Если «милая» версия длиннее лимита — отправится облегчённая или оригинал", p.maxLength > 2000) { p.maxLength = if (it) 4000 else 2000 })
        addView(Ui.spacer(ctx, 24))
    }
}
