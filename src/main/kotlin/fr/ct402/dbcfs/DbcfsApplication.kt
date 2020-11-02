package fr.ct402.dbcfs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class DbcfsApplication {}

fun main(args: Array<String>) {
	runApplication<DbcfsApplication>(*args)
}
