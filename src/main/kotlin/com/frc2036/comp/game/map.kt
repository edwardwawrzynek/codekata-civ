package com.frc2036.comp.game

import kotlin.random.Random

// A game map (just its tile layout)
enum class TileType (val apiIndex: Int) {
  Ocean(0),
  Grassland(1),
  Hills(2),
  Forest(3),
  Mountains(4),
  Fogged(-1)
}

data class GameMap(val size: Int, val contents: Array<Array<TileType>>) {
  companion object {
    fun generateRandom(size: Int): GameMap {
      // TODO: use perlin noise
      // generate a map that is symmetric for each player
      val contents = Array(size){ Array(size){ TileType.Fogged } }
      // all the intersections between player squares must be the same, so each square is a mirror of a triangle
      // generate triangle
      val squareSize = size/2
      for(y in 0 until squareSize) {
        for(x in 0..y) {
          contents[x][y] = listOf(TileType.Ocean, TileType.Grassland, TileType.Hills, TileType.Forest, TileType.Mountains).random()
          // mirror triangle
          contents[y][x] = contents[x][y]
        }
      }
      //mirror square
      for(x in 0 until squareSize) {
        for(y in 0 until squareSize) {
          // mirror to the right
          contents[size - x - 1][y] = contents[x][y]
          // mirror down
          contents[x][size - y - 1] = contents[x][y]
          // mirror down and right
          contents[size - x - 1][size - y - 1] = contents[x][y]
        }
      }

      return GameMap(size, contents)
    }
  }
}