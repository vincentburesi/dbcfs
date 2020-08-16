package fr.ct402.dbcfs

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File

@Controller
class HomeController {

    @GetMapping("/")
    fun home(model: Model): String {
        model["title"] = "DBCFS"
        return "home"
    }

    @GetMapping(
            "/test-download",
            produces = arrayOf(MediaType.APPLICATION_OCTET_STREAM_VALUE)
    )
    @ResponseBody
    fun download(model: Model): ResponseEntity<ByteArray> {
        val headersMap = mapOf("Content-Disposition" to listOf("attachment; filename=test.json"))
        return ResponseEntity(
                File("/mnt/test.json").readBytes(),
                HttpHeaders(CollectionUtils.unmodifiableMultiValueMap(CollectionUtils.toMultiValueMap(headersMap))),
                HttpStatus.OK
        )
    }
}
