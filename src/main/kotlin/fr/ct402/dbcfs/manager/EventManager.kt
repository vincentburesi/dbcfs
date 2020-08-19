package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
class EventManager (
        val profileManager: ProfileManager,
        val processManager: ProcessManager,
        val downloadApiService: DownloadApiService
) {
    val logger = getLogger()

    fun syncGameVersion() =
            downloadApiService.syncGameVersions()

    fun createProfile(name: String, version: String? = null, allowExperimental: Boolean = false) =
            profileManager.createProfile(name, version, allowExperimental)

    fun swapProfile(name: String) =
            profileManager.swapProfile(name)

    fun uploadConfigFile(unvalidatedJson: String, targetConfig: String) {} // TODO

    fun prepareProfile() =
            profileManager.downloadGame()

    fun buildMap(): Boolean {
        return processManager.genMap(profileManager.currentProfile ?: return false)
    }

    fun startProfile(): Boolean {
        return processManager.start(profileManager.currentProfile ?: return false)
    }
}
