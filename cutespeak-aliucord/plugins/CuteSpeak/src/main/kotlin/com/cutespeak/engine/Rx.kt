package com.cutespeak.engine

import java.util.regex.Matcher
import java.util.regex.Pattern

/*
 * Тонкая обёртка над java.util.regex.
 *
 * Почему не kotlin.text.Regex: в сборке Aliucord к проекту подключена обфусцированная копия части стандартной
 * библиотеки Kotlin из Discord, и Regex/RegexOption/MatchResult.groups при компиляции подменяются её классами
 * (ошибки вида «expected 'i!'»). Классы java.util.regex такой подмене не подвержены.
 */

/** Совпадение: значение, границы и группы (группа 0 — всё совпадение). */
class Match(val value: String, val start: Int, val endExclusive: Int, private val groups: Array<String?>) {
    /** null, если группа в совпадении не участвовала. */
    fun group(i: Int): String? = if (i >= 0 && i < groups.size) groups[i] else null

    /** Как в Kotlin: неучаствовавшая группа → "". */
    val groupValues: List<String> get() = groups.map { it ?: "" }
}

class Rx(pattern: String, ignoreCase: Boolean = false) {
    private val p: Pattern = Pattern.compile(pattern, if (ignoreCase) Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE else 0)

    private fun toMatch(m: Matcher): Match {
        val n = m.groupCount() + 1
        val arr = arrayOfNulls<String>(n)
        for (i in 0 until n) arr[i] = m.group(i)
        return Match(m.group(), m.start(), m.end(), arr)
    }

    fun containsMatchIn(s: CharSequence): Boolean = p.matcher(s).find()

    fun matches(s: CharSequence): Boolean = p.matcher(s).matches()

    fun findAll(s: CharSequence): List<Match> {
        val m = p.matcher(s)
        val out = ArrayList<Match>()
        while (m.find()) out.add(toMatch(m))
        return out
    }

    /** Замена через функцию: для каждого совпадения вызывается [transform]. */
    fun replace(s: CharSequence, transform: (Match) -> CharSequence): String {
        val m = p.matcher(s)
        val sb = StringBuilder()
        var last = 0
        while (m.find()) {
            sb.append(s, last, m.start())
            sb.append(transform(toMatch(m)))
            last = m.end()
        }
        sb.append(s, last, s.length)
        return sb.toString()
    }

    /** Замена на строку как есть (без обратных ссылок). */
    fun replace(s: CharSequence, literal: String): String = replace(s) { literal }

    fun split(s: CharSequence): List<String> = p.split(s, -1).toList()
}
