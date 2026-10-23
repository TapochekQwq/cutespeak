// АВТОГЕНЕРАЦИЯ из TypeScript-версии (moods.ts). Не править вручную.
@file:Suppress("ktlint")
package com.cutespeak.engine.data

import com.cutespeak.engine.Rx

enum class Mood { CONGRATS, GREETING, BYE, THANKS, SORRY, LOVE, COMPLIMENT, LAUGH, ANGRY, WORRY, SAD, SLEEPY, HUNGRY, GAME, WORK, COLD, BORED, HELP, EXCITED, SURPRISE, CONFUSED, YES, NO, QUESTION, NEUTRAL }
enum class Vibe { SHY, KAWAII, SLEEPY, CHAOS, NEKO, TSUNDERE, BABY }

object MoodData {
    val RULES: List<Pair<Mood, Rx>> = listOf(
        Mood.CONGRATS to Rx("""поздравля|с днём рождения|с днем рождения|с новым годом|с праздником|с днюхой|ура+"""),
        Mood.GREETING to Rx("""(?<![а-яёіїєґ])(?:прив|привет|приветик|приветики|здаров|здорово|здравствуй|здравствуйте|здрасьте|хай|хэй|хеллоу|хелло|ку|салют|доброе утро|добрый день|добрый вечер|доброго времени суток)(?![а-яёіїєґ])"""),
        Mood.BYE to Rx("""(?<![а-яёіїєґ])(?:пока|покеда|споки|спокойной ночи|доброй ночи|до встречи|до завтра|до связи|увидимся|побежал[аи]?|убегаю|иду спать|я спать|удачи|бывай|чао|до свидания|я off|я офф)(?![а-яёіїєґ])"""),
        Mood.THANKS to Rx("""спасиб|благодар|мерси|сенкс|выручил|спс"""),
        Mood.SORRY to Rx("""извин|(?<![а-яёіїєґ])прости(?![а-яёіїєґ])|прошу прощения|сорян|сорри|виноват|мой косяк|не хотел[аи]? обидеть"""),
        Mood.LOVE to Rx("""люблю|любишь|любим|обожа|скуча|обним|милаш|целую|чмок|нравишься|влюб|зайк|солнышк|милый|милая|родн(ой|ая)|дорог(ой|ая) мой|моя хорош"""),
        Mood.COMPLIMENT to Rx("""красив|прекрасн|шикарн|молодец|умница|умничка|гениальн|круто получил|потрясающ|восхитительн|обалденн|лучш(ий|ая|ее)|топ${'$'}|топчик|огонь"""),
        Mood.LAUGH to Rx("""ахах|хаха|хахах|ха-ха|лол|кек|ржу|ржак|смешн|хихи|хех|азаз|хд+|😂|🤣|ору с"""),
        Mood.ANGRY to Rx("""бесит|злюсь|злой|злая|достал|задолбал|ненавиж|раздража|бля|сука|нахер|нахуй|пиздец|ебан|хрен|чёрт|черт|капец|блин|тупой|тупая|идиот|дурак|дура(?![а-яёіїєґ])"""),
        Mood.WORRY to Rx("""боюсь|переживаю|волнуюсь|тревожн|нервничаю|страшно|опасаюсь|беспокоюсь|паника|паникую|не по себе"""),
        Mood.SAD to Rx("""грустн|плачу|плакать|плач|обидно|одиноко|тоскл|печаль|депресс|тяжело|больно|жаль|жалко|расстро|хреново|плохо|ужасно|устал[аи]? от всего|нет сил"""),
        Mood.SLEEPY to Rx("""спать|сплю|засыпа|зеваю|зевота|баиньки|устал|утомил|ночь|ночью|выспа|сонн|не сплю"""),
        Mood.HUNGRY to Rx("""голод|хочу есть|покушать|пожрать|поесть|вкусн|пицц|чай|кофе|шоколад|конфет|булочк|печень|суши|бургер|еда|еду|завтрак|обед|ужин|десерт|торт"""),
        Mood.GAME to Rx("""катк[аиу]|рейд|играть|поиграем|поиграть|геймер|стрим|лобби|тиммейт|сервер|гг вп|дота|майнкрафт|валорант|танки"""),
        Mood.WORK to Rx("""работа|работаю|работать|учёба|учусь|экзамен|сессия|дедлайн|задани|домашк|контрольн|проект|офис|совещани|созвон|отчёт|отчет|зачёт|зачет"""),
        Mood.COLD to Rx("""холодно|мёрзну|мерзну|замёрз|замерз|мороз|снег|дрожу|жарко|жара|душно|дождь|ливень"""),
        Mood.BORED to Rx("""скучно|скука|нечем заняться|заняться нечем|делать нечего|мне скучно|зеваю от скуки"""),
        Mood.HELP to Rx("""помоги|помогите|помощь|подскажи|подскажите|выручай|как сделать|не получается|объясни|посоветуй"""),
        Mood.EXCITED to Rx("""ура+|вау|класс[сс]|круто|офигенно|ничоси|в восторге|не могу дождаться|кайф|ааа+|ааа+!"""),
        Mood.SURPRISE to Rx("""(?<![а-яёіїєґ])(?:ого|вау|ничего себе|ух ты|ничоси|нифига|ни фига|офигеть|охренеть|серьёзно|серьезно|да ладно|не может быть|шок)(?![а-яёіїєґ])"""),
        Mood.CONFUSED to Rx("""не понимаю|не поняла|не понял|что это|как так|в смысле|что значит|почему|зачем|не знаю|непонятно|хз"""),
        Mood.YES to Rx("""^\s*(да|ага|угу|ок|окей|конечно|ладно|хорошо|согласн|давай|идёт|идет|принято|договорились)(?![а-яёіїєґ])"""),
        Mood.NO to Rx("""^\s*(нет|неа|не хочу|не буду|не могу|нельзя|никогда)(?![а-яёіїєґ])"""),
        Mood.QUESTION to Rx("""\?""")
    )

    val AUTO_VIBE: Map<Mood, List<Pair<Vibe, Double>>> = mapOf(
        Mood.GREETING to listOf(Vibe.KAWAII to 35.0, Vibe.SHY to 20.0, Vibe.NEKO to 25.0, Vibe.CHAOS to 20.0),
        Mood.BYE to listOf(Vibe.SLEEPY to 25.0, Vibe.KAWAII to 30.0, Vibe.SHY to 20.0, Vibe.NEKO to 25.0),
        Mood.THANKS to listOf(Vibe.SHY to 45.0, Vibe.KAWAII to 35.0, Vibe.BABY to 20.0),
        Mood.SORRY to listOf(Vibe.SHY to 65.0, Vibe.SLEEPY to 10.0, Vibe.BABY to 25.0),
        Mood.LOVE to listOf(Vibe.SHY to 40.0, Vibe.KAWAII to 35.0, Vibe.NEKO to 15.0, Vibe.BABY to 10.0),
        Mood.COMPLIMENT to listOf(Vibe.SHY to 40.0, Vibe.TSUNDERE to 25.0, Vibe.KAWAII to 35.0),
        Mood.LAUGH to listOf(Vibe.CHAOS to 40.0, Vibe.KAWAII to 35.0, Vibe.NEKO to 25.0),
        Mood.ANGRY to listOf(Vibe.TSUNDERE to 55.0, Vibe.KAWAII to 30.0, Vibe.CHAOS to 15.0),
        Mood.WORRY to listOf(Vibe.SHY to 65.0, Vibe.BABY to 20.0, Vibe.SLEEPY to 15.0),
        Mood.SAD to listOf(Vibe.SHY to 45.0, Vibe.SLEEPY to 35.0, Vibe.BABY to 20.0),
        Mood.SLEEPY to listOf(Vibe.SLEEPY to 80.0, Vibe.BABY to 20.0),
        Mood.HUNGRY to listOf(Vibe.KAWAII to 40.0, Vibe.BABY to 35.0, Vibe.NEKO to 25.0),
        Mood.GAME to listOf(Vibe.CHAOS to 45.0, Vibe.TSUNDERE to 30.0, Vibe.KAWAII to 25.0),
        Mood.WORK to listOf(Vibe.SHY to 35.0, Vibe.SLEEPY to 30.0, Vibe.TSUNDERE to 20.0, Vibe.KAWAII to 15.0),
        Mood.COLD to listOf(Vibe.BABY to 35.0, Vibe.SHY to 35.0, Vibe.NEKO to 30.0),
        Mood.BORED to listOf(Vibe.SLEEPY to 40.0, Vibe.CHAOS to 30.0, Vibe.NEKO to 30.0),
        Mood.HELP to listOf(Vibe.SHY to 55.0, Vibe.BABY to 25.0, Vibe.KAWAII to 20.0),
        Mood.EXCITED to listOf(Vibe.CHAOS to 50.0, Vibe.KAWAII to 35.0, Vibe.NEKO to 15.0),
        Mood.CONGRATS to listOf(Vibe.KAWAII to 45.0, Vibe.CHAOS to 35.0, Vibe.NEKO to 20.0),
        Mood.SURPRISE to listOf(Vibe.CHAOS to 35.0, Vibe.SHY to 50.0, Vibe.NEKO to 15.0),
        Mood.CONFUSED to listOf(Vibe.SHY to 55.0, Vibe.BABY to 25.0, Vibe.KAWAII to 20.0),
        Mood.YES to listOf(Vibe.KAWAII to 45.0, Vibe.SHY to 25.0, Vibe.NEKO to 20.0, Vibe.TSUNDERE to 10.0),
        Mood.NO to listOf(Vibe.TSUNDERE to 40.0, Vibe.KAWAII to 30.0, Vibe.SHY to 30.0),
        Mood.QUESTION to listOf(Vibe.SHY to 45.0, Vibe.KAWAII to 30.0, Vibe.NEKO to 15.0, Vibe.CHAOS to 10.0),
        Mood.NEUTRAL to listOf(Vibe.SHY to 25.0, Vibe.KAWAII to 25.0, Vibe.CHAOS to 12.0, Vibe.SLEEPY to 8.0, Vibe.NEKO to 15.0, Vibe.TSUNDERE to 8.0, Vibe.BABY to 7.0)
    )

    val SERIOUS: Rx = Rx("""(?<![а-яёіїєґ])(?:умер|умерл|умира|смерт|погиб|похорон|соболезн|скончал|панихид|суицид|самоубий|больниц|реанимац|скорая|скорую|диагноз|онколог|инсульт|инфаркт|авари|насили|изнасил|избил|побои|травм|обстрел|взрыв|тревога|ракет|шахед|прилёт|прилет|укрыти|сирен|оккупац|эвакуац|арестов|увольн|развод|пожар|землетряс|бомбёж|бомбеж|бомбоубежищ)|(?<![а-яёіїєґ])(?:суд|иск|война|дрон|дроны|арест|приступ|раненый|раненая|ранен|ранена|скорая)(?![а-яёіїєґ])|(?:не хочу жить|хочу умереть|покончить с собой|воздушная тревога|воздушную тревогу)""")
}
