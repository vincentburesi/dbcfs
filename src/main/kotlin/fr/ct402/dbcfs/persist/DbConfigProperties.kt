package fr.ct402.dbcfs.persist

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "dbcfs.db")
data class DbConfigProperties (
        val path: String
)