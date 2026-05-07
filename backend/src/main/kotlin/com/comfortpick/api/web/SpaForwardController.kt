package com.comfortpick.api.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaForwardController {

    @GetMapping(
        "/",
        "/profiles/{summonerId}",
        "/profiles/{summonerId}/enemies/{enemyChampionId}",
        "/profiles/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}",
    )
    fun forwardAppRoutes(): String = "forward:/index.html"
}
