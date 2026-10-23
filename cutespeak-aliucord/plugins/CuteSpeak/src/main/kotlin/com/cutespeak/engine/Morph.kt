package com.cutespeak.engine

import com.cutespeak.engine.data.LexiconData

/*
 * Мини-морфология: склонение существительных, имён и прилагательных для «умилений».
 * Для пары «слово → уменьшительное» склоняются ОБА слова, формы сопоставляются по номеру слота
 * (падеж + число), поэтому «маме» → «мамочке», «котов» → «котиков», «красивого» → «красивенького».
 * Если форма неоднозначна и результат разный — она не конвертируется (лучше пропустить, чем ошибиться).
 */

private typealias Slots = List<List<String>?> // 12 слотов: ед. им/род/дат/вин/тв/пр, мн. им/род/дат/вин/тв/пр

private val VELAR_HUSH = Rx("[кгхжшчщ]$")
private val HUSH = Rx("[жшчщ]$")
private val HUSH_TS = Rx("[жшчщц]$")
private val TS = Rx("ц$")

/** После к г х ж ш ч щ вместо «ы» пишется «и». */
private fun ortho(stem: String, end: String) = if (VELAR_HUSH.containsMatchIn(stem) && end.startsWith("ы")) "и" + end.substring(1) else end

internal data class NounRow(
    val base: String,
    val dim: String,
    val gpb: String? = null,
    val gpd: String? = null,
    val anim: Boolean = false,
    val masc: Boolean = false,
    /** чередования в мужском роде: основа косвенных падежей и им. п. уменьшительного */
    val bo: String? = null,
    val dn: String? = null,
    val dob: String? = null,
)

private enum class Kind { A, YA, N, NE, S, MS, M, MY, MI }

private fun kindOf(word: String, masc: Boolean): Kind = when {
    word.endsWith("ий") -> Kind.MI
    word.endsWith("й") -> Kind.MY
    word.endsWith("а") -> Kind.A
    word.endsWith("я") -> Kind.YA
    word.endsWith("о") -> Kind.N
    word.endsWith("е") -> Kind.NE
    word.endsWith("ь") -> if (masc) Kind.MS else Kind.S
    else -> Kind.M
}

private fun paradigm(word: String, kind: Kind, anim: Boolean, gp: String?, obl: String?): Slots {
    val gpl = gp?.let { listOf(it) }
    fun make(stem: String): (Array<out String>) -> List<String> = { ends -> ends.map { stem + ortho(stem, it) } }
    fun f(stem: String, vararg ends: String) = make(stem)(ends)
    val none: List<String>? = null
    return when (kind) {
        Kind.A -> {
            val stem = word.dropLast(1)
            val soft = HUSH_TS.containsMatchIn(stem) // «Машей», «лужей», «птицей» (безударное -ей)
            listOf(
                f(stem, "а"), f(stem, "ы"), f(stem, "е"), f(stem, "у"),
                if (soft) f(stem, "ей", "ой", "ею", "ою") else f(stem, "ой", "ою"),
                f(stem, "е"), f(stem, "ы"), gpl, f(stem, "ам"), if (anim) gpl else f(stem, "ы"), f(stem, "ами"), f(stem, "ах"),
            )
        }
        Kind.YA -> {
            val stem = word.dropLast(1)
            listOf(
                f(stem, "я"), f(stem, "и"), f(stem, "е"), f(stem, "ю"), f(stem, "ей", "ёй", "ею"), f(stem, "е"), f(stem, "и"),
                gpl, f(stem, "ям"), if (anim) gpl else f(stem, "и"), f(stem, "ями"), f(stem, "ях"),
            )
        }
        Kind.N -> {
            val stem = word.dropLast(1)
            val pl = if (word.endsWith("ко")) f(stem, "и") else f(stem, "а") // окошко → окошки
            listOf(f(stem, "о"), f(stem, "а"), f(stem, "у"), f(stem, "о"), f(stem, "ом"), f(stem, "е"), pl, gpl, f(stem, "ам"), pl, f(stem, "ами"), f(stem, "ах"))
        }
        Kind.NE -> { // только основы на -ц: солнце, сердце, зеркальце
            val stem = word.dropLast(1)
            listOf(f(stem, "е"), f(stem, "а"), f(stem, "у"), f(stem, "е"), f(stem, "ем"), f(stem, "е"), f(stem, "а"), gpl, f(stem, "ам"), f(stem, "а"), f(stem, "ами"), f(stem, "ах"))
        }
        Kind.S -> {
            val stem = word.dropLast(1)
            val hush = HUSH.containsMatchIn(stem) // ночам, вещами, мышах
            listOf(
                f(stem, "ь"), f(stem, "и"), f(stem, "и"), f(stem, "ь"), f(stem, "ью"), f(stem, "и"), f(stem, "и"), f(stem, "ей"),
                if (hush) f(stem, "ам") else f(stem, "ям"), f(stem, "и"), if (hush) f(stem, "ами") else f(stem, "ями"), if (hush) f(stem, "ах") else f(stem, "ях"),
            )
        }
        Kind.MS -> {
            val stem = word.dropLast(1)
            listOf(
                f(stem, "ь"), f(stem, "я"), f(stem, "ю"), if (anim) f(stem, "я") else f(stem, "ь"), f(stem, "ем", "ём"), f(stem, "е"), f(stem, "и"),
                f(stem, "ей"), f(stem, "ям"), if (anim) f(stem, "ей") else f(stem, "и"), f(stem, "ями"), f(stem, "ях"),
            )
        }
        Kind.MY -> { // Андрей, Сергей (только ед. число)
            val stem = word.dropLast(1)
            listOf(f(stem, "й"), f(stem, "я"), f(stem, "ю"), f(stem, "я"), f(stem, "ем"), f(stem, "е"), none, none, none, none, none, none)
        }
        Kind.MI -> { // Дмитрий, Евгений (только ед. число)
            val stem = word.dropLast(2)
            listOf(f(stem, "ий"), f(stem, "ия"), f(stem, "ию"), f(stem, "ия"), f(stem, "ием"), f(stem, "ии"), none, none, none, none, none, none)
        }
        Kind.M -> {
            val stem = obl ?: word
            val hush = HUSH.containsMatchIn(stem)
            val ts = TS.containsMatchIn(stem)
            val nom = listOf(word)
            val gpM = if (ts) f(stem, "ев", "ов") else if (hush) f(stem, "ей", "ов") else f(stem, "ов")
            listOf(
                nom, f(stem, "а"), f(stem, "у"), if (anim) f(stem, "а") else nom,
                if (ts || hush) f(stem, "ом", "ем") else f(stem, "ом"), f(stem, "е"),
                f(stem, "ы"), gpM, f(stem, "ам"), if (anim) gpM else f(stem, "ы"), f(stem, "ами"), f(stem, "ах"),
            )
        }
    }
}

/** Ничего не преобразуем, если форма — другое частотное слово. */
private val NO_CONVERT = setOf("ночью", "дома", "леса", "лета")

internal class NounForms(
    /** однозначные формы: слово → уменьшительное */
    val forms: Map<String, String>,
    /** одушевлённые мужские формы, где род. и вин. совпадают, а результат разный: [родительный, винительный] */
    val ambig: Map<String, Pair<String, String>>,
)

internal fun buildNounForms(rows: List<NounRow>): NounForms {
    val cand = HashMap<String, LinkedHashMap<String, MutableSet<Int>>>()
    for (r in rows) {
        val bk = kindOf(r.base, r.masc)
        val dk = kindOf(r.dim, false)
        val base = paradigm(r.base, bk, r.anim, r.gpb, r.bo)
        val dim = paradigm(r.dn ?: r.dim, dk, r.anim, r.gpd, r.dob)
        for (i in 0 until 12) {
            val b = base[i] ?: continue
            val d = dim[i] ?: continue
            for (form in b) {
                if (form in NO_CONVERT) continue
                cand.getOrPut(form) { LinkedHashMap() }.getOrPut(d[0]) { HashSet() }.add(i)
            }
        }
    }
    val forms = HashMap<String, String>()
    val ambig = HashMap<String, Pair<String, String>>()
    for ((form, byDim) in cand) {
        val dims = byDim.entries.toList()
        if (dims.size == 1) { forms[form] = dims[0].key; continue }
        // «ночь» (им. = вин.), «солнце» (им. = пр.): именительный падеж побеждает
        val nom = dims.firstOrNull { 0 in it.value }
        if (nom != null && dims.all { it.key == nom.key || it.value.all { s -> s == 3 || s == 5 } }) { forms[form] = nom.key; continue }
        // «Андрея»: родительный или винительный — решим по предлогу перед словом
        val gen = dims.firstOrNull { it.value.size == 1 && 1 in it.value }
        val acc = dims.firstOrNull { it.value.size == 1 && 3 in it.value }
        if (dims.size == 2 && gen != null && acc != null) ambig[form] = gen.key to acc.key
    }
    return NounForms(forms, ambig)
}

/* ───────────────────────────── прилагательные ───────────────────────────── */

private val ADJ_SLOTS = mapOf(
    "h" to listOf("ый", "ого", "ому", "ым", "ом", "ая", "ой", "ую", "ое", "ые", "ых", "ым", "ыми"),
    "v" to listOf("ий", "ого", "ому", "им", "ом", "ая", "ой", "ую", "ое", "ие", "их", "им", "ими"),
    "s" to listOf("ий", "его", "ему", "им", "ем", "ая", "ей", "ую", "ее", "ие", "их", "им", "ими"),
)
private val ADJ_DIM = listOf("ий", "ого", "ому", "им", "ом", "ая", "ой", "ую", "ое", "ие", "их", "им", "ими")

internal fun buildAdjForms(rows: List<Triple<String, String, String>>): Map<String, String> {
    val out = HashMap<String, String>()
    for ((base, dim, type) in rows) {
        val be = ADJ_SLOTS.getValue(type)
        for (i in be.indices) out[base + be[i]] = dim + ADJ_DIM[i]
    }
    return out
}

/* ───────────────────────────── лексикон ───────────────────────────── */

private fun String.nz(): String? = ifEmpty { null }

private fun parseRows(lines: List<String>): List<List<String>> = lines.filter { it.isNotBlank() }.map { it.split('|') }

private fun nounRows(lines: List<String>) = parseRows(lines).map { r ->
    NounRow(base = r[0], dim = r[1], gpb = r.getOrElse(2) { "" }.nz(), gpd = r.getOrElse(3) { "" }.nz(), anim = r.getOrElse(4) { "" }.contains('a'), masc = r.getOrElse(4) { "" }.contains('m'))
}

private fun altRows(lines: List<String>) = parseRows(lines).map { r ->
    NounRow(base = r[0], bo = r[1], dim = r[2], dn = r[2], dob = r[3], anim = r.getOrElse(4) { "" }.contains('a'))
}

internal object Lexicon {
    private val nouns by lazy { buildNounForms(nounRows(LexiconData.FEM_NEUT) + nounRows(LexiconData.MASC) + altRows(LexiconData.MASC_ALT)) }
    private val names by lazy { buildNounForms(nounRows(LexiconData.NAMES)) }

    val nounForms: Map<String, String> get() = nouns.forms
    val nounAmbig: Map<String, Pair<String, String>> get() = nouns.ambig
    val nameForms: Map<String, String> get() = names.forms
    val nameAmbig: Map<String, Pair<String, String>> get() = names.ambig
    val adjForms: Map<String, String> by lazy { buildAdjForms(parseRows(LexiconData.ADJ).map { Triple(it[0], it[1], it[2]) }) }
}
