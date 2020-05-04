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

    private var game = Game(GameMap.generateRandom(32), KeyManager(listOf(System.getenv()["CIV_PLAYER0_KEY"] ?: "secret0", System.getenv()["CIV_PLAYER1_KEY"] ?: "secret1", System.getenv()["CIV_PLAYER2_KEY"] ?: "secret2", System.getenv()["CIV_PLAYER3_KEY"] ?: "secret3"), listOf(System.getenv()["CIV_OBSERVE_KEY"] ?: "observe0"), listOf(System.getenv()["CIV_ADMIN_KEY"] ?: "admin0")))
    init {
        if(System.getenv()["CIV_ENABLE_ADMIN_CONTROL"] == null) {
            game.start()
        }
    }

    // check if position exists on board
    private fun checkPosition(position: Pair<Int, Int>): Boolean = position.first >= 0 && position.first < game.map.size && position.second >= 0 && position.second < game.map.size

    private fun makeErrorResponse(msg: String): String = "{\"error\": \"$msg\" }"

    /* get the current map. If a player, fogged tiles may be present */
    @RequestMapping(value=["/board"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getBoard(@RequestParam key: String): String {
        //if(!game.started) return makeErrorResponse("no active game")
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
    @RequestMapping(value=["/end_turn"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun endTurn(@RequestParam key: String): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("not a player key")

        // make sure key is current player
        if(game.players.indexOf(game.keysToPlayers[key]) != game.currentPlayerIndex) return makeErrorResponse("not current player")
        game.nextTurn()

        return "{\"error\": null}"
    }

    /* produce a unit */
    @RequestMapping(value=["/produce"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun produce(@RequestParam key: String, type: Int, x: Int, y: Int): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("not a player key")

        if(!checkPosition(Pair(x, y))) return makeErrorResponse("position is not on board")

        // make sure key is current player
        if(game.players.indexOf(game.keysToPlayers[key]) != game.currentPlayerIndex) return makeErrorResponse("not current player")

        val player = game.keysToPlayers[key]!!

        val cost = when(type) {
            0 -> ARMY_COST
            1 -> WORKER_COST
            2 -> CITY_COST
            else -> return makeErrorResponse("invalid production type")
        }
        if(player.resources[ResourceType.Production]!! < cost) {
            return makeErrorResponse("player doesn't have enough production")
        }

        if(type == 0 || type == 1) {
            /* position must be on a city */
            var isOnCity = false
            for(c in player.cities) {
                if(c.position.first == x && c.position.second == y) isOnCity = true
            }
            if(!isOnCity) return makeErrorResponse("worker or army must be created on a city")
        } else if(type == 2) {
            val fog = getFogMap(key)
            if(!fog[x][y]) return makeErrorResponse("city cannot be placed on a tile covered by fog of war")
            for(p in game.players) {
                for(c in p.cities) {
                    if(c.position.first == x && c.position.second == y) return makeErrorResponse("cannot place city on an existing city")
                }
            }
        }

        player.resources[ResourceType.Production] = player.resources[ResourceType.Production]!! - cost
        when(type) {
            0 -> player.armies.add(Army(Pair(x, y)))
            1 -> player.workers.add(Worker(Pair(x, y)))
            2 -> player.cities.add(City(Pair(x, y)))
        }

        return "{\"error\": null}"
    }

    @RequestMapping(value=["/technology"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun technology(@RequestParam key: String, @RequestParam type: Int): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("not a player key")
        // make sure key is current player
        if(game.players.indexOf(game.keysToPlayers[key]) != game.currentPlayerIndex) return makeErrorResponse("not current player")

        if(type != 0 && type != 1) return makeErrorResponse("invalid technology type")

        val player = game.keysToPlayers[key]!!
        if(player.resources[ResourceType.Trade]!! < TECHNOLOGY_COST) return makeErrorResponse("not enough trade")

        player.resources[ResourceType.Trade] = player.resources[ResourceType.Trade]!! - TECHNOLOGY_COST
        when(type) {
            0 -> player.offensiveStrength += TECHNOLOGY_IMPROVEMENT
            1 -> player.defensiveStrength += TECHNOLOGY_IMPROVEMENT
        }

        return "{\"error\": null}"
    }

    @RequestMapping(value=["/move_worker"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun moveWorker(@RequestParam key: String, @RequestParam srcX: Int, @RequestParam srcY: Int, @RequestParam dstX: Int, @RequestParam dstY: Int): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("not a player key")
        // make sure key is current player
        if(game.players.indexOf(game.keysToPlayers[key]) != game.currentPlayerIndex) return makeErrorResponse("not current player")

        if(!checkPosition(Pair(srcX, srcY))) return makeErrorResponse("src position is not on board")
        if(!checkPosition(Pair(dstX, dstY))) return makeErrorResponse("dst position is not on board")

        val player = game.keysToPlayers[key]!!
        // find worker
        for(w in player.workers) {
            if(w.position.first == srcX && w.position.second == srcY) {
                if(!w.isValidMove(Pair(dstX, dstY))) return makeErrorResponse("invalid move (too far)")
                if(w.moved) return makeErrorResponse("worker already moved this turn")

                // if there are enemy workers on dst, kill them, and then this worker
                // if there are enemy armies on dst, kill this worker
                var workerDied = false
                for(p in game.players) {
                    if(p == player) continue
                    val killedWorkers = mutableListOf<Worker>()
                    for(oW in p.workers) {
                        if(oW.position.first == dstX && oW.position.second == dstY) {
                            workerDied = true
                            killedWorkers.add(oW)
                        }
                    }
                    for(kW in killedWorkers) p.workers.remove(kW)
                    for(oA in p.armies) {
                        if(oA.position.first == dstX && oA.position.second == dstY) {
                            workerDied = true
                        }
                    }
                }

                if(workerDied) player.workers.remove(w)
                else w.doMove(Pair(dstX, dstY))

                return "{\"error\": null}"
            }
        }

        return makeErrorResponse("no worker at specified source location")
    }

    @RequestMapping(value=["/move_army"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun moveArmy(@RequestParam key: String, @RequestParam srcX: Int, @RequestParam srcY: Int, @RequestParam dstX: Int, @RequestParam dstY: Int): String {
        if(!game.started) return makeErrorResponse("no active game")
        if(!game.keys.isPlayer(key)) return makeErrorResponse("not a player key")
        // make sure key is current player
        if(game.players.indexOf(game.keysToPlayers[key]) != game.currentPlayerIndex) return makeErrorResponse("not current player")

        if(!checkPosition(Pair(srcX, srcY))) return makeErrorResponse("src position is not on board")
        if(!checkPosition(Pair(dstX, dstY))) return makeErrorResponse("dst position is not on board")

        val player = game.keysToPlayers[key]!!
        // find worker
        for(a in player.armies) {
            if(a.position.first == srcX && a.position.second == srcY) {
                if(!a.isValidMove(Pair(dstX, dstY))) return makeErrorResponse("invalid move (too far)")
                if(a.moved) return makeErrorResponse("army already moved this turn")

                game.doCombat(player, a, Pair(dstX, dstY))

                return "{\"error\": null}"
            }
        }

        return makeErrorResponse("no army at specified source location")
    }

    @RequestMapping(value=["/info"], method=[RequestMethod.GET], produces=["application/json"])
    @Synchronized
    fun getInfo(@RequestParam key: String): String {
        if(!game.keys.isValidKey(key)) return makeErrorResponse("not a valid key")

        return "{\"error\": null, \"version\": \"$VERSION\", \"observeRefreshRate\": 500, \"playerRefreshRate\": 500}"
    }

    @RequestMapping(value=["/admin/stop"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun stopGame(@RequestParam key: String): String {
        if(!game.keys.isAdmin(key)) return makeErrorResponse("not admin key")

        game.started = false

        return "{\"error\": null}"
    }

    @RequestMapping(value=["/admin/start"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun startGame(@RequestParam key: String): String {
        if(!game.keys.isAdmin(key)) return makeErrorResponse("not admin key")

        game.start()

        return "{\"error\": null}"
    }

    @RequestMapping(value=["/admin/new_map"], method=[RequestMethod.POST], produces=["application/json"])
    @Synchronized
    fun newBoard(@RequestParam key: String): String {
        if(!game.keys.isAdmin(key)) return makeErrorResponse("not admin key")

        val newMap = GameMap.generateRandom(32)
        for(x in 0 until 32) {
            for(y in 0 until 32) {
                game.map.contents[x][y] = newMap.contents[x][y]
            }
        }

        return "{\"error\": null}"
    }

}