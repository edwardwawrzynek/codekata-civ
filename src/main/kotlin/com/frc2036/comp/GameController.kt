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
    init {
        game.players[0].workers.add(Worker(Pair(1,1)))
        game.players[0].workers.add(Worker(Pair(3,1)))
        game.players[1].workers.add(Worker(Pair(15,14)))
        game.players[2].armies.add(Army(Pair(25, 25)))
        game.players[3].cities.add(City(Pair(24, 24)))
    }

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

    // get the fog map for the key (if non player, a blank key)
    fun getFogMap(key: String) = if(game.keys.isPlayer(key)) game.keysToPlayers[key]!!.calculateUnfoggedMap(game.map) else game.map.observeFogMap()

    /* get the current cities. If player, fogged results will not be included */
    @RequestMapping(value=["/cities"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getCities(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        // apply fog of war
        val fogMap = getFogMap(key)
        // calculate visible cities
        val cities = game.players.map { player ->
            player.cities.filter { city -> fogMap[city.position.first][city.position.second] }.map {
                city -> "{\"x\": ${city.position.first}, \"y\": ${city.position.second}}"
            }
        }

        return "{\"error\": null, \"cities\": $cities}"
    }

    /* get the current armies. if using a player key, some entries may be fogged */
    @RequestMapping(value=["/armies"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getArmies(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        // apply fog of war
        val fogMap = getFogMap(key)
        // calculate visible armies
        val armies = game.players.map { player ->
            player.armies.filter { army -> fogMap[army.position.first][army.position.second] }.map {
                army -> "{\"x\": ${army.position.first}, \"y\": ${army.position.second}, \"hitpoints\":${army.hitpoints}}"
            }
        }

        return "{\"error\": null, \"armies\": $armies}"
    }

    /* get the current workers, if using a player key, some entries may be fogged */
    @RequestMapping(value=["/workers"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getWorkers(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        // apply fog of war
        val fogMap = getFogMap(key)
        // calculate visible armies
        val workers = game.players.map { player ->
            player.workers.filter { worker -> fogMap[worker.position.first][worker.position.second] }.map {
                worker -> "{\"x\": ${worker.position.first}, \"y\": ${worker.position.second}}"
            }
        }

        return "{\"error\": null, \"workers\": $workers}"
    }

    // get which player index maps to the key
    @RequestMapping(value=["/player_index"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getPlayerIndex(@RequestParam key: String): String {
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("key is not a player key")
        val player = game.players.indexOf(game.keysToPlayers[key])

        return "{\"error\": null, \"player\": $player}"
    }

    /* get the names and offensive and defensive strengths of each player */
    @RequestMapping(value=["/players"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getPlayers(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        return "{\"error\": null, \"players\": ${game.players.mapIndexed { i, player -> "{\"name\": \"${game.playerNames[i]}\", \"offense\": ${player.offensiveStrength}, \"defense\": ${player.defensiveStrength}}"}}}"
    }

    /* get the number of resources (trade, production, food) each player has */
    @RequestMapping(value=["/resources"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getResources(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        return "{\"error\": null, \"resources\": ${game.players.map { player -> "{\"production\": ${player.resources[ResourceType.Production]}, \"food\": ${player.resources[ResourceType.Food]}, \"trade\": ${player.resources[ResourceType.Trade]}}"}}}"
    }

    /* set the name of a player */
    @RequestMapping(value=["/set_name"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun setName(@RequestParam key: String, @RequestParam name: String): String {
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("key is not a player key")
        val player = game.players.indexOf(game.keysToPlayers[key])

        game.playerNames[player] = name

        return "{\"error\": null}"
    }

    /* get the current player to go */
    @RequestMapping(value=["/current_player"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getCurrentPlayer(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isValidKey(key)) return makeErrorResponse("invalid key")

        return "{\"error\": null, \"turn\": ${game.currentPlayerIndex}}"
    }

    /* end current turn */
    @RequestMapping(value=["/end_trun"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun endTurn(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("not a player key")

        // make sure key is current player
        if(game.players.indexOf(game.keysToPlayers[key]) != game.currentPlayerIndex) return makeErrorResponse("not current player")
        game.nextTurn()

        return "{\"error\": null}"
    }
}