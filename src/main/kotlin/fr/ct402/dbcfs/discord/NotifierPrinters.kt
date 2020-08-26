package fr.ct402.dbcfs.discord

import fr.ct402.dbcfs.persist.model.GameVersion
import fr.ct402.dbcfs.persist.model.ModRelease
import fr.ct402.dbcfs.persist.model.Profile

const val listPoint = ":small_blue_diamond:"

infix fun Notifier.printProfiles(list: List<Profile>) {
    val str = list.joinToString("\n") {
        (if (it.allowExperimental) ":tools:" else ":shield:") +
                " ${it.name} - ${it.gameVersion.versionNumber}" +
                (if (it.gameVersion.localPath != null) " :floppy_disk:" else "")
    }
    print(str, force = true)
}

infix fun Notifier.printModReleases(list: List<ModRelease>) {
    val str = list.joinToString("\n") {
        "$listPoint ${it.mod.name} - ${it.version} - *${it.mod.summary}*"
    }
    print(str, force = true)
}

infix fun Notifier.printGameReleases(list: List<GameVersion>) {
    val str = list.joinToString("\n") {
        (if (!it.isStable) ":tools:" else ":shield:") + " ${it.versionNumber} "
    }
    print(str, force = true)
}
