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

    private val game = Game(GameMap.generateRandom(32), KeyManager(listOf("secret0", "secret1", "secret2", "secret3"), listOf("observe0"), listOf("admin0")), true)

    private fun makeErrorResponse(msg: String): String = "{\"error\": \"$msg\" }"

    /* get the current map. If a player, fogged tiles may be present */
    @RequestMapping(value=["/board"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getBoard(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")
        // apply fog of war
        val fogMap = if(game.keys.isPlayer(key)) game.keysToPlayers[key]!!.calculateUnfoggedMap(game.map) else game.map.observeFogMap()
        return "{\"error\": null, \"size\": ${game.map.size}, \"board\": ${game.map.contents.mapIndexed { 
            x, column -> column.mapIndexed { 
                y, item -> if(fogMap[x][y]) item.apiIndex else TileType.Fogged.apiIndex 
            } 
        }}}"
    }

    /* get the current cities. If player, fogged results will not be included */
    @RequestMapping(value=["/cities"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getCities(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        // apply fog of war
        val fogMap = if(game.keys.isPlayer(key)) game.keysToPlayers[key]!!.calculateUnfoggedMap(game.map) else game.map.observeFogMap()
        // calculate visible cities
        val cities = game.players.map { player ->
            player.cities.filter { city -> fogMap[city.position.first][city.position.second] }.map {
                city -> "{\"x\": ${city.position.first}, \"y\": ${city.position.second}}"
            }
        }

        return "{\"error\": null, \"cities\": $cities}"
    }

}