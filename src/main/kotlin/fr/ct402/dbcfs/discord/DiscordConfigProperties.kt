package fr.ct402.dbcfs.discord

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "dbcfs.discord")
data class DiscordConfigProperties (
        val token: String
)