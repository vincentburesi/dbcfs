package fr.ct402.dbcfs.controller

import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.commons.parseDateTime
import fr.ct402.dbcfs.manager.ProfileManager
import fr.ct402.dbcfs.persist.model.Profile
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Controller
@RequestMapping("/edit")
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
}
