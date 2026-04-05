package io.github.haburashi76.wordQuiz

import io.github.haburashi76.wordQuiz.util.getValue
import io.github.haburashi76.wordQuiz.util.lazyVal
import io.github.haburashi76.wordQuiz.util.setValue
import java.util.*

class Exam(
    val name: String,
    val wordTable: Map<String, List<String>>,
) {
    val scoreboard: MutableMap<UUID, Int> = mutableMapOf()
    var numberOfQuestions: Int? by lazyVal()
}