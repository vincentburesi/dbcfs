package fr.ct402.dbcfs.controller

import fr.ct402.dbcfs.manager.ProfileManager
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File
import java.io.FileInputStream
import javax.servlet.http.HttpServletResponse


@Controller
class FileDownloadController(
        val profileManager: ProfileManager,
) {
    @GetMapping(
            "/file/{profileName}/{authToken}/{fileName}",
            produces = arrayOf(MediaType.APPLICATION_OCTET_STREAM_VALUE)
    )
    @ResponseBody
    fun getLargeFile(
            @PathVariable profileName: String,
            @PathVariable authToken: String,
            @PathVariable fileName: String,
            response: HttpServletResponse,
    ) {
        val profile = profileManager.getProfileByNameOrThrow(profileName)
        val filePath = "${profile.localPath}/$fileName"
        val file = File(filePath).apply { if (!exists()) throw HttpNotFoundException("File not found") }
        if (!profile.checkAuth(authToken)) throw HttpForbiddenException("Bad or expired auth token")
        response.setHeader("Content-Disposition", "attachment; filename=$fileName")
        response.setHeader("Content-Length", file.length().toString())

        val input = FileInputStream(filePath)
        val bytes = ByteArray(1024)
        val output = response.outputStream

        while (true) {
            val read = input.read(bytes)
            if (read < 0) break
            else output.write(bytes, 0, read)
        }
        output.flush()
        output.close()
    }

}