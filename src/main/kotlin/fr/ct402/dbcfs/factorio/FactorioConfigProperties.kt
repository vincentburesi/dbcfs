package fr.ct402.dbcfs.factorio

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "dbcfs.factorio")
data class FactorioConfigProperties (
        val username: String,
        val token: String,
        val cookie: String
)