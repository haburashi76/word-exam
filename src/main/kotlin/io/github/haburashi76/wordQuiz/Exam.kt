package io.github.haburashi76.wordQuiz

import java.util.UUID

class Exam(
    val name: String,
    val wordTable: Map<String, List<String>>
) {
    val scoreboard: MutableMap<UUID, Int> = mutableMapOf()
}