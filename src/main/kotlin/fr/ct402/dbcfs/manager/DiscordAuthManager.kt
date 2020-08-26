package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.commons.orderAfterDbLoad
import fr.ct402.dbcfs.discord.DiscordConfigProperties
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.AuthorizedId
import fr.ct402.dbcfs.persist.model.AuthorizedIds
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.removeIf
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Configuration
class DiscordAuthManager (
        val config: DiscordConfigProperties,
        dbLoader: DbLoader
) {
    val authorizedIdSequence = dbLoader.database.sequenceOf(AuthorizedIds)
    val authorizedUserIds = arrayListOf<AuthorizedId>()
    val authorizedRoleIds = arrayListOf<AuthorizedId>()

    @Order(orderAfterDbLoad)
    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        val authorizedId = authorizedIdSequence.toList()
        authorizedUserIds.addAll(authorizedId.filter { it.idType == AuthorizedId.IdType.USER })
        authorizedRoleIds.addAll(authorizedId.filter { it.idType == AuthorizedId.IdType.ROLE })
    }

    fun isAuthorized(event: MessageReceivedEvent): Boolean {
        return event.run {
            when {
                author.isBot -> false
                author.name == config.owner -> true
                authorizedUserIds.any { it.discordId == author.id } -> true
                member?.roles?.map { it.id }
                        ?.intersect(authorizedRoleIds.map { it.discordId })
                        .orEmpty().isNotEmpty() -> true
                else -> false
            }
        }
    }


    fun buildAuthorizedId(id: String, name: String, isRole: Boolean) =
            AuthorizedId().apply {
                discordId = id
                this.name = name
                idType = if (isRole) AuthorizedId.IdType.ROLE else AuthorizedId.IdType.USER
            }

    fun addAuthorizedId(id: String, name: String, isRole: Boolean): Boolean {
        val idList = if (isRole) authorizedRoleIds else authorizedUserIds

        return when (val entity = idList.find { it.discordId == id }) {
            null ->
                buildAuthorizedId(id, name, isRole).let {
                    authorizedIdSequence.add(it)
                    idList.add(it)
                    true
                }
            else ->
                entity.let {
                    if (it.name != name) {
                        it.name = name
                        it.flushChanges()
                    }
                    false
                }
        }
    }

    fun removeAuthorizedId(id: String, isRole: Boolean): Boolean {
        val idList = if (isRole) authorizedRoleIds else authorizedUserIds

        return when (val entity = idList.find { it.discordId == id }) {
            null -> false
            else -> entity.let {
                idList.remove(entity)
                authorizedIdSequence.removeIf { it.discordId eq entity.discordId }
                true
            }
        }
    }

    fun addAuthorizedUser(user: User) =
            addAuthorizedId(user.id, user.name, false)

    fun addAuthorizedRole(role: Role) =
            addAuthorizedId(role.id, role.name, true)

    fun removeAuthorizedUser(user: User) =
            removeAuthorizedId(user.id, false)

    fun removeAuthorizedRole(role: Role) =
            removeAuthorizedId(role.id, true)

    fun addAuthorized(message: Message, notifier: Notifier) {
        val ignored = arrayListOf<String>()

        message.mentionedUsers.forEach {
            if (!addAuthorizedUser(it))
                ignored.add(it.name)
        }
        message.mentionedRoles.forEach {
            if (!addAuthorizedRole(it))
                ignored.add(it.name)
        }

        if (ignored.isEmpty())
            notifier.success("Authorized all properly mentioned users and roles")
        else
            notifier.error("Following users and roles are already authorized and were ignored: ${ ignored.reduce { acc, s -> "$acc, $s" } }")
    }

    fun removeAuthorized(message: Message, notifier: Notifier) {
        val ignored = arrayListOf<String>()

        message.mentionedUsers.forEach {
            if (!removeAuthorizedUser(it))
                ignored.add(it.name)
        }
        message.mentionedRoles.forEach {
            if (!removeAuthorizedRole(it))
                ignored.add(it.name)
        }

        if (ignored.isEmpty())
            notifier.success("Removed all properly mentioned users and roles from authorized list")
        else
            notifier.error("Following users and roles are not authorized and were ignored: ${ ignored.reduce { acc, s -> "$acc, $s" } }")
    }

}