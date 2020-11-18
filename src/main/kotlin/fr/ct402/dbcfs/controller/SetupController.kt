package fr.ct402.dbcfs.controller

import fr.ct402.dbcfs.refactor.commons.getLogger
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Controller
@RequestMapping("/setup", method = [RequestMethod.GET])
class SetupController {
//    constructor() {
//        print("Started SetupController")
//    }

    val logger = getLogger()

    @GetMapping("/")
    fun getHome(model: Model): String { //FIXME Major feature : Add setup through controller
        logger.warn("SetupController::getHome()")
        model["name"] = "Vincent"
        model["age"] = 28
        return "setup/home"
    }
}
