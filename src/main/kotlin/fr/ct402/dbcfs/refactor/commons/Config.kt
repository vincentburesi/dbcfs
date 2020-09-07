package fr.ct402.dbcfs.refactor.commons

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "dbcfs")
data class Config (
        val discord: DiscordConfig,
        val factorio: FactorioConfig,
        val db: DbConfig,
        val server: ServerConfig,
) {
    data class DiscordConfig (
            val token: String,
            val owner: String,
    )

    data class FactorioConfig (
            val username: String,
            val token: String,
            val cookie: String,
    )

    data class DbConfig (
            val path: String,
    )

    data class ServerConfig (
            val domain: String,
    )
}
