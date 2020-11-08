//package fr.ct402.dbcfs.refactor.discord
//
//import fr.ct402.dbcfs.refactor.commons.fileSizeAsString
//import fr.ct402.dbcfs.refactor.commons.tokenValidityInMinutes
//import fr.ct402.dbcfs.persist.model.GameVersion
//import fr.ct402.dbcfs.persist.model.ModRelease
//import fr.ct402.dbcfs.persist.model.Profile
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import java.io.File
//
//const val listPoint = ":small_blue_diamond:"
//const val discordMessageLimit = 2000
//
//fun Notifier.printStrings(list: List<String>, all: Boolean = false) {
//    var messages = arrayListOf<String>()
//    var acc = ""
//    fun chunk(str: String) {
//        if (str.length > discordMessageLimit) {
//            messages.add(str.take(discordMessageLimit))
//            chunk(str.drop(discordMessageLimit))
//        } else
//            messages.add(str)
//    }
//
//    list.mapIndexed { index, s ->
//        if (index > 0) "\n" + s else s
//    }.forEach {
//        if (acc.length + it.length < discordMessageLimit)
//            acc += it
//        else {
//            messages.add(acc)
//            if (it.length < discordMessageLimit)
//                acc = it
//            else {
//                chunk(it)
//                acc = ""
//            }
//        }
//    }
//    if (acc.isEmpty()) {
//        print("*Empty*", force = true)
//        return
//    } else
//        messages.add(acc)
//
//    if (!all)
//        print(messages.first(), force = true)
//    else
//        GlobalScope.launch {
//            messages.forEach { event.channel.sendMessage(it).complete() }
//        }
//}
//
////infix fun Notifier.printProfiles(list: List<Profile>) {
////    val strings = list.map {
////        (if (it.allowExperimental) ":tools:" else ":shield:") +
////                " **${it.name}** - ${it.gameVersion.versionNumber}" +
////                (if (it.gameVersion.localPath != null) " :floppy_disk:" else "")
////    }
////    printStrings(strings)
////}
//
//fun Notifier.printModReleases(list: List<ModRelease>, all: Boolean = false) {
//    val strings = list.map {
//        "$listPoint **${it.mod.name}** - ${it.version} - *${it.mod.summary}*"
//    }.reversed()
//    printStrings(strings, all)
//}
//
//infix fun Notifier.printGameReleases(list: List<GameVersion>) {
//    val strings = list.map {
//        (if (!it.isStable) ":tools:" else ":shield:") + " **${it.versionNumber}** "
//    }
//    printStrings(strings)
//}
//
//fun Notifier.printProfileFiles(list: List<File>, profile: Profile, domain: String) {
//    val strings = list.map { "$listPoint **${it.name}** *${fileSizeAsString(it.length())}* $domain/file/${profile.name}/${profile.token}/${it.name}" }
//    printStrings(strings + "*Links will be valid for the next $tokenValidityInMinutes minutes*", all = true)
//}
//
//fun Notifier.success(fileName: String, profile: Profile, domain: String) =
//        success("**$fileName** $domain/file/${profile.name}/${profile.token}/$fileName\n"
//                + "*Link will be valid for the next $tokenValidityInMinutes minutes*")
