package io.github.haburashi76.wordQuiz.plugin

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID

object EventListener: Listener {

    val chat = linkedMapOf<UUID, String>()

    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        try {
            chat[event.player.uniqueId] = plainText().serialize(event.message())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}