package fr.ct402.dbcfs

import fr.ct402.dbcfs.factorio.config.ServerSettings
import fr.ct402.dbcfs.manager.ProfileManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class ServerSettingsController (
        val profileManager: ProfileManager
) {

    @GetMapping("/{profile}/server-settings")
    fun serverSettings(model: Model): String {
        model["profileName"] = profileManager.currentProfile ?: return "no-profile-selected" //FIXME
        return "server-settings"
    }

    @PostMapping("/{profile}/server-settings", consumes = ["application/json"], produces = ["application/json"])
    fun setServerSettings(
            @RequestBody serverSettings: ServerSettings,
            @PathVariable profileName: String
    ): ResponseEntity<Unit> {
        return if (profileManager.setServerSettings(profileName, serverSettings))
            ResponseEntity.ok().build()
        else
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }
}
