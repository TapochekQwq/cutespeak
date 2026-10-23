// Запуск на обычной JVM (без Android): см. README → «Тесты движка».
import com.cutespeak.engine.CustomData
import com.cutespeak.engine.CuteEngine
import com.cutespeak.engine.CuteOptions
import com.cutespeak.engine.Lexicon
import com.cutespeak.engine.SeriousGuard
import com.cutespeak.engine.data.ActionData
import com.cutespeak.engine.data.DictionaryData
import com.cutespeak.engine.data.EmojiData
import com.cutespeak.engine.data.KaomojiData
import com.cutespeak.engine.data.PersonaData
import com.cutespeak.engine.data.Vibe
import java.util.Calendar

var failed = 0
fun fail(m: String) { failed++; println("  ✗ $m") }
fun section(s: String) = println("\n── $s")

fun main() {
    val rng = CuteEngine.mulberry32(42)
    val styles: List<Vibe?> = listOf(null) + Vibe.values().toList()

    section("украшения: запрещённые символы")
    val forbidden = Regex("[`*_|\\\\<>]")
    val decor = ArrayList<String>()
    KaomojiData.KAOMOJI.values.forEach { decor += it }
    EmojiData.EMOJI.values.forEach { decor += it }
    EmojiData.VIBE_EMOJI.values.forEach { decor += it }
    ActionData.ACTIONS.values.forEach { decor += it }
    ActionData.VIBE_ACTIONS.values.forEach { decor += it }
    PersonaData.INTERJ.values.forEach { decor += it }
    decor += PersonaData.FILLERS; decor += PersonaData.ENDINGS
    PersonaData.VIBE_ENDINGS.values.forEach { decor += it }
    decor += PersonaData.TSUNDERE_OPEN; decor += PersonaData.TSUNDERE_CLOSE; decor += PersonaData.SWEET_OPEN
    DictionaryData.DICT.values.forEach { decor += it }
    PersonaData.SOFTEN.values.forEach { decor += it }
    DictionaryData.PHRASES.forEach { decor += it.second }
    decor += Lexicon.nounForms.values; decor += Lexicon.adjForms.values; decor += Lexicon.nameForms.values
    for (d in decor) {
        if (forbidden.containsMatchIn(d)) fail("запрещённый символ в «$d»")
        if (d.contains("~~")) fail("двойная тильда в «$d»")
    }
    println("проверено строк: ${decor.size}")

    section("грамматика склонений")
    fun expect(m: Map<String, String>, pairs: List<Pair<String, String?>>) {
        for ((from, to) in pairs) { val got = m[from]; if (got != to) fail("«$from» → ожидали «$to», получили «$got»") }
    }
    expect(Lexicon.nounForms, listOf(
        "мама" to "мамочка", "мамы" to "мамочки", "маме" to "мамочке", "маму" to "мамочку", "мамой" to "мамочкой", "мам" to "мамочек",
        "кот" to "котик", "кота" to "котика", "коту" to "котику", "котом" to "котиком", "коты" to "котики", "котов" to "котиков",
        "друг" to "дружок", "друга" to "дружка", "другу" to "дружку", "другом" to "дружком",
        "палец" to "пальчик", "пальца" to "пальчика", "пальцем" to "пальчиком", "пальцы" to "пальчики", "пальцев" to "пальчиков",
        "кошки" to "кошечки", "кошек" to "кошечек", "тётю" to "тётушку", "ночь" to "ночка", "ночи" to null, "ночью" to null, "ночам" to "ночкам",
        "руки" to "ручки", "рук" to "ручек", "ногами" to "ножками", "окно" to "окошко", "окну" to "окошку", "окна" to null,
        "солнце" to "солнышко", "солнцем" to "солнышком", "слово" to "словечко", "слова" to null, "минуту" to "минутку", "воды" to "водички",
        "дождь" to "дождик", "дождём" to "дождиком", "лужей" to "лужицей", "зубы" to "зубки", "носки" to "носочки", "дома" to null,
    ))
    expect(Lexicon.adjForms, listOf(
        "красивый" to "красивенький", "красивая" to "красивенькая", "красивого" to "красивенького", "красивым" to "красивеньким", "красивые" to "красивенькие",
        "тихий" to "тихонький", "мягкие" to "мяконькие", "хорошего" to "хорошенького", "хорошей" to "хорошенькой", "горячие" to "горяченькие",
    ))
    expect(Lexicon.nameForms, listOf(
        "маша" to "машенька", "маше" to "машеньке", "машу" to "машеньку", "машей" to "машенькой", "андрею" to "андрюше", "андреем" to "андрюшей",
        "илье" to "илюше", "максимом" to "максимкой", "сергеем" to "серёженькой", "сашей" to "сашенькой",
    ))
    if (Lexicon.nameAmbig["андрея"] != ("андрюши" to "андрюшу")) fail("«андрея» должно быть неоднозначным")
    for ((k, v) in Lexicon.nounForms + Lexicon.adjForms + Lexicon.nameForms) if (Regex("[кгхжшчщ]ы").containsMatchIn(v)) fail("орфография: «$k» → «$v»")
    println("форм: сущ. ${Lexicon.nounForms.size}, прил. ${Lexicon.adjForms.size}, имена ${Lexicon.nameForms.size}; словарь ${DictionaryData.DICT.size}; фраз ${DictionaryData.PHRASES.size}")

    section("защищённые куски")
    val protectedCases = listOf(
        "привет <@123456789012345678> как дела?", "смотри https://example.com/path?x=1&y=2 это круто", "вот код: `const x = 1;` и всё",
        "```js\nconsole.log('привет');\n```\nа это текст", "<:pepe:123456789> <a:dance:987654321> классно получилось",
        "@everyone собираемся в <#123456789> вечером", "время <t:1700000000:R> не забудь", ":smile: ну что ты",
    )
    val protRe = Regex("```[\\s\\S]*?```|`[^`\\n]+`|https?://[^\\s<>]+|<a?:\\w+:\\d+>|<[@#][!&]?\\d+>|<t:-?\\d+(?::[a-zA-Z])?>|@everyone|:\\w{2,}:")
    for (src in protectedCases) {
        val prot = protRe.findAll(src).map { it.value }.toList()
        for (i in 0 until 400) {
            val out = CuteEngine.cuteify(src, CuteOptions(style = styles[i % styles.size], softenProfanity = true), rng)
            val lost = prot.firstOrNull { !out.contains(it) }
            if (lost != null) { fail("потерян «$lost» в «$out»"); break }
        }
    }

    section("лимит длины и нерусский текст")
    for (i in 0 until 200) {
        val out = CuteEngine.cuteify("привет как дела что делаешь сегодня ".repeat(55), CuteOptions(maxLength = 2000), rng)
        if (out.length > 2000) { fail("превышен лимит длины ${out.length}"); break }
    }
    if (CuteEngine.cuteify("hello world", rng = rng) != "hello world") fail("английский текст изменился")
    if (CuteEngine.cuteify("12345 !!!", rng = rng) != "12345 !!!") fail("цифры изменились")

    section("целостность разметки (все стили, 4000 прогонов)")
    val inputs = listOf("привет, как у тебя дела сегодня? что будем делать вечером", "да", "Маша, спасибо большое!", "хочу спать, устала очень", "ну что, идём гулять или как?", "Это нечестно!!!", "я тебя люблю", "где мои ключи")
    for (i in 0 until 4000) {
        val out = CuteEngine.cuteify(inputs[i % inputs.size], CuteOptions(style = styles[i % styles.size]), rng)
        if (Regex("~~|\\*\\*|__|`").containsMatchIn(out)) { fail("разметка сломана: $out"); break }
        if (out.count { it == '*' } % 2 != 0) { fail("непарная звёздочка: $out"); break }
        if (Regex("undefined|NaN|\uE000|\uE001").containsMatchIn(out)) { fail("мусор в выводе: $out"); break }
        if (Regex("\\s{2,}").containsMatchIn(out)) { fail("двойной пробел: «$out»"); break }
    }

    section("замена г→к не создаёт другие слова")
    val bare = CuteOptions(swapGK = true, stutter = false, stretch = false, kaomoji = false, emoji = false, actions = false, interjections = false, tilde = false, intensity = 100)
    for (i in 0 until 400) {
        val out = CuteEngine.cuteify("мы были в гостях, там был год и гора", bare, rng)
        if (Regex("костях|\\bкод\\b|кора").containsMatchIn(out)) { fail("г→к создало другое слово: $out"); break }
    }

    section("украинский текст")
    for (i in 0 until 300) {
        val out = CuteEngine.cuteify("привіт, як справи? його вже немає", bare.copy(swapGK = false), rng)
        if (!out.contains("його")) { fail("украинское слово испорчено: $out"); break }
    }

    section("серьёзные темы")
    for (s in listOf("у меня умерла бабушка", "в городе воздушная тревога", "я в больнице", "обстрел был ночью", "соболезную")) {
        if (CuteEngine.cuteify(s, rng = rng) != s) fail("серьёзное сообщение изменилось: $s")
    }
    if (CuteEngine.cuteify("у меня умерла бабушка", CuteOptions(seriousGuard = SeriousGuard.OFF), rng) == "у меня умерла бабушка") fail("защита не отключилась")
    for (s in listOf("диск не открывается", "поиск не работает", "раненько встала")) {
        if (CuteEngine.cuteify(s, CuteOptions(stutter = false), rng) == s) fail("ложное срабатывание защиты: $s")
    }

    section("смайлики из текста")
    val noDecor = CuteOptions(kaomoji = false, emoji = false, actions = false)
    for (i in 0 until 200) {
        val a = CuteEngine.cuteify("привет:)", noDecor, rng)
        val b = CuteEngine.cuteify("как дела))", noDecor, rng)
        val c = CuteEngine.cuteify("люблю <3", noDecor, rng)
        if (Regex(":\\)|\\)\\)").containsMatchIn(a + b)) { fail("смайлик не заменён: «$a» «$b»"); break }
        if (c.contains("<3")) { fail("сердечко не заменено: $c"); break }
    }

    section("свои слова и защита")
    val custom = CustomData.parse(customWords = "кофе = кофеёк | кофеечек\nсупер=суперски", customActions = "*машет хвостом*", customKaomoji = "(•◡•)", protectedWords = "Рамзес, Тузик")
    var sawCustom = false
    for (i in 0 until 300) {
        val out = CuteEngine.cuteify("Рамзес любит кофе и Тузик тоже", CuteOptions(custom = custom), rng)
        if (!out.contains("Рамзес") || !out.contains("Тузик")) { fail("защищённое слово изменено: $out"); break }
        if (Regex("кофеёк|кофеечек").containsMatchIn(out)) sawCustom = true
    }
    if (!sawCustom) fail("свои слова не применяются")

    section("предпросмотр")
    val v = CuteEngine.variants("давай поговорим", 5, rng = rng)
    if (v.toSet().size != v.size || v.size < 5) fail("предпросмотр выдал повторы или мало вариантов")

    section("время суток")
    fun at(h: Int) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 17, h, 30, 0) }.timeInMillis
    val sleepRe = Regex("zzZ|zzz|💤|😴|🌙|зевает|клюёт|сонно|посапывает")
    var night = 0; var day = 0
    for (i in 0 until 600) {
        if (sleepRe.containsMatchIn(CuteEngine.cuteify("как дела", CuteOptions(nowMillis = at(2)), rng))) night++
        if (sleepRe.containsMatchIn(CuteEngine.cuteify("как дела", CuteOptions(nowMillis = at(14)), rng))) day++
    }
    println("сонных ночью: $night/600, днём: $day/600")
    if (night <= day) fail("время суток не влияет на стиль")

    section("фаззинг (20000 строк, все стили)")
    val junk = listOf("", " ", "\n\n", "😀😀", "!!!???", "...", "a b c", "<@1>", "ё", "я", "ъ", "ь-ь", "а-а-а", "—", "(((", ")))", ":)", ":3", "<3", "XD", "((", "привет)", "- ", "# заголовок", "```", "`", "||спойлер||", "**жирный** текст", "__подч__", "~~зач~~ текст")
    val ws = listOf("привет", "как", "дела", "Маша", "кот", "ты", "не", "знаю", "очень", "давай", "что", "мы", "я")
    var bad = 0
    for (i in 0 until 20000) {
        val sb = StringBuilder()
        repeat(1 + (rng() * 5).toInt()) { sb.append(if (rng() < 0.5) junk[(rng() * junk.size).toInt()] else ws[(rng() * ws.size).toInt()]).append(if (rng() < 0.7) " " else "") }
        try {
            val out = CuteEngine.cuteify(sb.toString(), CuteOptions(style = styles[i % styles.size], intensity = 1 + (rng() * 100).toInt(), stutterAmount = (rng() * 201).toInt(), decorAmount = (rng() * 201).toInt(), softenProfanity = true), rng)
            if (Regex("undefined|NaN|\uE000|\uE001").containsMatchIn(out)) { bad++; println("BAD ${sb} -> $out") }
        } catch (e: Throwable) { bad++; println("THROW ${sb}: $e") }
    }
    if (bad > 0) fail("фаззинг: $bad плохих результатов")

    println("\n── демонстрация")
    CuteEngine.resetHistory()
    val demo = CuteEngine.mulberry32(2026)
    val noon = at(14)
    for (s in listOf("Давай поговорим", "Как это выключить?", "Привет", "Спасибо большое!", "Маша, посмотри какой красивый котик на картинке")) {
        println("\n▸ $s")
        repeat(4) { println("   " + CuteEngine.cuteify(s, CuteOptions(nowMillis = noon), demo)) }
    }
    println("\n── по одному на стиль")
    for (st in Vibe.values()) println("   [$st] " + CuteEngine.cuteify("Давай поговорим об этом вечером", CuteOptions(style = st, nowMillis = noon), demo))

    println(if (failed > 0) "\nПРОВАЛЕНО проверок: $failed" else "\nвсе проверки пройдены ✓")
    System.exit(if (failed > 0) 1 else 0)
}
