package io.github.haburashi76.wordQuiz.command

import io.github.haburashi76.wordQuiz.Exam
import io.github.haburashi76.wordQuiz.plugin.WordExamPlugin
import io.github.monun.kommand.PluginKommand
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

object KommandWordExam {

    lateinit var plugin: WordExamPlugin

    fun register(plugin: WordExamPlugin, kommand: PluginKommand) {

        this.plugin = plugin

        kommand.register("exam", "ex") {

            requires { hasPermission(4) }

            val examsArgument = dynamic { _, input ->
                getExam(input)
            }.apply {
                suggests {
                    suggest(getExams())
                }
            }

            val progressingExamsArgument = dynamic { _, input ->
                plugin.startedExams.keys.firstOrNull { it.name == input }
            }.apply {
                suggests {
                    suggest(plugin.startedExams.keys.map { it.name })
                }
            }

            then("start") {
                then("exam" to examsArgument) {
                    then("player" to players()) {
                        then("count" to int(0, 100)) {
                            executes {
                                plugin.registerExam(
                                    it["exam"],
                                    it["player"],
                                    it["count"]
                                )
                            }
                        }
                    }
                }
            }

            then("stop") {
                then("exam" to progressingExamsArgument) {
                    executes {
                        stopExam(it["exam"])
                    }
                }
            }

            then("reload") {
                executes {
                    try {
                        plugin.reloadConfig()
                        sender.sendMessage("config.yml 다시 불러오기 성공!")
                    } catch (e: Exception) {
                        sender.sendMessage("config.yml 다시 불러오기 실패")
                        e.printStackTrace()
                    }
                }
            }
        }

    }

    private fun stopExam(exam: Exam) {
        plugin.startedExams.remove(exam)
    }

    @Suppress("Unchecked_Cast")
    private fun getExam(name: String): Exam {

        val map = plugin.config.getConfigurationSection(name)?: throw Exception()

        val dictionary = map.getValues(false)

        return Exam(
            name,
            dictionary.filter { word ->
                word.value is List<*> && (word.value as List<*>).all { it is String }
            } as Map<String, List<String>>
        )

    }

    private fun getExams(): List<String> {
        return plugin.config.getValues(false).keys.toList()
    }
}