package com.cutespeak.engine

import com.cutespeak.engine.data.ActionData
import com.cutespeak.engine.data.DictionaryData
import com.cutespeak.engine.data.EmojiData
import com.cutespeak.engine.data.KaoPool
import com.cutespeak.engine.data.KaomojiData
import com.cutespeak.engine.data.Mood
import com.cutespeak.engine.data.MoodData
import com.cutespeak.engine.data.PersonaData
import com.cutespeak.engine.data.Vibe
import java.util.Calendar

/*
 * CuteSpeak v2 — движок «милой» русской речи (порт с TypeScript-версии для Vencord).
 * Чистый Kotlin без зависимостей от Android/Discord → тестируется на обычной JVM.
 *
 * Конвейер:
 *   0. защита серьёзных тем (сообщение уходит как есть)
 *   1. «заначка»: код, ссылки, упоминания, эмодзи, свои защищённые слова и смайлики прячем под маркеры
 *   2. фразы целиком, текстовые смайлики (:) → каомодзи)
 *   3. токенизация, настроение сообщения → вайб (стиль)
 *   4. слова: свои → смягчение мата → имена → словарь/склонения → фонетика → замены букв
 *   5. заикание, растягивание, перекаты согласных, повторы — с бюджетом, чтобы не было каши
 *   6. пунктуация → украшения (каомодзи, эмодзи, действия в звёздочках, «ня~», междометия, «рамки» цундере)
 *   7. проверка лимита длины с запасными вариантами
 */

typealias Rng = () -> Double

enum class SeriousGuard { SKIP, OFF }

data class CuteOptions(
    /** null = «авто» (подбирать под настроение и время суток) */
    val style: Vibe? = null,
    /** 1..100 — общая «сила» превращения */
    val intensity: Int = 55,
    /** множители в процентах (0..200) */
    val stutterAmount: Int = 100,
    val stretchAmount: Int = 100,
    val decorAmount: Int = 100,
    val stutter: Boolean = true,
    val stretch: Boolean = true,
    val consonantRoll: Boolean = true,
    val echo: Boolean = true,
    val swapGK: Boolean = true,
    val swapRL: Boolean = false,
    val lisp: Boolean = false,
    val phonetic: Boolean = true,
    val diminutives: Boolean = true,
    val names: Boolean = true,
    val softenProfanity: Boolean = false,
    val smileys: Boolean = true,
    val kaomoji: Boolean = true,
    val emoji: Boolean = true,
    val actions: Boolean = true,
    val interjections: Boolean = true,
    val tilde: Boolean = true,
    val lowercase: Boolean = true,
    val timeAware: Boolean = true,
    val seasonal: Boolean = true,
    val seriousGuard: SeriousGuard = SeriousGuard.SKIP,
    val maxLength: Int = 2000,
    val custom: CustomData? = null,
    /** для тестов: «текущее» время в миллисекундах */
    val nowMillis: Long? = null,
)

object CuteEngine {
    /* ─────────────────────────────── утилиты ─────────────────────────────── */

    val defaultRng: Rng = { Math.random() }

    /** Детерминированный генератор для тестов. */
    fun mulberry32(seed0: Int): Rng {
        var seed = seed0
        return {
            seed += 0x6D2B79F5
            var t = (seed xor (seed ushr 15)) * (1 or seed)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
        }
    }

    private fun clamp(x: Double, a: Double, b: Double) = minOf(b, maxOf(a, x))

    private fun <T> pickFrom(rng: Rng, arr: List<T>): T = arr[(rng() * arr.size).toInt().coerceAtMost(arr.size - 1)]

    private fun <T> weighted(rng: Rng, items: List<Pair<T, Double>>): T {
        val total = items.sumOf { maxOf(0.0, it.second) }
        if (total <= 0) return items[0].first
        var r = rng() * total
        for ((v, w) in items) {
            r -= maxOf(0.0, w)
            if (r < 0) return v
        }
        return items.last().first
    }

    /** Выбор без недавних повторов: помним последние N выбранных значений для каждого «ключа». */
    private val history = HashMap<String, MutableList<String>>()

    private fun pickFresh(rng: Rng, key: String, pool: List<String>, keep: Int = 6): String {
        val hist = history.getOrPut(key) { ArrayList() }
        val fresh = pool.filter { it !in hist }
        val src = if (fresh.isNotEmpty()) fresh else pool
        val v = pickFrom(rng, src)
        hist.add(v)
        val limit = minOf(keep, maxOf(1, pool.size - 1))
        while (hist.size > limit) hist.removeAt(0)
        return v
    }

    @Synchronized fun resetHistory() = history.clear()

    /* ───────────────────────────── токенизация ───────────────────────────── */

    private const val LETTER = "А-Яа-яЁёІіЇїЄєҐґ"
    private const val VOWELS = "аеёиоуыэюяіїє"
    private val UK_ONLY = Rx("[іїєґІЇЄҐ]")

    /** Маркеры «заначки»: спрятанные куски текста (приватная область Unicode). */
    private const val STASH_OPEN = "\uE000"
    private const val STASH_CLOSE = "\uE001"
    private val STASH_RE = Rx("$STASH_OPEN(\\d+)$STASH_CLOSE")

    /** Всё, что нельзя трогать: код, ссылки, упоминания, кастомные эмодзи, :shortcode:, @everyone... */
    private val PROTECTED_SRC = listOf(
        "```[\\s\\S]*?```",
        "`[^`\\n]+`",
        "https?://[^\\s<>]+",
        "<a?:\\w+:\\d+>",
        "<(?:@[!&]?|#)\\d+>",
        "<t:-?\\d+(?::[a-zA-Z])?>",
        "</[^>\\s]+:\\d+>",
        ":[A-Za-z0-9_]{2,}:",
        "@(?:everyone|here)",
        "@[\\w.]+",
        "#[\\w-]+",
        "$STASH_OPEN\\d+$STASH_CLOSE",
    ).joinToString("|")

    private val PROTECTED_RE = Rx(PROTECTED_SRC)
    private const val WORD = "[$LETTER]+(?:-[$LETTER]+)*"
    private val TOKEN_RE = Rx("($PROTECTED_SRC)|($WORD)|([.!?…]+)|(\\s+)|((?s:.))")

    private enum class Kind { PROT, WORD, STOP, SPACE, OTHER }

    private class Tok(val kind: Kind, val text: String) {
        var out: String = text
        var pre = ""
        var post = ""
        var sent = 0; var pos = 0; var clause = 0; var cpos = 0
        var nameOk = false
        var prev = ""
        var locked = false
        var stutter = 0
        var stretch = false
        var roll = false
    }

    private fun tokenize(text: String): List<Tok> = TOKEN_RE.findAll(text).map { m ->
        val kind = when {
            m.group(1) != null -> Kind.PROT
            m.group(2) != null -> Kind.WORD
            m.group(3) != null -> Kind.STOP
            m.group(4) != null -> Kind.SPACE
            else -> Kind.OTHER
        }
        Tok(kind, m.value)
    }.toList()

    private fun mapUnprotected(text: String, fn: (String) -> String): String {
        val sb = StringBuilder()
        var last = 0
        for (m in PROTECTED_RE.findAll(text)) {
            sb.append(fn(text.substring(last, m.start)))
            sb.append(m.value)
            last = m.endExclusive
        }
        sb.append(fn(text.substring(last)))
        return sb.toString()
    }

    private fun stripProtected(text: String) = PROTECTED_RE.replace(text, " ")

    /* ─────────────────────── настроение и «вайб» сообщения ─────────────────────── */

    fun analyzeMood(text: String): List<Mood> {
        val plain = stripProtected(text).lowercase()
        val found = ArrayList<Mood>()
        for ((mood, re) in MoodData.RULES) if (re.containsMatchIn(plain)) found.add(mood)
        return if (found.isEmpty()) listOf(Mood.NEUTRAL) else found.take(2)
    }

    fun isSerious(text: String): Boolean = MoodData.SERIOUS.containsMatchIn(stripProtected(text).lowercase())

    private class VibeCfg(
        val stutter: Double, val stretch: Double, val dict: Double, val decor: Double,
        val interj: Double, val tilde: Double, val nya: Double,
        val biasKao: Double, val biasEmo: Double, val biasAct: Double,
        /** шанс «картавости» / «шепелявости» на слово, которые стиль включает сам */
        val rl: Double = 0.0, val lisp: Double = 0.0,
        /** «рамки» цундере и «открывашки» малыша */
        val frames: Double = 0.0, val sweet: Double = 0.0,
        /** кошачий «ррр» внутри слов */
        val purr: Double = 0.0,
    )

    private val VIBES: Map<Vibe, VibeCfg> = mapOf(
        Vibe.SHY to VibeCfg(1.6, 0.8, 0.9, 1.0, 1.6, 0.6, 0.4, 1.2, 0.8, 1.5),
        Vibe.KAWAII to VibeCfg(0.8, 1.3, 1.4, 1.1, 0.6, 1.5, 1.4, 1.3, 1.4, 0.8),
        Vibe.SLEEPY to VibeCfg(0.35, 1.7, 0.8, 0.9, 1.7, 1.0, 0.3, 0.8, 1.3, 1.4),
        Vibe.CHAOS to VibeCfg(1.5, 1.6, 1.2, 1.3, 1.0, 1.2, 1.2, 1.3, 1.3, 1.3),
        Vibe.NEKO to VibeCfg(0.5, 1.2, 1.1, 1.2, 1.2, 1.3, 2.2, 1.4, 1.3, 1.3, purr = 0.55),
        Vibe.TSUNDERE to VibeCfg(1.7, 0.4, 0.5, 0.9, 1.3, 0.3, 0.2, 1.0, 0.7, 1.6, frames = 0.55),
        Vibe.BABY to VibeCfg(0.9, 1.1, 1.8, 1.1, 1.0, 1.2, 0.8, 1.2, 1.4, 1.0, rl = 0.5, lisp = 0.5, sweet = 0.18),
    )

    private fun chooseVibe(o: CuteOptions, moods: List<Mood>, rng: Rng, hour: Int): Vibe {
        o.style?.let { return it }
        val base = MoodData.AUTO_VIBE.getValue(moods[0]).toMutableList()
        if (o.timeAware) {
            val boost = if (hour >= 23 || hour < 5) 3.0 else if (hour >= 21 || hour < 7) 1.6 else 1.0
            if (boost > 1) {
                val i = base.indexOfFirst { it.first == Vibe.SLEEPY }
                if (i >= 0) base[i] = Vibe.SLEEPY to base[i].second * boost else base.add(Vibe.SLEEPY to 12.0 * boost)
            }
        }
        return weighted(rng, base)
    }

    /* ─────────────────────── операции над отдельными словами ─────────────────────── */

    private fun isVowel(c: Char) = VOWELS.indexOf(c.lowercaseChar()) >= 0
    private const val ROLLABLE = "мнсшлрфзжв"

    /** Предлоги родительного падежа: «у Андрея» (а не «вижу Андрея»). */
    private val GEN_PREPS = setOf("у", "от", "для", "без", "из", "до", "после", "около", "возле", "кроме", "против", "вместо", "среди", "ради")

    private val NON_LETTERS = Rx("[^А-Яа-яЁёІіЇїЄєҐґ]")

    private fun restoreCase(orig: String, out: String, lower: Boolean): String {
        if (lower) return out.lowercase()
        val letters = NON_LETTERS.replace(orig, "")
        if (letters.length > 1 && letters == letters.uppercase()) return out.uppercase()
        if (orig[0] != orig[0].lowercaseChar()) return out.replaceFirstChar { it.uppercaseChar() }
        return out
    }

    /** «давай» → «д-д-давай». Для некоторых согласных вместо этого «перекат»: «нннет». */
    private fun stutterWord(w: String, n: Int, rng: Rng, allowRoll: Boolean): String {
        val f = w[0]
        if (allowRoll && ROLLABLE.indexOf(f) >= 0 && w.length >= 3 && isVowel(w[1]) && rng() < 0.28) {
            return f.toString().repeat(minOf(n, 2) + 1) + w.substring(1)
        }
        return "$f-".repeat(n) + w
    }

    /**
     * Растягивание гласных: «привет» → «привееет». Ударение неизвестно, поэтому тянем чаще последнюю гласную
     * (прив-е-ет, спас-и-бо-о, пока-а), реже предпоследнюю/первую; «ё» всегда ударная.
     */
    private fun stretchWord(w: String, rng: Rng, t: Double, protectFirst: Boolean): String {
        val idxs = ArrayList<Int>()
        for (i in (if (protectFirst) 1 else 0) until w.length) if (isVowel(w[i])) idxs.add(i)
        if (idxs.isEmpty()) return w

        val last = idxs.last()
        val pen = if (idxs.size >= 2) idxs[idxs.size - 2] else last
        val first = idxs.first()
        val yo = w.indexOf('ё')

        val picks = HashMap<Int, Int>()
        fun extra() = 1 + (rng() * (2 + t * 3)).toInt()
        val roll = rng()
        val main = if (yo >= 0) yo else if (roll < 0.55) last else if (roll < 0.8) pen else first
        picks[main] = extra()

        if (w.length >= 6 && idxs.size >= 3 && rng() < 0.18 + 0.2 * t) {
            val others = idxs.filter { it != main }
            picks[pickFrom(rng, others)] = 1 + (rng() * 2).toInt()
        }

        val sb = StringBuilder()
        for (i in w.indices) {
            sb.append(w[i])
            val k = picks[i]
            if (k != null) sb.append(w[i].toString().repeat(k))
        }
        return sb.toString()
    }

    /** Кошачий «перекат»: «привет» → «пррривет». */
    private fun purrWord(w: String, rng: Rng): String {
        val i = w.indexOf('р')
        if (i < 0) return w
        return w.substring(0, i + 1) + "р".repeat(1 + (rng() * 3).toInt()) + w.substring(i + 1)
    }

    /** После этих слов нельзя вставлять каомодзи посреди фразы («я (^_^) не знаю» выглядит странно). */
    private val NO_SPLIT_AFTER = setOf(
        "и", "в", "на", "не", "но", "да", "я", "ты", "мы", "вы", "он", "она", "они", "с", "со", "к", "ко", "по", "за", "из", "от", "до", "для", "под",
        "при", "без", "над", "про", "как", "что", "это", "так", "то", "же", "бы", "ли", "а", "у", "о", "об", "ну", "вот", "мне", "меня", "тебе", "тебя",
        "мой", "моя", "твой", "твоя", "ещё", "еще", "очень", "доброе", "добрый", "доброго", "доброй", "спокойной", "сладких", "приятных", "приятного",
        "хорошего", "какой", "какая", "какое", "такой", "такая", "такое", "самый", "самая", "большое", "огромное", "всё", "все", "тоже", "уже", "если",
        "когда", "потому", "чтобы", "или", "либо", "даже", "только", "просто", "совсем", "почти", "ведь", "ладно",
    )

    /* ───────────────────────────── украшения ───────────────────────────── */

    private enum class DKind { KAO, EMO, ACT }
    private enum class Slot { START, MID, END }
    private typealias Item = Pair<DKind, Slot>

    private fun it(k: DKind, s: Slot): Item = k to s
    private val KAO_END = DKind.KAO to Slot.END
    private val EMO_END = DKind.EMO to Slot.END
    private val ACT_END = DKind.ACT to Slot.END
    private val ACT_START = DKind.ACT to Slot.START
    private val KAO_MID = DKind.KAO to Slot.MID
    private val ACT_MID = DKind.ACT to Slot.MID

    private val TEMPLATES: Map<String, List<Pair<List<Item>, Double>>> = mapOf(
        "short" to listOf(
            listOf(KAO_END) to 3.0, listOf(EMO_END) to 2.0, listOf(ACT_END) to 2.0,
            listOf(KAO_END, EMO_END) to 2.0, listOf(ACT_END, EMO_END) to 1.0,
            listOf(ACT_START, KAO_END) to 1.0, listOf(ACT_START, EMO_END) to 1.0,
        ),
        "medium" to listOf(
            listOf(KAO_MID, ACT_END) to 3.0, listOf(KAO_END, EMO_END) to 2.0,
            listOf(KAO_MID, EMO_END) to 2.0, listOf(ACT_END, EMO_END) to 1.0,
            listOf(KAO_END) to 2.0, listOf(ACT_START, KAO_END) to 1.0,
            listOf(KAO_MID, ACT_END, EMO_END) to 2.0, listOf(ACT_MID, KAO_END) to 1.0,
        ),
        "long" to listOf(
            listOf(KAO_MID, KAO_END) to 2.0, listOf(ACT_END) to 2.0,
            listOf(KAO_END, EMO_END) to 2.0, listOf(KAO_MID, ACT_END) to 2.0,
            listOf(EMO_END) to 1.0, listOf(ACT_MID, EMO_END) to 1.0,
        ),
        "xlong" to listOf(listOf(KAO_END) to 2.0, listOf(EMO_END) to 2.0, emptyList<Item>() to 1.0),
    )

    /** Своё из настроек подмешивается с вероятностью p. */
    private fun fromCustom(rng: Rng, custom: List<String>?, p: Double = 0.35): String? =
        if (!custom.isNullOrEmpty() && rng() < p) pickFresh(rng, "custom:${custom.size}", custom, 4) else null

    private fun pickKaomoji(rng: Rng, mood: Mood, vibe: Vibe, o: CuteOptions): String {
        fromCustom(rng, o.custom?.kaomoji)?.let { return it }
        var pool: KaoPool = pickFrom(rng, KaomojiData.BY_MOOD.getValue(mood))
        val strong = mood == Mood.ANGRY || mood == Mood.SAD || mood == Mood.SORRY || mood == Mood.WORRY
        if (!strong && rng() < (if (vibe == Vibe.CHAOS) 0.4 else 0.5)) pool = pickFrom(rng, KaomojiData.BY_VIBE.getValue(vibe))
        return pickFresh(rng, "kao", KaomojiData.KAOMOJI.getValue(pool), 10)
    }

    private fun pickAction(rng: Rng, mood: Mood, vibe: Vibe, o: CuteOptions): String {
        fromCustom(rng, o.custom?.actions)?.let { return "*$it*" }
        val vibeList = ActionData.VIBE_ACTIONS[vibe]
        var pool = if (mood != Mood.NEUTRAL && rng() < 0.65) ActionData.ACTIONS.getValue(mood) else ActionData.ACTIONS.getValue(Mood.NEUTRAL)
        if (vibeList != null && rng() < (if (vibe == Vibe.SHY || vibe == Vibe.SLEEPY) 0.3 else 0.6)) pool = vibeList
        return "*" + pickFresh(rng, "act:$vibe", pool, 10) + "*"
    }

    private fun pickEmoji(rng: Rng, mood: Mood, vibe: Vibe, o: CuteOptions, month: Int, day: Int): String {
        fromCustom(rng, o.custom?.emoji)?.let { return it }
        if (o.seasonal && rng() < 0.3) {
            EmojiData.seasonal(month, day)?.let { return pickFresh(rng, "emo:season", it, 4) }
        }
        var pool = if (mood != Mood.NEUTRAL && rng() < 0.7) EmojiData.EMOJI.getValue(mood) else EmojiData.EMOJI.getValue(Mood.NEUTRAL)
        val vp = EmojiData.VIBE_EMOJI[vibe]
        if (vp != null && rng() < 0.35) pool = vp
        return pickFresh(rng, "emo:$vibe", pool, 6)
    }

    private fun pickInterjection(rng: Rng, vibe: Vibe, moods: List<Mood>): String {
        val key = when {
            Mood.ANGRY in moods && vibe != Vibe.TSUNDERE -> "pout"
            Mood.SAD in moods || Mood.SORRY in moods || Mood.WORRY in moods -> "sad"
            moods.any { it == Mood.EXCITED || it == Mood.CONGRATS } -> "happy"
            vibe == Vibe.SLEEPY || Mood.SLEEPY in moods -> "sleepy"
            vibe == Vibe.SHY -> "shy"
            vibe == Vibe.NEKO -> "neko"
            vibe == Vibe.TSUNDERE -> "tsundere"
            vibe == Vibe.BABY -> "baby"
            else -> "general"
        }
        return pickFresh(rng, "interj:$vibe", PersonaData.INTERJ.getValue(key), 5)
    }

    /* ───────────────────────────── пунктуация ───────────────────────────── */

    private fun transformStop(s: String, isLast: Boolean, tilde: Boolean, tildeW: Double, rng: Rng): String {
        val t = if (tilde) tildeW else 0.0
        return when (s) {
            "." -> if (isLast) weighted(rng, listOf("" to 40.0, "~" to 25 * t, "..." to 10.0, "." to 25.0))
            else weighted(rng, listOf("." to 35.0, "~" to 30 * t, "..." to 25.0, "," to 10.0))
            "!" -> weighted(rng, listOf("!" to 30.0, "!!" to 30.0, "!~" to 22 * t, "!!!" to 10.0))
            "?" -> weighted(rng, listOf("?" to 35.0, "??" to 20.0, "?~" to 20 * t, "?.." to 15.0))
            "…", ".." -> "..."
            else -> s
        }
    }

    /* ───────────────────────────── смайлики из текста ───────────────────────────── */

    private val HEARTS = listOf("♡", "💕", "💗", "💞", "🩷")

    private val RE_HEART = Rx("<3+(?![\\w])")
    private val RE_CAT = Rx("(\\S?)[:;]-?3(?=\\s|$|[.,!?])")
    private val RE_LAUGH = Rx("(\\S?)[:=xX]-?[Dd](?=\\s|$|[.,!?])")
    private val RE_HAPPY = Rx("(\\S?)[:;]-?\\)+(?=\\s|$|[.,!?])")
    private val RE_SAD = Rx("(\\S?)[:;]-?\\(+(?=\\s|$|[.,!?])")
    private val RE_TRAIL_CLOSE = Rx("(\\S?)(\\)+)(\\s*)\\z")
    private val RE_TRAIL_OPEN = Rx("(\\S?)(\\(+)(\\s*)\\z")

    private fun convertSmileys(text: String, rng: Rng, stash: MutableList<String>): String {
        fun hide(s: String, glue: String): String {
            stash.add(s)
            return "$glue$STASH_OPEN${stash.size - 1}$STASH_CLOSE"
        }
        fun kao(pool: KaoPool) = pickFresh(rng, "kao:smiley", KaomojiData.KAOMOJI.getValue(pool), 6)

        var opens = 0
        var closes = 0
        mapUnprotected(text) { seg -> opens += seg.count { it == '(' }; closes += seg.count { it == ')' }; seg }

        var res = mapUnprotected(text) { seg0 ->
            var seg = seg0
            seg = RE_HEART.replace(seg) { pickFrom(rng, HEARTS) }
            seg = RE_CAT.replace(seg) { m -> val p = m.groupValues[1]; hide(kao(KaoPool.CAT), if (p.isNotEmpty()) "$p " else "") }
            seg = RE_LAUGH.replace(seg) { m -> val p = m.groupValues[1]; hide(kao(KaoPool.LAUGH), if (p.isNotEmpty()) "$p " else "") }
            seg = RE_HAPPY.replace(seg) { m -> val p = m.groupValues[1]; hide(kao(KaoPool.HAPPY), if (p.isNotEmpty()) "$p " else "") }
            seg = RE_SAD.replace(seg) { m -> val p = m.groupValues[1]; hide(kao(KaoPool.SAD), if (p.isNotEmpty()) "$p " else "") }
            seg
        }
        // «привет))» → «привет (◕‿◕)»: только если закрывающих скобок больше, чем открывающих
        res = RE_TRAIL_CLOSE.replace(res) { m ->
            val p = m.groupValues[1]
            val run = m.groupValues[2]
            if (closes <= opens || p.isEmpty() || p == "(") m.value
            else p + hide(kao(if (run.length >= 3) KaoPool.LAUGH else KaoPool.HAPPY), " ") + m.groupValues[3]
        }
        res = RE_TRAIL_OPEN.replace(res) { m ->
            val p = m.groupValues[1]
            if (opens <= closes || p.isEmpty() || p == ")") m.value
            else p + hide(kao(KaoPool.SAD), " ") + m.groupValues[3]
        }
        return res
    }

    /* ───────────────────────────── основной проход ───────────────────────────── */

    private val ESCAPE_RE = Rx("[.*+?^\${}()|\\[\\]\\\\]")
    private val UK_WORD = UK_ONLY
    private val RE_TO_TSA1 = Rx("ться$")
    private val RE_TO_TSA2 = Rx("тся$")
    private val RE_MARKUP = Rx("[*_~|]")

    private fun build(input: String, o: CuteOptions, rng: Rng, force: Boolean): String {
        val t = clamp(o.intensity.toDouble(), 1.0, 100.0) / 100.0
        val cal = Calendar.getInstance().apply { if (o.nowMillis != null) timeInMillis = o.nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mulStutter = o.stutterAmount / 100.0
        val mulStretch = o.stretchAmount / 100.0
        val mulDecor = o.decorAmount / 100.0
        val ukMsg = UK_ONLY.containsMatchIn(input)

        // 1. настроение и вайб определяем по исходному тексту
        val moods = analyzeMood(input)
        val vibe = chooseVibe(o, moods, rng, hour)
        val v = VIBES.getValue(vibe)

        // 2. «заначка»: свои защищённые слова, затем смайлики
        val stash = ArrayList<String>()
        var text = input
        val pw = o.custom?.protectedWords.orEmpty()
        if (pw.isNotEmpty()) {
            val esc = pw.joinToString("|") { ESCAPE_RE.replace(it) { m -> "\\" + m.value } }
            val re = Rx("(?<![$LETTER])(?:$esc)(?![$LETTER])", true)
            text = mapUnprotected(text) { seg -> re.replace(seg) { m -> stash.add(m.value); "$STASH_OPEN${stash.size - 1}$STASH_CLOSE" } }
        }
        if (o.smileys) text = convertSmileys(text, rng, stash)

        // 3. фразы целиком
        if (o.diminutives && !ukMsg) {
            text = mapUnprotected(text) { seg0 ->
                var seg = seg0
                for ((re, forms) in DictionaryData.PHRASES) {
                    seg = re.replace(seg) { m ->
                        if (rng() > 0.7) m.value
                        else {
                            val f = pickFresh(rng, "phrase:" + forms[0], forms, 2)
                            if (m.value[0] != m.value[0].lowercaseChar()) f.replaceFirstChar { c -> c.uppercaseChar() } else f
                        }
                    }
                }
                seg
            }
        }

        // 4. токены
        val toks = tokenize(text)
        val words = toks.filter { it.kind == Kind.WORD }
        val n = words.size
        if (n == 0) return input

        var sent = 0; var spos = 0; var clause = 0; var cpos = 0
        toks.forEachIndexed { i, k ->
            if (k.kind == Kind.STOP || (k.kind == Kind.SPACE && k.text.contains('\n'))) { sent++; spos = 0; clause++; cpos = 0 }
            else if (k.kind == Kind.OTHER && k.text.length == 1 && ",;:—–()".indexOf(k.text[0]) >= 0) { clause++; cpos = 0 }
            else if (k.kind == Kind.WORD) {
                k.sent = sent; k.pos = spos++; k.clause = clause; k.cpos = cpos++
                val next = toks.drop(i + 1).firstOrNull { it.kind != Kind.SPACE }
                k.nameOk = k.pos > 0 || (next != null && next.kind == Kind.OTHER && next.text == ",")
                val before = toks.getOrNull(i - 2)
                k.prev = if (toks.getOrNull(i - 1)?.kind == Kind.SPACE && before?.kind == Kind.WORD) before.text.lowercase() else ""
            }
        }
        val sentLen = HashMap<Int, Int>()
        val clauseLen = HashMap<Int, Int>()
        for (w in words) {
            sentLen[w.sent] = (sentLen[w.sent] ?: 0) + 1
            clauseLen[w.clause] = (clauseLen[w.clause] ?: 0) + 1
        }

        // 5. слова: свои → смягчение → имена → словарь/склонения → фонетика → замены букв
        val pDict = clamp((0.30 + 0.50 * t) * v.dict, 0.0, 0.95)
        val pPhon = 0.55 + 0.3 * t
        val pGK = clamp(0.35 + 0.55 * t, 0.0, 0.95)
        val pRL = if (o.swapRL) 0.35 + 0.4 * t else v.rl
        val pLisp = if (o.lisp) 0.30 + 0.3 * t else v.lisp
        var echoLeft = if (o.echo) 1 else 0

        for (w in words) {
            val orig = w.text
            var s = orig.lowercase()
            val uk = ukMsg || UK_WORD.containsMatchIn(s)
            val plain = !s.contains('-')
            val cl = clauseLen[w.clause] ?: 1

            // свои слова из настроек — самый высокий приоритет
            val mine = o.custom?.words?.get(s)
            if (mine != null && rng() < 0.85) { w.out = pickFrom(rng, mine); w.locked = true; continue }

            // смягчение мата
            if (o.softenProfanity) {
                val soft = PersonaData.SOFTEN[s]
                if (soft != null) { w.out = pickFresh(rng, "soft:$s", soft, 2); w.locked = true; continue }
            }

            if (!uk && plain) {
                // имена: только с большой буквы и не «просто в начале предложения»
                val isName = orig[0] != orig[0].lowercaseChar() && w.nameOk
                fun byCase(m: Map<String, Pair<String, String>>): String { val a = m.getValue(s); return if (w.prev in GEN_PREPS) a.first else a.second }
                if (o.names && o.diminutives && isName && (s in Lexicon.nameForms || s in Lexicon.nameAmbig) && rng() < 0.55) {
                    s = Lexicon.nameForms[s] ?: byCase(Lexicon.nameAmbig); w.locked = true
                } else if (o.diminutives) {
                    val forms = DictionaryData.DICT[s]
                    val alone = cl == 1 || (s == "пока" && cl <= 3 && w.cpos == cl - 1)
                    if (forms != null && (s !in DictionaryData.STANDALONE || alone) && rng() < pDict) {
                        s = pickFresh(rng, "dict:$s", forms, 3); w.locked = true
                    } else if ((s in Lexicon.nounForms || s in Lexicon.nounAmbig) && rng() < pDict * 0.85) {
                        s = Lexicon.nounForms[s] ?: byCase(Lexicon.nounAmbig); w.locked = true
                    } else if (s in Lexicon.adjForms && rng() < pDict * 0.6) {
                        s = Lexicon.adjForms.getValue(s); w.locked = true
                    }
                }
            }

            if (!uk && o.phonetic && !w.locked && plain) {
                // «чего» → «чево», «его» → «ево»: пишем как слышим
                if ((s.endsWith("ого") || s.endsWith("его")) && s !in DictionaryData.NOT_GENITIVE && rng() < pPhon) s = s.dropLast(2) + "во"
                // «улыбаться» → «улыбаца», «делается» → «делаеца»
                if (RE_TO_TSA1.containsMatchIn(s) && rng() < 0.45) s = RE_TO_TSA1.replace(s, "ца")
                else if (RE_TO_TSA2.containsMatchIn(s) && rng() < 0.45) s = RE_TO_TSA2.replace(s, "ца")
            }

            if (!w.locked && !uk && s.length > 1) {
                if (o.swapGK && s.contains('г') && s !in DictionaryData.KEEP_G && !DictionaryData.RISKY_G.containsMatchIn(s) &&
                    !(s.endsWith("ого") || s.endsWith("его")) && rng() < pGK
                ) s = s.replace('г', 'к')
                if (pRL > 0 && s.contains('р') && rng() < pRL) s = s.replace('р', 'л')
                if (pLisp > 0 && s.any { it in "шщжч" } && rng() < pLisp) {
                    s = s.replace('ш', 'с').replace('щ', 'с').replace('ж', 'з').replace('ч', 'ц')
                }
            }

            // «да-да», «нет-нет», «тихо-тихо»
            if (echoLeft > 0 && s in PersonaData.ECHO && !w.locked && rng() < 0.14 + 0.18 * t) { s = "$s-$s"; w.locked = true; echoLeft-- }

            w.out = s
        }

        // 6. заикание, растягивание, перекаты (с бюджетом)
        val pStutter = (0.06 + 0.22 * t) * v.stutter * mulStutter
        val pStutterStart = clamp((0.30 + 0.55 * t) * v.stutter * mulStutter, 0.0, 0.95)
        var sBudget = minOf(Math.round(n * (0.14 + 0.20 * t) * v.stutter * mulStutter).toInt(), 1 + n / 6)
        if (sBudget == 0 && rng() < pStutterStart * 0.8) sBudget = 1
        val shortBoost = if (n <= 4) 2 else 1 // в коротких фразах заикаемся охотнее («д-д-давай п-п-поговорим»)
        var lastS = -9

        val pStretch = (0.10 + 0.30 * t) * v.stretch * mulStretch
        var xBudget = minOf(Math.round(n * (0.08 + 0.18 * t) * v.stretch * mulStretch).toInt(), 1 + n / 5)
        if (xBudget == 0 && rng() < pStretch * 2) xBudget = 1

        words.forEachIndexed { i, w ->
            if (o.stutter && sBudget > 0 && i - lastS > 1 && !w.out.contains('-') && (w.out.length >= 2 || w.out == "я")) {
                if (rng() < (if (w.pos == 0) pStutterStart else minOf(pStutter * shortBoost, 0.9))) {
                    // у коротких слов («я», «он») не более двух повторов
                    w.stutter = if (w.out.length <= 2) weighted(rng, listOf(1 to 50.0, 2 to 50.0))
                    else weighted(rng, listOf(1 to 40.0, 2 to 45.0, 3 to 15 * v.stutter))
                    sBudget--; lastS = i
                }
            }
            if (o.stretch && xBudget > 0 && !w.locked && (w.out.length > 1 || w.out == "я")) {
                val lastInSent = w.pos == (sentLen[w.sent] ?: 1) - 1
                val p = pStretch * (if (lastInSent) 1.5 else 1.0) * (if (w.pos == 0) 1.3 else 1.0)
                if (rng() < p) { w.stretch = true; xBudget-- }
            }
            if (v.purr > 0 && o.stretch && !w.locked && w.out.contains('р') && rng() < v.purr * (0.4 + t)) w.roll = true
        }

        for (w in words) {
            var s = w.out
            if (w.roll) s = purrWord(s, rng)
            else if (w.stretch) s = stretchWord(s, rng, t, w.stutter > 0 && isVowel(s[0]))
            if (w.stutter > 0) s = stutterWord(s, w.stutter, rng, o.consonantRoll && !w.roll)
            w.out = restoreCase(w.text, s, o.lowercase)
        }

        // 7. пунктуация
        val meaningful = toks.indices.filter { toks[it].kind != Kind.SPACE }
        val lastIdx = meaningful.last()
        val firstIdx = meaningful.first()
        toks.forEachIndexed { i, k ->
            if (k.kind == Kind.STOP) k.out = transformStop(k.text, meaningful.all { it <= i }, o.tilde, v.tilde, rng)
        }
        if (o.tilde && toks[lastIdx].kind == Kind.WORD && rng() < 0.22 * v.tilde * (0.5 + t)) toks[lastIdx].post += "~"

        // 8. «рамки» цундере: «н-не то чтобы мне было важно, но ...» / «... б-бака.»
        val gloomy = moods.any { it == Mood.SAD || it == Mood.SORRY || it == Mood.WORRY }
        var framed = false
        if (v.frames > 0 && !gloomy && n <= 16 && rng() < v.frames * (0.35 + 0.65 * t)) {
            framed = true
            val mode = weighted(rng, listOf("open" to 50.0, "close" to 35.0, "both" to 15.0))
            val fw = toks.indexOfFirst { it.kind == Kind.WORD }
            if (mode != "close") toks[fw].pre = pickFresh(rng, "ts-open", PersonaData.TSUNDERE_OPEN, 4) + " " + toks[fw].pre
            if (mode != "open") {
                val closer = pickFresh(rng, "ts-close", PersonaData.TSUNDERE_CLOSE, 4)
                val lastTok = toks[lastIdx]
                lastTok.post += (if (lastTok.kind == Kind.STOP && lastTok.out.isEmpty()) "" else " ") + closer
            }
        }
        // «открывашки» малыша: «ути-пути, ...»
        if (v.sweet > 0 && !framed && n >= 2 && rng() < v.sweet) {
            val fw = toks.indexOfFirst { it.kind == Kind.WORD }
            toks[fw].pre = pickFresh(rng, "sweet", PersonaData.SWEET_OPEN, 3) + " " + toks[fw].pre
        }

        // 9. украшения
        val cls = if (n <= 3) "short" else if (n <= 12) "medium" else if (n <= 40) "long" else "xlong"
        val pDecor = if (force) 1.0 else clamp((0.50 + 0.45 * t) * v.decor * mulDecor, 0.0, 0.97)
        val hasMarkup = RE_MARKUP.containsMatchIn(stripProtected(input))
        val enabled = mapOf(DKind.KAO to o.kaomoji, DKind.EMO to o.emoji, DKind.ACT to (o.actions && !hasMarkup))

        var items: List<Item> = emptyList()
        if (rng() < pDecor) {
            val options = TEMPLATES.getValue(cls).map { (tpl, wgt) ->
                val filtered = tpl.filter { enabled.getValue(it.first) }
                val bias = filtered.fold(1.0) { m, (k, _) -> m * when (k) { DKind.KAO -> v.biasKao; DKind.EMO -> v.biasEmo; DKind.ACT -> v.biasAct } }
                filtered to wgt * bias * (if (filtered.size == tpl.size) 1.0 else 0.5)
            }.filter { it.first.isNotEmpty() }
            if (options.isNotEmpty()) items = weighted(rng, options)
            // при «повышенной милоте» иногда добавляем ещё один эмодзи
            if (enabled.getValue(DKind.EMO) && mulDecor > 1.2 && cls != "xlong" && rng() < (mulDecor - 1) / 1.6) items = items + EMO_END
            // при «пониженной» — убираем лишнее
            if (mulDecor < 0.6 && items.size > 1 && rng() < 0.7) items = items.take(1)
        }

        fun pickMood() = if (moods.size > 1 && rng() < 0.4) moods[1] else moods[0]
        fun render(k: DKind): String = when (k) {
            DKind.KAO -> pickKaomoji(rng, pickMood(), vibe, o)
            DKind.EMO -> pickEmoji(rng, pickMood(), vibe, o, month, day)
            DKind.ACT -> pickAction(rng, pickMood(), vibe, o)
        }

        // «ня~» / «мур~» в конце (не в грустных сообщениях и не вместе с рамками цундере)
        class EndPart(val kind: DKind?, val text: String)
        val endParts = ArrayList<EndPart>()
        if (!gloomy && Mood.ANGRY !in moods && !framed && items.isNotEmpty() && rng() < (0.06 + 0.22 * t) * v.nya) {
            val pool = PersonaData.VIBE_ENDINGS[vibe.name.lowercase()] ?: PersonaData.ENDINGS
            var e = pickFresh(rng, "end:$vibe", pool, 5)
            if (!o.tilde) e = e.replace("~", "")
            endParts.add(EndPart(null, e))
        }

        val midCandidates = ArrayList<Int>()
        toks.forEachIndexed { i, k ->
            val next = toks.getOrNull(i + 1)
            if (next == null || next.kind != Kind.SPACE || next.text.contains('\n') || i == lastIdx) return@forEachIndexed
            if (k.kind == Kind.WORD && n >= 3 && k.text.lowercase() !in NO_SPLIT_AFTER) midCandidates.add(i)
            if (k.kind == Kind.STOP) midCandidates.add(i)
        }
        val early = midCandidates.filter { toks[it].kind == Kind.WORD && words.indexOf(toks[it]) <= 1 }

        var startItem = ""
        for ((kind, slot) in items) {
            val text1 = render(kind)
            if (slot == Slot.START) { startItem = text1; continue }
            if (slot == Slot.MID && midCandidates.isNotEmpty()) {
                val pool = if (early.isNotEmpty() && rng() < 0.6) early else midCandidates
                toks[pickFrom(rng, pool)].post += " $text1"
                continue
            }
            endParts.add(EndPart(kind, text1))
        }
        if (startItem.isNotEmpty()) toks[firstIdx].pre = startItem + " " + toks[firstIdx].pre

        // междометие в начале («э-э... привет»)
        if (o.interjections && startItem.isEmpty() && !framed) {
            val clash = words[0].stutter > 0 && rng() < 0.7
            if (!clash && rng() < (0.12 + 0.30 * t) * v.interj) {
                var ij = pickInterjection(rng, vibe, moods)
                if (!o.tilde) ij = ij.replace("~", "")
                val lead = toks.indexOfFirst { it.kind == Kind.WORD }
                toks[lead].pre = ij + " " + toks[lead].pre
            }
        }

        // филлер после запятой («..., ну... слушай»)
        if (o.interjections && n >= 6 && rng() < 0.10 + 0.15 * t) {
            val commas = toks.indices.filter { i ->
                val k = toks[i]
                k.kind == Kind.OTHER && k.text == "," && toks.getOrNull(i + 1)?.kind == Kind.SPACE && !toks[i + 1].text.contains('\n') && i < lastIdx
            }
            if (commas.isNotEmpty()) toks[pickFrom(rng, commas)].post += " " + pickFresh(rng, "filler", PersonaData.FILLERS, 4)
        }

        if (endParts.isNotEmpty()) {
            val sb = StringBuilder()
            endParts.forEachIndexed { i, p ->
                val prev = endParts.getOrNull(i - 1)
                val glue = if (i == 0) " " else if (prev?.kind == DKind.KAO && p.kind == DKind.EMO && rng() < 0.4) "" else " "
                sb.append(glue).append(p.text)
            }
            toks[lastIdx].post += sb.toString()
        }

        // 10. собираем и возвращаем спрятанное
        val joined = toks.joinToString("") { it.pre + it.out + it.post }
        return STASH_RE.replace(joined) { m -> stash[m.groupValues[1].toInt()] }
    }

    /* ───────────────────────────── публичный API ───────────────────────────── */

    /**
     * Превращает сообщение в «милое». Гарантии:
     *  — код, ссылки, упоминания, кастомные эмодзи и защищённые слова остаются нетронутыми;
     *  — серьёзные темы (по умолчанию) не трогаются вообще;
     *  — результат не длиннее maxLength (иначе пробуем облегчённый вариант или отправляем как есть);
     *  — если в тексте нет русских слов, возвращает его без изменений.
     */
    @Synchronized
    fun cuteify(text: String, o: CuteOptions = CuteOptions(), rng: Rng = defaultRng): String {
        if (text.isBlank()) return text
        if (o.seriousGuard != SeriousGuard.OFF && isSerious(text)) return text

        var result = build(text, o, rng, false)
        // чтобы короткие сообщения вроде «да» не оставались без изменений
        var attempt = 0
        while (attempt < 2 && result == text) { result = build(text, o, rng, true); attempt++ }

        if (result.length <= o.maxLength) return result

        val lite = build(text, o.copy(kaomoji = false, emoji = false, actions = false, interjections = false), rng, false)
        return if (lite.length <= o.maxLength) lite else text
    }

    /** Несколько разных вариантов одного текста — для предпросмотра. */
    fun variants(text: String, count: Int, o: CuteOptions = CuteOptions(), rng: Rng = defaultRng): List<String> {
        val seen = LinkedHashSet<String>()
        var i = 0
        while (i < count * 6 && seen.size < count) { seen.add(cuteify(text, o, rng)); i++ }
        return seen.toList()
    }
}
