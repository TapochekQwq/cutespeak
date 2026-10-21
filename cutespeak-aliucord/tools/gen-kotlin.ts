// Генератор Kotlin-данных из TS-источников (чтобы база слов/каомодзи была идентична версии для Vencord).
import { mkdirSync, writeFileSync } from "node:fs";
import { ACTIONS, VIBE_ACTIONS } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/actions";
import { DICT, KEEP_G, NOT_GENITIVE, PHRASES, RISKY_G, STANDALONE } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/dictionary";
import { EMOJI, VIBE_EMOJI } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/emoji";
import { KAOMOJI, KAOMOJI_BY_MOOD, KAOMOJI_BY_VIBE } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/kaomoji";
import { ADJ, FEM_NEUT, MASC, MASC_ALT, NAMES } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/lexicon";
import { AUTO_VIBE, MOOD_RULES, SERIOUS_RE } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/moods";
import { ECHO, ENDINGS, FILLERS, INTERJ, SOFTEN, SWEET_OPEN, TSUNDERE_CLOSE, TSUNDERE_OPEN, VIBE_ENDINGS } from "/home/claude/Vencord/src/userplugins/cuteSpeak/data/persona";

const OUT = process.argv[2];
mkdirSync(OUT, { recursive: true });

const esc = (s: string) => s.replace(/\\/g, () => "\\\\").replace(/"/g, () => '\\"').replace(/\$/g, () => "\\$").replace(/\n/g, () => "\\n");
const q = (s: string) => `"${esc(s)}"`;
const raw = (s: string) => `"""${s.replace(/\$/g, () => "\${'$'}")}"""`;
const enumName = (s: string) => s.toUpperCase();
const lst = (a: readonly string[]) => (a.length ? `pipe(${q(a.join("|"))})` : "emptyList()");
for (const a of [KAOMOJI, EMOJI, ACTIONS, DICT, SOFTEN]) for (const v of Object.values(a)) for (const x of v as string[]) if (x.includes("|")) throw new Error("pipe in data: " + x);

const header = (name: string) => `// АВТОГЕНЕРАЦИЯ из TypeScript-версии (${name}). Не править вручную.\n@file:Suppress("ktlint")\npackage com.cutespeak.engine.data\n\n`;
const w = (file: string, body: string) => writeFileSync(`${OUT}/${file}`, body);

// ── Moods ──
const moods = [...new Set([...MOOD_RULES.map(([m]) => m), ...Object.keys(AUTO_VIBE)])] as string[];
const vibes = ["shy", "kawaii", "sleepy", "chaos", "neko", "tsundere", "baby"];
w("MoodData.kt", header("moods.ts") + `enum class Mood { ${moods.map(enumName).join(", ")} }\nenum class Vibe { ${vibes.map(enumName).join(", ")} }\n\n` +
`object MoodData {
    val RULES: List<Pair<Mood, Regex>> = listOf(
${MOOD_RULES.map(([m, re]) => `        Mood.${enumName(m)} to Regex(${raw(re.source)})`).join(",\n")}
    )

    val AUTO_VIBE: Map<Mood, List<Pair<Vibe, Double>>> = mapOf(
${Object.entries(AUTO_VIBE).map(([m, v]) => `        Mood.${enumName(m)} to listOf(${v.map(([n, wt]) => `Vibe.${enumName(n)} to ${wt}.0`).join(", ")})`).join(",\n")}
    )

    val SERIOUS: Regex = Regex(${raw(SERIOUS_RE.source)})
}
`);

// ── Kaomoji ──
w("KaomojiData.kt", header("kaomoji.ts") + `enum class KaoPool { ${Object.keys(KAOMOJI).map(enumName).join(", ")} }\n\n` +
`object KaomojiData {
    val KAOMOJI: Map<KaoPool, List<String>> = mapOf(
${Object.entries(KAOMOJI).map(([k, v]) => `        KaoPool.${enumName(k)} to ${lst(v)}`).join(",\n")}
    )

    val BY_MOOD: Map<Mood, List<KaoPool>> = mapOf(
${Object.entries(KAOMOJI_BY_MOOD).map(([m, v]) => `        Mood.${enumName(m)} to listOf(${v.map(x => `KaoPool.${enumName(x)}`).join(", ")})`).join(",\n")}
    )

    val BY_VIBE: Map<Vibe, List<KaoPool>> = mapOf(
${Object.entries(KAOMOJI_BY_VIBE).map(([m, v]) => `        Vibe.${enumName(m)} to listOf(${(v as string[]).map(x => `KaoPool.${enumName(x)}`).join(", ")})`).join(",\n")}
    )
}
`);

// ── Actions ──
w("ActionData.kt", header("actions.ts") + `object ActionData {
    val ACTIONS: Map<Mood, List<String>> = mapOf(
${Object.entries(ACTIONS).map(([m, v]) => `        Mood.${enumName(m)} to ${lst(v)}`).join(",\n")}
    )

    val VIBE_ACTIONS: Map<Vibe, List<String>> = mapOf(
${Object.entries(VIBE_ACTIONS).map(([m, v]) => `        Vibe.${enumName(m)} to ${lst(v as string[])}`).join(",\n")}
    )
}
`);

// ── Emoji ──
w("EmojiData.kt", header("emoji.ts") + `object EmojiData {
    val EMOJI: Map<Mood, List<String>> = mapOf(
${Object.entries(EMOJI).map(([m, v]) => `        Mood.${enumName(m)} to ${lst(v)}`).join(",\n")}
    )

    val VIBE_EMOJI: Map<Vibe, List<String>> = mapOf(
${Object.entries(VIBE_EMOJI).map(([m, v]) => `        Vibe.${enumName(m)} to ${lst(v as string[])}`).join(",\n")}
    )

    /** Сезонные эмодзи по дате (month 1–12, day 1–31). */
    fun seasonal(m: Int, d: Int): List<String>? = when {
        (m == 12 && d >= 18) || (m == 1 && d <= 10) -> ${lst(["🎄", "❄️", "⛄", "🎁", "🦌", "✨", "🌟", "🍊", "🥂"])}
        m == 2 && d in 10..14 -> ${lst(["💘", "💝", "💌", "🌹", "💕", "🍫", "🧸"])}
        m == 3 && d in 5..8 -> ${lst(["🌷", "🌸", "💐", "🌼", "🌺"])}
        (m == 3 && d >= 20) || (m == 4 && d <= 25) -> ${lst(["🌸", "🌷", "🐣", "🌱", "🦋", "🌼", "☔"])}
        (m == 10 && d >= 20) || (m == 11 && d == 1) -> ${lst(["🎃", "👻", "🦇", "🕸️", "🍬", "🕯️", "🧙", "🐈‍⬛"])}
        (m == 10 && d < 20) || (m == 11 && d <= 20) || (m == 9 && d >= 20) -> ${lst(["🍂", "🍁", "🍄", "☕", "🧣", "🌰", "🍎"])}
        m in 6..8 -> ${lst(["☀️", "🌊", "🍉", "🍦", "🌻", "🏖️", "🍹", "🌴"])}
        else -> null
    }
}
`);

// ── Persona ──
w("PersonaData.kt", header("persona.ts") + `object PersonaData {
    val INTERJ: Map<String, List<String>> = mapOf(
${Object.entries(INTERJ).map(([k, v]) => `        ${q(k)} to ${lst(v)}`).join(",\n")}
    )
    val FILLERS: List<String> = ${lst(FILLERS)}
    val ENDINGS: List<String> = ${lst(ENDINGS)}
    val VIBE_ENDINGS: Map<String, List<String>> = mapOf(
${Object.entries(VIBE_ENDINGS).map(([k, v]) => `        ${q(k)} to ${lst(v)}`).join(",\n")}
    )
    val TSUNDERE_OPEN: List<String> = ${lst(TSUNDERE_OPEN)}
    val TSUNDERE_CLOSE: List<String> = ${lst(TSUNDERE_CLOSE)}
    val SWEET_OPEN: List<String> = ${lst(SWEET_OPEN)}
    val ECHO: Set<String> = ${lst([...ECHO])}.toSet()
    val SOFTEN: Map<String, List<String>> = mapOf(
${Object.entries(SOFTEN).map(([k, v]) => `        ${q(k)} to ${lst(v)}`).join(",\n")}
    )
}
`);

// ── Dictionary ──
w("DictionaryData.kt", header("dictionary.ts") + `object DictionaryData {
    val DICT: Map<String, List<String>> = mapOf(
${Object.entries(DICT).map(([k, v]) => `        ${q(k)} to ${lst(v)}`).join(",\n")}
    )
    val STANDALONE: Set<String> = ${lst([...STANDALONE])}.toSet()
    val NOT_GENITIVE: Set<String> = ${lst([...NOT_GENITIVE])}.toSet()
    val KEEP_G: Set<String> = ${lst([...KEEP_G])}.toSet()
    val RISKY_G: Regex = Regex(${raw(RISKY_G.source)})

    /** Целые фразы: (регулярка, варианты). */
    val PHRASES: List<Pair<Regex, List<String>>> = listOf(
${PHRASES.map(([re, f]) => `        Regex(${raw(re.source)}, RegexOption.IGNORE_CASE) to ${lst(f)}`).join(",\n")}
    )
}
`);

// ── Lexicon rows (разбираются в Morph.kt) ──
const rowStr = (r: (string | undefined)[], n: number) => Array.from({ length: n }, (_, i) => r[i] ?? "").join("|");
const rows = (a: any[][], n: number) => a.map(r => rowStr(r, n)).join("\n");
const chunk = (name: string, text: string) => {
    // Kotlin: одна строковая константа ≤ 65535 байт — режем на куски по ~12 КБ символов
    const lines = text.split("\n");
    const parts: string[] = [];
    let cur: string[] = [];
    let len = 0;
    for (const l of lines) { if (len + l.length > 12000) { parts.push(cur.join("\n")); cur = []; len = 0; } cur.push(l); len += l.length + 1; }
    if (cur.length) parts.push(cur.join("\n"));
    return `    val ${name}: List<String> = listOf(\n${parts.map(p => `        ${q(p)}`).join(",\n")}\n    ).joinToString("\\n").split("\\n")\n`;
};
w("LexiconData.kt", header("lexicon.ts") + `/** Строки вида «слово|уменьшительное|род.п.мн.|род.п.мн. умен.|флаги» — склоняются в Morph.kt. */
object LexiconData {
${chunk("FEM_NEUT", rows(FEM_NEUT as any, 5))}
${chunk("MASC", rows(MASC as any, 5))}
${chunk("MASC_ALT", rows(MASC_ALT as any, 5))}
${chunk("ADJ", rows(ADJ as any, 3))}
${chunk("NAMES", rows(NAMES as any, 5))}}
`);

writeFileSync(`${OUT}/Util.kt`, `package com.cutespeak.engine.data\n\n/** Список строк, записанных через «|» (в данных этот символ не встречается). */\ninternal fun pipe(s: String): List<String> = s.split('|')\n`);
console.log("ok");
