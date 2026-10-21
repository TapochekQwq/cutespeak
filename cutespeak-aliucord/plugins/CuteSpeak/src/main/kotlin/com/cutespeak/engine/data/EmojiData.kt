// АВТОГЕНЕРАЦИЯ из TypeScript-версии (emoji.ts). Не править вручную.
@file:Suppress("ktlint")
package com.cutespeak.engine.data

object EmojiData {
    val EMOJI: Map<Mood, List<String>> = mapOf(
        Mood.GREETING to pipe("✨|🌸|🐾|💕|🫶|🐰|☀️|🌷|🍓|🐣|🧸|🦋"),
        Mood.BYE to pipe("💤|🌙|🫶|💞|🐾|☁️|🌟|🧸|🌠|🫧"),
        Mood.THANKS to pipe("🥹|💕|🫶|🌷|💗|✨|🍓|🩷|🌼|🥰"),
        Mood.SORRY to pipe("🥺|👉👈|🥹|💔|🙈|😿|🫣|🩹|🌧️"),
        Mood.LOVE to pipe("💕|💗|💞|💖|🩷|😻|🥰|💘|💓|💝|🫶|🌹|😍"),
        Mood.COMPLIMENT to pipe("🥰|✨|💖|🌟|😳|🫶|💫|🏆|👏|🌸"),
        Mood.LAUGH to pipe("😹|🤭|😸|🥰|✨|😆|🤣|😂|🫠"),
        Mood.ANGRY to pipe("😤|💢|😾|🙄|😠|🔥|😒"),
        Mood.WORRY to pipe("🥺|😟|😰|🫣|😬|🙏|🫂|💦"),
        Mood.SAD to pipe("🥺|😢|🥲|💔|😿|☁️|🌧️|😞|🫂|🩹"),
        Mood.SLEEPY to pipe("😴|💤|🌙|☁️|🧸|🥱|🛏️|🌛|🫧|🐑"),
        Mood.HUNGRY to pipe("🍓|🍡|🍰|🧁|😋|🍪|🍩|🍵|🍜|🥐|🍙|🍑"),
        Mood.SURPRISE to pipe("😳|🫢|👀|😮|✨|😲|🙀|‼️|🤯"),
        Mood.CONFUSED to pipe("🥺|👀|😳|❓|🤔|🫠|😵‍💫|🙃"),
        Mood.YES to pipe("✨|💕|🐾|🍓|🫶|👍|🌟|💫|🆗"),
        Mood.NO to pipe("🥺|😤|🙈|💢|🙅|😾|🚫|😑"),
        Mood.QUESTION to pipe("🥺|👀|🐾|🍓|👉👈|🤔|❓|🐱|🧐"),
        Mood.GAME to pipe("🎮|🕹️|👾|⚔️|🏆|💥|🎲|🔥|😎|🧩"),
        Mood.WORK to pipe("💻|📚|☕|📝|✏️|📎|🗂️|😵‍💫|🫡|🍵"),
        Mood.COLD to pipe("🧣|☃️|❄️|🥶|☕|🧤|🫖|🔥|🧦|🌨️"),
        Mood.BORED to pipe("🥱|😑|🫠|🌀|🙃|😶‍🌫️|🐌|☁️"),
        Mood.HELP to pipe("🥺|🙏|👉👈|🆘|🫶|💭|🤝|🌸"),
        Mood.EXCITED to pipe("✨|🎉|🥳|🤩|💥|🌟|💫|🎊|😆|🚀|🌈"),
        Mood.CONGRATS to pipe("🎉|🥳|🎂|🎁|🎈|🎊|✨|💐|🍰|🧁|🪅|🌟"),
        Mood.NEUTRAL to pipe("💕|✨|🥺|🫶|💞|🌸|🩷|🎀|🍓|🐾|😻|🥹|👉👈|🌷|☁️|🧸|🍡|🐰|🐱|🍑|🫧|🪽|🦋|🌼|💫|🧁|🍒|🐻|🐥|🐑|🌈|🎐|🪐|🍥|🥛")
    )

    val VIBE_EMOJI: Map<Vibe, List<String>> = mapOf(
        Vibe.NEKO to pipe("🐱|😺|😸|😻|🐾|🐟|🧶|🥛|🐈|🪶"),
        Vibe.BABY to pipe("🍼|🧸|🐣|🍭|🎈|🧁|🐥|🌈|🫧|🍪"),
        Vibe.TSUNDERE to pipe("😤|🙄|💢|😳|🌸|😒|🫣"),
        Vibe.SLEEPY to pipe("😴|💤|🌙|🛏️|🧸|☁️|🥱"),
        Vibe.SHY to pipe("🥺|👉👈|🙈|🫣|😳|🥹|💗"),
        Vibe.KAWAII to pipe("🎀|🍓|🌸|💖|✨|🍡|🩷|🦄|🍬|🌷"),
        Vibe.CHAOS to pipe("✨|💥|🌈|🎉|🔥|🤯|🫠|🦄|🪩|💫")
    )

    /** Сезонные эмодзи по дате (month 1–12, day 1–31). */
    fun seasonal(m: Int, d: Int): List<String>? = when {
        (m == 12 && d >= 18) || (m == 1 && d <= 10) -> pipe("🎄|❄️|⛄|🎁|🦌|✨|🌟|🍊|🥂")
        m == 2 && d in 10..14 -> pipe("💘|💝|💌|🌹|💕|🍫|🧸")
        m == 3 && d in 5..8 -> pipe("🌷|🌸|💐|🌼|🌺")
        (m == 3 && d >= 20) || (m == 4 && d <= 25) -> pipe("🌸|🌷|🐣|🌱|🦋|🌼|☔")
        (m == 10 && d >= 20) || (m == 11 && d == 1) -> pipe("🎃|👻|🦇|🕸️|🍬|🕯️|🧙|🐈‍⬛")
        (m == 10 && d < 20) || (m == 11 && d <= 20) || (m == 9 && d >= 20) -> pipe("🍂|🍁|🍄|☕|🧣|🌰|🍎")
        m in 6..8 -> pipe("☀️|🌊|🍉|🍦|🌻|🏖️|🍹|🌴")
        else -> null
    }
}
