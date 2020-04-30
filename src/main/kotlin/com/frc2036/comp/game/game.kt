package com.frc2036.comp.game

import java.lang.AssertionError

// how many hitpoints an army starts with
const val ARMY_INIT_HITPOINTS = 100
// what is considered nearby a city (effectively for ownership of workers)
const val CITY_NEARBY_DISTANCE = 2
// distance from unit or city for fogging
const val FOG_DISTANCE = 3

// food each worker eats
const val WORKER_FOOD = 1
// food each army eats
const val ARMY_FOOD = 1

// production cost of army, worker, city
const val WORKER_COST = 10
const val ARMY_COST = 10
const val CITY_COST = 30

// trade cost of technology (offesive or defensive strength improvements)
const val TECHNOLOGY_COST = 20
// how much technology increases offensive or defensive strength
const val TECHNOLOGY_IMPROVEMENT = 0.3

/**
 * player keys correspond to a player in the game. They give access to game manipulation routes, but limit some observation routes with the fog of war
 * observe keys allow unfogged access to observe routes, but no access to game manipulation routes
 * admin keys are the same as observe keys, but allow access to some game management routes
 */
class KeyManager(val playerKeys: List<String>, val observeKeys: List<String>, val adminKeys: List<String>) {
    fun isValidKey(key: String) = key in playerKeys || key in observeKeys || key in adminKeys
    fun isPlayer(key: String) = key in playerKeys
    fun isObserver(key: String) = key in observeKeys || key in adminKeys
    fun isAdmin(key: String) = key in adminKeys
}

class Game (val map: GameMap, val keys: KeyManager) {
    val players = mutableListOf<Player>()
    val keysToPlayers = mutableMapOf<String, Player>()
    val playerNames = mutableListOf<String>()

    var started = false
    var currentPlayerIndex = 0

    init {
        for(i in keys.playerKeys.indices) {
            val k = keys.playerKeys[i]
            val p = Player()
            players.add(p)
            keysToPlayers[k] = p
            playerNames.add("Unnamed AI $i")

            //initialize cities
            when(i) {
                0 -> p.cities.add(City(Pair(0, 0)))
                1 -> p.cities.add(City(Pair(map.size - 1, 0)))
                2 -> p.cities.add(City(Pair(0, map.size - 1)))
                3 -> p.cities.add(City(Pair(map.size - 1, map.size - 1)))
                else -> throw AssertionError("Only four players are allowed")
            }
        }
    }

    fun nextTurn() {
        currentPlayerIndex++
        currentPlayerIndex %= 4
        // if player has no cities, skip them
        if(players[currentPlayerIndex].cities.isEmpty()) nextTurn()

        doPreTurn()
    }

    // run the pre-turn sequence for the player (harvesting, eating)
    private fun doPreTurn() {
        players[currentPlayerIndex].clearMoved()
        players[currentPlayerIndex].doHarvest(map)
        players[currentPlayerIndex].doEat()
    }

    // start match
    fun start() {
        started = true
        doPreTurn()
    }

    /* do combat
     attackUnit is the army attacking
     defendPosition is the square being attacked
    */
    fun doCombat(currentPlayer: Player, attackUnit: Army, defendPosition: Pair<Int, Int>) {
        // kill all workers on defendPosition
        for(p in players) {
            if(p == currentPlayer) continue
            p.workers.removeIf {w -> w.position == defendPosition}
        }
        // find defending armies
        val defending = mutableListOf<Army>()
        var defendingPlayer: Player? = null
        for(p in players) {
            for(a in p.armies) {
                if(a.position == defendPosition) {
                    defending.add(a)
                    if(defendingPlayer != null && defendingPlayer != p) throw AssertionError("Only armies of one player can be in a square")
                    defendingPlayer = p
                }
            }
        }
        // if no defenders, move army into target
        if(defendingPlayer == null) {
            attackUnit.doMove(defendPosition)
            return
        }
        // terrain multipliers
        val attackTerrain = map.getCombatMultiplier(attackUnit.position)
        val defendTerrain = map.getCombatMultiplier(defendPosition)

        // damage dealt by attacker
        val attackDamage = ((currentPlayer.offensiveStrength)/(defendingPlayer.defensiveStrength)) *
                (1.0/defending.size.toDouble()) *
                (attackTerrain/defendTerrain) *
                100

        val defendDamage =  ((defendingPlayer.defensiveStrength)/(currentPlayer.offensiveStrength)) *
                defending.size.toDouble() *
                (defendTerrain/attackTerrain) *
                100

        attackUnit.takeDamage(defendDamage.toInt())

        var anyAlive = false
        for(d in defending) {
            d.takeDamage(attackDamage.toInt())
            if(d.isDead()) defendingPlayer.armies.remove(d)
            else anyAlive = true
        }

        if(attackUnit.isDead()) {
            currentPlayer.armies.remove(attackUnit)
        } else {
            /* if some defenders survived, stay in psoition */
            if(anyAlive) attackUnit.moved = true
            else attackUnit.doMove(defendPosition)
        }
    }
}