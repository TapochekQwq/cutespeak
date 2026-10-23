package com.cutespeak.engine.data

/** Список строк, записанных через «|» (в данных этот символ не встречается). */
internal fun pipe(s: String): List<String> = s.split('|')
