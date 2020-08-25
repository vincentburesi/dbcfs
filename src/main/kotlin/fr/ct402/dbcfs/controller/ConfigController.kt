package fr.ct402.dbcfs.controller

import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.commons.parseDateTime
import fr.ct402.dbcfs.factorio.config.MapGenSettings
import fr.ct402.dbcfs.factorio.config.MapSettings
import fr.ct402.dbcfs.factorio.config.ServerSettings
import fr.ct402.dbcfs.manager.ProfileManager
import fr.ct402.dbcfs.persist.model.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Controller
@RequestMapping("/edit", method = [RequestMethod.GET, RequestMethod.POST])
class ConfigController (
        val profileManager: ProfileManager
) {
    val logger = getLogger()

    fun Profile.checkAuth(authToken: String) =
            !token.isNullOrBlank() && token == authToken && LocalDateTime.now().isBefore(tokenExpiration)

    @GetMapping("/{profileName}/{authToken}")
    fun getEditIndex(model: Model, @PathVariable profileName: String, @PathVariable authToken: String): String {
        logger.warn("getEditIndex - $profileName - $authToken")
        val profile = profileManager.getProfileByName(profileName) ?: throw HttpNotFoundException("Profile not found")
        if (!profile.checkAuth(authToken)) throw HttpForbiddenException("Bad or expired auth token")

        model["profile"] = profile
        model["authToken"] = authToken
        return "edit-index"
    }

    fun postSettings(profileName: String, authToken: String, settings: Any): ResponseEntity<Nothing> {
        val profile = profileManager.getProfileByName(profileName) ?: throw HttpNotFoundException("Profile not found")
        if (!profile.checkAuth(authToken)) throw HttpForbiddenException("Bad or expired auth token")
        profileManager.uploadConfigFile(profile, settings)
        return ResponseEntity(null, HttpStatus.OK)
    }

    @PostMapping("/{profileName}/server-settings/{authToken}", consumes = ["application/json"], produces = ["application/json"])
    fun postServerSettings(
            model: Model,
            @PathVariable profileName: String,
            @PathVariable authToken: String,
            @RequestBody serverSettings: ServerSettings,
    ) = postSettings(profileName, authToken, serverSettings)

    @PostMapping("/{profileName}/map-settings/{authToken}", consumes = ["application/json"], produces = ["application/json"])
    fun postServerSettings(
            model: Model,
            @PathVariable profileName: String,
            @PathVariable authToken: String,
            @RequestBody serverSettings: MapSettings,
    ) = postSettings(profileName, authToken, serverSettings)

    @PostMapping("/{profileName}/map-gen-settings/{authToken}", consumes = ["application/json"], produces = ["application/json"])
    fun postServerSettings(
            model: Model,
            @PathVariable profileName: String,
            @PathVariable authToken: String,
            @RequestBody serverSettings: MapGenSettings,
    ) = postSettings(profileName, authToken, serverSettings)

    @GetMapping("/{profileName}/server-settings/{authToken}")
    fun getEditServerSettings(model: Model, @PathVariable profileName: String, @PathVariable authToken: String): String {
        logger.warn("getEditServerSettings - $profileName - $authToken")
        val profile = profileManager.getProfileByName(profileName) ?: throw HttpNotFoundException("Profile not found")
        if (!profile.checkAuth(authToken)) throw HttpForbiddenException("Bad or expired auth token")

        model["profile"] = profile
        model["authToken"] = authToken
        return "edit-server-settings"
    }

    @GetMapping("/{profileName}/map-settings/{authToken}")
    fun getEditMapSettings(model: Model, @PathVariable profileName: String, @PathVariable authToken: String): String {
        logger.warn("getEditMapSettings - $profileName - $authToken")
        val profile = profileManager.getProfileByName(profileName) ?: throw HttpNotFoundException("Profile not found")
        if (!profile.checkAuth(authToken)) throw HttpForbiddenException("Bad or expired auth token")

        model["profile"] = profile
        model["authToken"] = authToken
        return "edit-map-settings"
    }

    @GetMapping("/{profileName}/map-gen-settings/{authToken}")
    fun getEditMapGenSettings(model: Model, @PathVariable profileName: String, @PathVariable authToken: String): String {
        logger.warn("getEditMapGenSettings - $profileName - $authToken")
        val profile = profileManager.getProfileByName(profileName) ?: throw HttpNotFoundException("Profile not found")
        if (!profile.checkAuth(authToken)) throw HttpForbiddenException("Bad or expired auth token")

        model["profile"] = profile
        model["authToken"] = authToken
        return "edit-map-gen-settings"
    }

    @GetMapping("/{profileName}/revoke/{authToken}")
    fun getRevokeToken(model: Model, @PathVariable profileName: String, @PathVariable authToken: String): String {
        logger.warn("Revoke - $profileName - $authToken")
        val profile = profileManager.getProfileByName(profileName) ?: throw HttpNotFoundException("Profile not found")
        if (profile.token == authToken)
            profile.apply {
                token = null
            }.flushChanges()
        else
            throw HttpForbiddenException("Bad auth token")

        model["profile"] = profile
        return "revoked"
    }

}
