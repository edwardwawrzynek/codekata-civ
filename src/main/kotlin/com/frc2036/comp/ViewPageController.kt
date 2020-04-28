package com.frc2036.comp

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

// Controller to show the page that displays graphically the state of the competition
@Controller
class ViewPageController {
    @RequestMapping("/")
    fun main() = "index.html"
}