package fr.ct402.dbcfs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class Application {}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
