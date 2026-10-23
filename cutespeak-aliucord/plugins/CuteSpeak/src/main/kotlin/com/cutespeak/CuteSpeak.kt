package com.cutespeak

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.widget.NestedScrollView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SelectDialog
import com.aliucord.patcher.Hook
import com.aliucord.patcher.before
import com.aliucord.utils.DimenUtils
import com.cutespeak.engine.CuteEngine
import com.discord.api.commands.ApplicationCommandType
import com.discord.databinding.WidgetChatListActionsBinding
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.lytefast.flexinput.R
import com.lytefast.flexinput.fragment.FlexInputFragment
import de.robv.android.xposed.XC_MethodHook

/**
 * CuteSpeak для Aliucord — «милый режим»: превращает твои исходящие сообщения в «милые» по-русски.
 *
 * Мобильный интерфейс:
 *  — сердечко в поле ввода: тап — вкл/выкл для этого чата, долгий тап — быстрая панель (режим и стиль чата, сила, эффекты);
 *  — долгое нажатие на СВОЁ сообщение → «CuteSpeak»: переколдовать, вернуть оригинал, сделать милым;
 *  — настройки плагина: примерка на 5 вариантов, все эффекты, свои слова, лимиты;
 *  — команды /cute, /cutepreview, /cutemode; префикс «//» — отправить как есть.
 */
@AliucordPlugin
class CuteSpeak : Plugin() {
    private val prefs = Prefs(settings)
    private val core = CuteCore(prefs)

    private val textContentField = MessageContent::class.java.getDeclaredField("textContent").apply { isAccessible = true }

    init {
        settingsTab = SettingsTab(CuteSettingsPage::class.java).withArgs(core)
    }

    override fun start(context: Context) {
        hookSend()
        hookChatBarButton()
        hookMessageActions()
        registerCommands()
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
        core.clearListeners()
    }

    /* ───────────────────────────── отправка ───────────────────────────── */

    private fun hookSend() {
        patcher.before<ChatInputViewModel>(
            "sendMessage",
            Context::class.java,
            MessageManager::class.java,
            MessageContent::class.java,
            List::class.java,
            Boolean::class.javaPrimitiveType!!,
            Function1::class.java,
        ) {
            try {
                val mc = it.args[2] as MessageContent? ?: return@before
                val original = textContentField.get(mc) as String? ?: return@before
                if (original.isBlank()) return@before
                // то, что уже подготовила команда /cute, повторно не трогаем
                if (core.consumePrepared(original)) return@before
                val channelId = StoreStream.getChannelsSelected().id
                val state = core.resolve(channelId)
                if (!state.enabled) return@before
                val out = core.transform(original, state.style)
                textContentField.set(mc, out)
                core.remember(channelId, out, original)
            } catch (t: Throwable) {
                logger.error(t)
            }
        }
    }

    /* ───────────────────────────── сердечко в поле ввода ───────────────────────────── */

    private val buttons = ArrayList<java.lang.ref.WeakReference<AppCompatImageButton>>()

    private fun refreshButton(btn: AppCompatImageButton) {
        val channelId = StoreStream.getChannelsSelected().id
        val on = core.resolve(channelId).enabled
        btn.setImageDrawable(HeartDrawable(if (on) HeartDrawable.PINK else ColorCompat.getThemedColor(btn.context, R.b.colorInteractiveNormal), !on))
        btn.contentDescription = if (on) "Милый режим: включён" else "Милый режим: выключен"
    }

    private fun hookChatBarButton() {
        core.onChange { buttons.removeAll { it.get() == null }; buttons.forEach { ref -> ref.get()?.let { b -> b.post { refreshButton(b) } } } }

        with(FlexInputFragment::class.java) {
            patcher.patch(
                getDeclaredMethod("onViewCreated", View::class.java, Bundle::class.java),
                Hook { callFrame ->
                    try {
                        val fragmentManager = (callFrame.thisObject as FlexInputFragment).parentFragmentManager
                        val bar = ((callFrame.args[0] as LinearLayout).getChildAt(1) as RelativeLayout).getChildAt(0) as LinearLayout

                        val btn = AppCompatImageButton(ContextThemeWrapper(bar.context, R.i.UiKit_ImageView_Clickable), null, 0).apply {
                            layoutParams = LinearLayout.LayoutParams(DimenUtils.dpToPx(40), ViewGroup.LayoutParams.MATCH_PARENT)
                            setBackgroundColor(0)
                            setPadding(DimenUtils.dpToPx(8), DimenUtils.dpToPx(8), DimenUtils.dpToPx(8), DimenUtils.dpToPx(8))
                            isClickable = true
                            isLongClickable = true
                        }
                        btn.setOnClickListener {
                            val channelId = StoreStream.getChannelsSelected().id
                            val on = core.toggleHere(channelId)
                            refreshButton(btn)
                            Utils.showToast(if (on) "Милый режим: включён ♡" else "Милый режим: выключен")
                        }
                        btn.setOnLongClickListener {
                            QuickSheet(core, StoreStream.getChannelsSelected().id).show(fragmentManager, "CuteSpeakQuick")
                            true
                        }
                        refreshButton(btn)
                        bar.addView(btn)
                        buttons.add(java.lang.ref.WeakReference(btn))

                        // иконка следует за сменой чата: лёгкий опрос, пока кнопка на экране
                        val tick = object : Runnable {
                            override fun run() {
                                if (!btn.isAttachedToWindow) return
                                refreshButton(btn)
                                btn.postDelayed(this, 1500)
                            }
                        }
                        btn.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) { btn.removeCallbacks(tick); btn.postDelayed(tick, 1500) }
                            override fun onViewDetachedFromWindow(v: View) { btn.removeCallbacks(tick) }
                        })
                    } catch (t: Throwable) {
                        logger.error(t)
                    }
                },
            )
        }
    }

    /* ───────────────────────────── долгое нажатие на сообщение ───────────────────────────── */

    private fun hookMessageActions() {
        val itemId = View.generateViewId()

        with(WidgetChatListActions::class.java) {
            val getBinding = getDeclaredMethod("getBinding").apply { isAccessible = true }

            // 1. добавляем пункт в шторку действий
            patcher.patch(
                getDeclaredMethod("onViewCreated", View::class.java, Bundle::class.java),
                Hook { frame ->
                    try {
                        val layout = (frame.args[0] as NestedScrollView).getChildAt(0) as LinearLayout
                        val ctx = layout.context
                        val item = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
                            text = "CuteSpeak"
                            id = itemId
                            visibility = View.GONE
                            val icon = HeartDrawable(HeartDrawable.PINK, false).apply { setBounds(0, 0, DimenUtils.dpToPx(24), DimenUtils.dpToPx(24)) }
                            setCompoundDrawablesRelative(icon, null, null, null)
                            compoundDrawablePadding = DimenUtils.dpToPx(16)
                        }
                        layout.addView(item)
                    } catch (t: Throwable) {
                        logger.error(t)
                    }
                },
            )

            // 2. показываем его только на СВОИХ сообщениях и вешаем действие
            patcher.patch(
                getDeclaredMethod("configureUI", WidgetChatListActions.Model::class.java),
                Hook { frame: XC_MethodHook.MethodHookParam ->
                    try {
                        val model = frame.args[0] as WidgetChatListActions.Model
                        val msg = model.message
                        val binding = getBinding.invoke(frame.thisObject) as WidgetChatListActionsBinding
                        val item = binding.root.findViewById<TextView>(itemId) ?: return@Hook
                        val mine = msg.author.id == StoreStream.getUsers().me.id && !msg.content.isNullOrBlank()
                        item.visibility = if (mine) View.VISIBLE else View.GONE
                        if (!mine) return@Hook

                        item.setOnClickListener {
                            (frame.thisObject as WidgetChatListActions).dismiss()
                            showMessageMenu(msg)
                        }
                    } catch (t: Throwable) {
                        logger.error(t)
                    }
                },
            )
        }
    }

    private fun showMessageMenu(msg: Message) {
        val channelId = msg.channelId
        val content = msg.content.orEmpty()
        val original = core.originalOf(channelId, content)
        val style = core.resolve(channelId).style
        val labels = if (original != null) arrayOf("Переколдовать (новый вариант)", "Вернуть оригинал") else arrayOf("Сделать милым")

        SelectDialog().apply {
            title = "CuteSpeak"
            items = labels
            onResultListener = { index ->
                try {
                    when {
                        original != null && index == 0 -> {
                            val out = CuteEngine.cuteify(original, core.prefs.toOptions(style))
                            core.remember(channelId, out, original)
                            edit(msg, out, "Готово, новый вариант!")
                        }
                        original != null -> edit(msg, original, "Оригинал возвращён")
                        else -> {
                            val out = CuteEngine.cuteify(content, core.prefs.toOptions(style))
                            core.remember(channelId, out, content)
                            edit(msg, out, "Сообщение стало милым")
                        }
                    }
                } catch (t: Throwable) {
                    logger.error(t)
                    Utils.showToast("Не получилось отредактировать сообщение")
                }
            }
        }.show(Utils.appActivity.supportFragmentManager, "CuteSpeakMessage")
    }

    private fun edit(msg: Message, content: String, ok: String) {
        StoreStream.getMessages().editMessage(msg.id, msg.channelId, content, msg.allowedMentions)
        Utils.showToast(ok)
    }

    /* ───────────────────────────── команды ───────────────────────────── */

    private fun registerCommands() {
        val styleChoices = STYLES.map { Utils.createCommandChoice(it.second, it.first) }

        commands.registerCommand(
            "cute",
            "Отправить сообщение в «милом» виде (один раз, даже если режим выключен)",
            listOf(
                Utils.createCommandOption(ApplicationCommandType.STRING, "text", "Что написать", required = true),
                Utils.createCommandOption(ApplicationCommandType.STRING, "style", "Стиль (по умолчанию — из настроек)", choices = styleChoices),
            ),
        ) { ctx ->
            val text = ctx.getRequiredString("text")
            val style = ctx.getString("style") ?: core.resolve(ctx.channelId).style
            val out = CuteEngine.cuteify(text, core.prefs.toOptions(style))
            core.remember(ctx.channelId, out, text)
            core.markPrepared(out)
            CommandsAPI.CommandResult(out, null, true)
        }

        commands.registerCommand(
            "cutepreview",
            "Показать 5 вариантов «милого» текста — только тебе, ничего не отправляется",
            listOf(
                Utils.createCommandOption(ApplicationCommandType.STRING, "text", "Текст для примерки", required = true),
                Utils.createCommandOption(ApplicationCommandType.STRING, "style", "Стиль (по умолчанию — из настроек)", choices = styleChoices),
            ),
        ) { ctx ->
            val text = ctx.getRequiredString("text")
            val style = ctx.getString("style") ?: core.resolve(ctx.channelId).style
            val list = CuteEngine.variants(text, 5, core.prefs.toOptions(style))
            CommandsAPI.CommandResult(
                "**CuteSpeak — варианты** (видишь только ты, скопируй понравившийся)\n\n" + list.mapIndexed { i, v -> "**${i + 1}.** $v" }.joinToString("\n\n"),
                null,
                false,
            )
        }

        commands.registerCommand(
            "cutemode",
            "Включить/выключить милый режим в этом чате и выбрать стиль",
            listOf(
                Utils.createCommandOption(ApplicationCommandType.BOOLEAN, "state", "Включить или выключить"),
                Utils.createCommandOption(ApplicationCommandType.STRING, "style", "Стиль речи", choices = styleChoices),
            ),
        ) { ctx ->
            val state = ctx.getBool("state")
            val style = ctx.getString("style")
            if (state != null) core.setMode(ctx.channelId, if (state == core.defaultEnabled(ctx.channelId)) null else if (state) "on" else "off")
            if (style != null) core.setStyleHere(ctx.channelId, if (style == core.prefs.style) null else style)
            val now = core.resolve(ctx.channelId)
            CommandsAPI.CommandResult(
                "Милый режим в этом чате: **${if (now.enabled) "включён" else "выключен"}**, стиль: **${styleLabel(now.style)}**",
                null,
                false,
            )
        }
    }

}
