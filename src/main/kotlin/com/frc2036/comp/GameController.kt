package com.frc2036.comp

import com.frc2036.comp.game.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam

import kotlin.random.Random

// Controller to manage api calls to run the tournament
@RestController
@RequestMapping(value=["/api"])
class GameController {

    private val game: Game? = Game(GameMap.generateRandom(32), KeyManager(listOf("secret0"), listOf("observe0")))

    private fun makeErrorResponse(msg: String): String = "{\"error\": $msg }"

    /* get the current map. If a player, fogged tiles may be present */
    @RequestMapping(value=["/board"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getBoard(@RequestParam key: String): String {
        if(game == null) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")
        // TODO: fog of war if using a player key
        return "{\"error\": null, \"size\": ${game.map.size}, \"board\": ${game.map.contents.map { column -> column.map { it.apiIndex } }}}"
    }

}