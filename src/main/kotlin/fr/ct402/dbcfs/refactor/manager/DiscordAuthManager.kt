package fr.ct402.dbcfs.refactor.manager

import fr.ct402.dbcfs.refactor.commons.Config
import fr.ct402.dbcfs.refactor.commons.orderAfterDbLoad
import fr.ct402.dbcfs.refactor.discord.Notifier
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.AllowedId
import fr.ct402.dbcfs.persist.model.AllowedId.IdType.*
import fr.ct402.dbcfs.persist.model.AllowedIds
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.removeIf
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Configuration
class DiscordAuthManager (
        val config: Config,
        dbLoader: DbLoader
) {
    val allowedIdSequence = dbLoader.database.sequenceOf(AllowedIds)
    val allowedUserIds = arrayListOf<AllowedId>()
    val allowedRoleIds = arrayListOf<AllowedId>()
    val allowedChannelIds = arrayListOf<AllowedId>()

    @Order(orderAfterDbLoad)
    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        val allowedId = allowedIdSequence.toList()
        allowedUserIds.addAll(allowedId.filter { it.idType == USER })
        allowedRoleIds.addAll(allowedId.filter { it.idType == ROLE })
    }

    fun User.isOwner() = (name == config.discord.owner)
    fun User.isAllowed() = allowedUserIds.any { it.discordId == id }

    fun Member.isAllowed() = roles
            .map { it.id }
            .intersect(allowedRoleIds.map { it.discordId })
            .isNotEmpty()

    fun TextChannel.isAllowed() = allowedChannelIds.any { it.discordId == id }

    fun isAllowed(event: MessageReceivedEvent): Boolean {
        return event.run {
            when {
                author.isOwner() ->
                    true
                author.isBot ->
                    false
                isFromGuild ->
                    if (isFromType(ChannelType.TEXT) && textChannel.isAllowed())
                        author.isAllowed() || member?.isAllowed() == true
                    else
                        false
                else ->
                    author.isAllowed()
            }
        }
    }

    fun buildAllowedId(id: String, name: String, type: AllowedId.IdType) =
            AllowedId().apply {
                discordId = id
                this.name = name
                idType = type
            }

    private fun getAllowedIdList(type: AllowedId.IdType) = when (type) {
        USER -> allowedUserIds
        ROLE -> allowedRoleIds
        CHANNEL -> allowedChannelIds
    }

    private fun addAllowedId(id: String, name: String, type: AllowedId.IdType): Boolean {
        val idList = getAllowedIdList(type)

        return when (val entity = idList.find { it.discordId == id }) {
            null ->
                buildAllowedId(id, name, type).let {
                    allowedIdSequence.add(it)
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

    private fun removeAllowedId(id: String, type: AllowedId.IdType): Boolean {
        val idList = getAllowedIdList(type)

        return when (val entity = idList.find { it.discordId == id }) {
            null -> false
            else -> entity.let {
                idList.remove(entity)
                allowedIdSequence.removeIf { it.discordId eq entity.discordId }
                true
            }
        }
    }

    fun allowUser(user: User) =
            addAllowedId(user.id, user.name, USER)

    fun allowRole(role: Role) =
            addAllowedId(role.id, role.name, ROLE)

    fun allowChannel(channel: TextChannel) =
            addAllowedId(channel.id, channel.name, CHANNEL)

    fun disallowUser(user: User) =
            removeAllowedId(user.id, USER)

    fun disallowRole(role: Role) =
            removeAllowedId(role.id, ROLE)

    fun disallowChannel(channel: TextChannel) =
            removeAllowedId(channel.id, CHANNEL)

    fun allowFromMentions(message: Message, notifier: Notifier) {
        val ignored = arrayListOf<String>()

        message.mentionedUsers.forEach {
            if (!allowUser(it))
                ignored.add(it.name)
        }
        message.mentionedRoles.forEach {
            if (!allowRole(it))
                ignored.add(it.name)
        }
        message.mentionedChannels.forEach {
            if (!allowChannel(it))
                ignored.add(it.name)
        }

        if (ignored.isEmpty())
            notifier.success("Allowed all properly mentioned users, roles and channels")
        else
            notifier.error("Following users, roles and channels are already allowed and were ignored: ${ ignored.reduce { acc, s -> "$acc, $s" } }")
    }

    fun disallowFromMentions(message: Message, notifier: Notifier) {
        val ignored = arrayListOf<String>()

        message.mentionedUsers.forEach {
            if (!disallowUser(it))
                ignored.add(it.name)
        }
        message.mentionedRoles.forEach {
            if (!disallowRole(it))
                ignored.add(it.name)
        }
        message.mentionedChannels.forEach {
            if (!disallowChannel(it))
                ignored.add(it.name)
        }

        if (ignored.isEmpty())
            notifier.success("Removed all properly mentioned users, roles and channels from allowed list")
        else
            notifier.error("Following users, roles and channels were not in allowed list: ${ ignored.reduce { acc, s -> "$acc, $s" } }")
    }

}