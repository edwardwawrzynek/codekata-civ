package com.frc2036.comp.game

// A game map (just its tile layout)
enum class TileType (val apiIndex: Int) {
  Ocean(0),
  Grassland(1),
  Hills(2),
  Forest(3)
  Mountains(4),
  Fogged(-1)
}

data class Map(val size: Int, val contents: Array<Array<TileType>>) {
  companion object {
    fun generateRandom(val size: Int): Map {
      // generate a map that is symetric for each player
      val contents = Array(size){ Array(size){ TileType.Ocean } }
    }
  }
}