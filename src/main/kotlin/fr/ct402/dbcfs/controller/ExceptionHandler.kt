package fr.ct402.dbcfs.controller

import fr.ct402.dbcfs.commons.getLogger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.lang.RuntimeException

@ControllerAdvice
class ExceptionHandler: ResponseEntityExceptionHandler() {
    val logger = getLogger()

    @ExceptionHandler(HttpException::class)
    fun handleHttpException(e: HttpException, request: WebRequest): ResponseEntity<String> {
        logger.warn(e.message)
        return ResponseEntity(e.message, e.status)
    }
}

open class HttpException(val status: HttpStatus, msg: String = ""): RuntimeException(msg)
class HttpNotFoundException(msg: String): HttpException(HttpStatus.NOT_FOUND, msg)
class HttpForbiddenException(msg: String): HttpException(HttpStatus.FORBIDDEN, msg)
