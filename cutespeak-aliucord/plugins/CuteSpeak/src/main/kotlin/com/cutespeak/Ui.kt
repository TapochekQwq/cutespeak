package com.cutespeak

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.lytefast.flexinput.R

/*
 * Мобильные «кирпичики» интерфейса — используются и страницей настроек, и быстрой панелью.
 * Всё построено на штатных виджетах Aliucord/Discord, поэтому выглядит как родные настройки.
 */
object Ui {
    fun header(ctx: Context, text: String) = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply { this.text = text }

    fun note(ctx: Context, text: String) = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
        this.text = text
        setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
        val p = DimenUtils.dpToPx(16)
        setPadding(p, DimenUtils.dpToPx(4), p, DimenUtils.dpToPx(8))
    }

    /** Переключатель. */
    fun switch(ctx: Context, title: String, subtitle: String?, value: Boolean, onChange: (Boolean) -> Unit): CheckedSetting =
        Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, title, subtitle).apply {
            isChecked = value
            setOnCheckedListener { onChange(it) }
        }

    /** Группа радиокнопок: [items] — пары (ключ, подпись); [subtitles] — необязательные пояснения. */
    fun radios(
        ctx: Context,
        items: List<Pair<String, String>>,
        current: String,
        subtitles: Map<String, String> = emptyMap(),
        onPick: (String) -> Unit,
    ): RadioGroup {
        val radios = items.map { Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, it.second, subtitles[it.first]) }
        val manager = RadioManager(radios)
        val group = RadioGroup(ctx)
        radios.forEachIndexed { i, radio ->
            radio.e {
                onPick(items[i].first)
                manager.a(radio)
            }
            group.addView(radio)
            if (items[i].first == current) manager.a(radio)
        }
        return group
    }

    /** Ползунок со значением справа. [min]..[max] с шагом [step]. */
    fun slider(ctx: Context, title: String, subtitle: String?, value: Int, min: Int, max: Int, step: Int, unit: String, onChange: (Int) -> Unit): LinearLayout {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label).apply { text = title })
        if (subtitle != null) box.addView(note(ctx, subtitle))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val valueView = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
            text = "$value$unit"
            width = DimenUtils.dpToPx(56)
            setPadding(DimenUtils.dpToPx(8), 0, DimenUtils.dpToPx(16), 0)
        }
        val steps = (max - min) / step
        val seek = SeekBar(ctx, null, 0, R.i.UiKit_SeekBar).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            this.max = steps
            progress = ((value - min) / step).coerceIn(0, steps)
            setPadding(DimenUtils.dpToPx(24), 0, DimenUtils.dpToPx(12), 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { valueView.text = "${min + progress * step}$unit" }
                override fun onStopTrackingTouch(seekBar: SeekBar) { onChange(min + seekBar.progress * step) }
            })
        }
        row.addView(seek)
        row.addView(valueView)
        box.addView(row)
        return box
    }

    /** Текстовое поле: сохраняет при каждом изменении. */
    fun text(ctx: Context, hint: String, value: String, multiline: Boolean = false, onChange: (String) -> Unit): TextInput {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { onChange(s?.toString().orEmpty()) }
        }
        return TextInput(ctx, hint, value, watcher).apply {
            editText.apply {
                inputType = if (multiline) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE else InputType.TYPE_CLASS_TEXT
                if (multiline) { maxLines = 8; setHorizontallyScrolling(false) }
            }
        }
    }

    fun spacer(ctx: Context, dp: Int = 8): View = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(1, DimenUtils.dpToPx(dp)) }
}
