package com.frc2036.comp.game

import java.lang.AssertionError

// how many hitpoints an army starts with
val ARMY_INIT_HITPOINTS = 100
// what is considered nearby a city (effectively for ownership of workers)
val CITY_NEARBY_DISTANCE = 2
// distance from unit or city for fogging
val FOG_DISTANCE = 2

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

class Game (val map: GameMap, val keys: KeyManager, var started: Boolean) {
    val players = mutableListOf<Player>()
    val keysToPlayers = mutableMapOf<String, Player>()
    val playerNames = mutableListOf<String>()

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
}