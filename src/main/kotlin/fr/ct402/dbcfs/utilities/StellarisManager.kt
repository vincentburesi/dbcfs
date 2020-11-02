package fr.ct402.dbcfs.utilities

import fr.ct402.dbcfs.refactor.discord.Notifier
import net.dv8tion.jda.api.entities.Message
import org.springframework.stereotype.Component

@Component
class StellarisManager() {

    val ethics = arrayOf(
            arrayOf("Pacifiste", "Militariste"),
            arrayOf("Spiritualiste", "Matérialiste"),
            arrayOf("Xénophile", "Xénophobe"),
            arrayOf("Autoritariste", "Égalitariste"),
    )

    private fun generateHumanBuild() = ethics
            .apply { shuffle() }
            .take(3)
            .map { it.random() }

    fun generateHumanBuilds(message: Message, notifier: Notifier) {
        val userBuilds = message.mentionedUsers.map {
            val userEthics = generateHumanBuild().reduce { acc, s -> "$acc - $s" }
            "${it.asMention}: ||$userEthics||"
        }.reduce { acc, s -> "$acc\n$s" }
        val toSend = "**Builds aléatoires générés:**\n$userBuilds\n\n" +
                """
                    __**Règles:**__
                    :small_blue_diamond: **Éthiques** *Prendre les éthiques choisies pour vous*
                    :small_blue_diamond: **Système politique** Oligarchie
                    :small_blue_diamond: **Origine** Unification durable
                    :small_blue_diamond: **Type de monde** Continental
                    :small_blue_diamond: **Type de système** Solaire (Sol)
                    :small_blue_diamond: **Traits d'espèces** *Aucun*
                    :small_blue_diamond: **Civics** *Aucun*
                    :small_blue_diamond: **Portrait d'espèce** Humain
                    :small_blue_diamond: **Drapeau / Lore** Inspiré de pays/régions existants
                """.trimIndent()
        notifier.success(toSend)
    }
}