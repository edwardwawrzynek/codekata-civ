package com.frc2036.comp.game

class KeyManager(val playerKeys: List<String>, val observeKeys: List<String>) {
    fun isValidKey(key: String) = key in playerKeys || key in observeKeys
    fun isPlayer(key: String) = key in playerKeys
    fun isObserver(key: String) = key in observeKeys
}

class Game (val map: GameMap, val keys: KeyManager) {

}