package io.github.haburashi76.wordQuiz.plugin

import io.github.haburashi76.wordQuiz.Exam
import io.github.haburashi76.wordQuiz.command.KommandWordExam
import io.github.monun.kommand.kommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.*

class WordExamPlugin : JavaPlugin() {

    val startedExams: MutableMap<Exam, Pair<List<UUID>, Int>> = mutableMapOf()

    override fun onEnable() {
        kommand { KommandWordExam.register(this@WordExamPlugin, this) }

        server.pluginManager.registerEvents(EventListener, this)

        saveDefaultConfig()

        val cfile = File(dataFolder, "config.yml")

        if (cfile.length() == 0L) {
            config.options().copyDefaults(true)
            saveConfig()
        }
    }

    fun registerExam(exam: Exam, players: List<Player>, count: Int) {
        if (!startedExams.keys.any { it.name == exam.name }) {
            var newCount: Int? = null
            if (count == 0) newCount = exam.wordTable.toList().flatMap { (key, valueList) ->
                valueList.map { value ->
                    key to value
                }
            }.size

            startedExams[exam] = players.map { it.uniqueId } to (newCount?: count) -1

            exam.numberOfQuestions = newCount?: (count)

            players.forEach {
                exam.scoreboard[it.uniqueId] = 0
            }

            object : BukkitRunnable() {
                override fun run() {
                    startExam(exam)
                }
            }.runTaskLater(this, 80L)

        } else throw Exception()
    }

    private fun startExam(exam: Exam) {
        val (uuids, count) = requireNotNull(startedExams[exam])
        val players = uuids.map { Bukkit.getPlayer(it)!! }

        val usedWords: MutableList<Pair<String, String>> = mutableListOf()

        val wordTableList = exam.wordTable.toList().flatMap { (key, valueList) ->
            valueList.map { value ->
                key to value
            }
        }
        question(
            exam,
            wordTableList,
            usedWords,
            players,
            count
        ).runTaskTimer(this@WordExamPlugin, 0L, 1L)
    }

    private fun question(
        exam: Exam,
        wordTableList: List<Pair<String, String>>,
        usedWords: MutableList<Pair<String, String>>,
        players: List<Player>,
        remaining: Int
    ): BukkitRunnable {
        return object : BukkitRunnable() {

            val word =
                wordTableList
                    .filter { word ->
                        usedWords.none { used ->
                            word.first == used.first && used.second == word.second
                        }
                    }.random().first

            var ticks = 0

            init {
                EventListener.chat.clear()
                players.forEach {
                    it.playSound(it, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f)
                }
                players.forEach { player ->
                    player.showTitle(
                        Title.title(
                            Component.text("${
                                (exam.numberOfQuestions!!-remaining)
                            }/${exam.numberOfQuestions} $word"),
                            Component.text().apply { base ->

                                var i = 0
                                usedWords.filter { it.first == word }.run {
                                    if (this.isNotEmpty()) base.append(Component.text("이미 나온 것: "))
                                    forEach {
                                        i++
                                        base.append(Component.text(it.second))
                                        if (i < this.size) {
                                            base.append(Component.text(", "))
                                        }
                                    }
                                }
                            }.build()
                        )
                    )
                }
            }

            var b = false

            var isRemoved = false

            override fun run() {
                if (exam !in startedExams) {
                    isRemoved = true
                    cancel()
                }

                for ((uuid, message) in EventListener.chat) {
                    if (message in exam.wordTable[word]!!
                        && (message !in usedWords.filter { it.first == word }.map { it.second })) {

                        exam.scoreboard[uuid] = exam.scoreboard[uuid]!! + 1
                        b = true
                        val winner = Bukkit.getPlayer(uuid)!!
                        players.forEach { player ->
                            player.clearTitle()
                            player.showTitle(
                                Title.title(
                                    Component.text(winner.name + " 정답!").color(NamedTextColor.GREEN),
                                    Component.text(message)
                                )
                            )
                        }
                        usedWords.add(word to message)
                        cancel()
                        break
                    }
                }

                EventListener.chat.clear()

                val lengthBonus = word.length * 6

                if (ticks in listOf(65+lengthBonus, 72+lengthBonus)) {
                    players.forEach {
                            player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 2.0f, 1.189207f)
                    }
                }

                if (ticks == 79+lengthBonus) cancel()

                ticks++

            }

            override fun cancel() {
                players.forEach { player ->
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 2.0f, 1.587401f)
                    if (!b) {
                        val showWord = exam.wordTable[word]!!.filter { s ->
                            s !in usedWords.filter { it.first == word }.map { it.second }
                        }.random()
                        player.showTitle(
                            Title.title(
                                Component.text("정답: $showWord"),
                                Component.text("")
                            )
                        )
                        usedWords.add(word to showWord)
                    }
                }

                if (remaining > 0 && !isRemoved)  {
                    if (usedWords.size >= wordTableList.size) usedWords.clear()
                    object : BukkitRunnable() {
                        override fun run() {
                            question(
                                exam,
                                wordTableList,
                                usedWords,
                                players,
                                remaining - 1
                            ).runTaskTimer(this@WordExamPlugin, 0L, 1L)
                        }
                    }.runTaskLater(this@WordExamPlugin, 50L)
                } else {
                    object : BukkitRunnable() {
                        override fun run() {
                            players.forEach { player ->
                                player.showTitle(
                                    Title.title(
                                        Component.text("종료!").color(NamedTextColor.AQUA),
                                        Component.text().apply { base ->
                                            val sorted = players.sortedByDescending { exam.scoreboard[it.uniqueId]!! }
                                            sorted.forEach { sortedPlayer ->
                                                base.append(
                                                    Component.text(
                                                        player.name +
                                                                " ${exam.scoreboard[sortedPlayer.uniqueId]!!}점 "
                                                    )
                                                )
                                            }
                                        }.color(NamedTextColor.GREEN).build()
                                    )
                                )
                            }
                            isRemoved = true
                            startedExams.remove(exam)
                        }
                    }.runTaskLater(this@WordExamPlugin, 50L)
                }

                super.cancel()
            }

        }
    }

}
