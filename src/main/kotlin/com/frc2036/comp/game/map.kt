package com.frc2036.comp.game

import kotlin.math.abs

/**
 * Types of tiles
 */
enum class TileType (val apiIndex: Int) {
  Ocean(0),
  Grassland(1),
  Hills(2),
  Forest(3),
  Mountains(4),
  Fogged(-1)
}

/*
 * Types of harvested materials
 */
enum class ResourceType {
  Production,
  Trade,
  Food
}

/**
 * A map (just a tile configuration)
 */
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

  fun observeFogMap(): Array<Array<Boolean>> = Array(size) { Array(size) { true } }
}

/**
 * Something owned by a player that has a place on the board (city, worker, army)
 */
open class BoardObject(var position: Pair<Int, Int>) {
  // calculate the manhattan distance to another object on the board
  fun distanceTo(other: BoardObject): Int {
    return abs(position.first - other.position.first) + abs(position.second - other.position.second)
  }
  fun distanceTo(other: Pair<Int, Int>): Int {
    return abs(position.first - other.first) + abs(position.second - other.second)
  }
}

// a city instance
class City(position: Pair<Int, Int>): BoardObject(position)

// A movable BoardObject (worker or army)
abstract class BoardUnit(position: Pair<Int, Int>): BoardObject(position) {
  // if the unit has been moved this turn
  var moved = false
  // if a position is a valid move for this unit
  fun isValidMove(newPos: Pair<Int, Int>): Boolean {
    return distanceTo(newPos) <= 1
  }
  // make a move
  fun doMove(newPos: Pair<Int, Int>) {
    if (!isValidMove(newPos)) return
    moved = true
    position = position.copy(first = newPos.first, second = newPos.second)
  }
  // damage operations
  abstract fun takeDamage(damage: Int)
  abstract fun isDead(): Boolean
}

// an army unit
class Army(position: Pair<Int, Int>): BoardUnit(position) {
  var hitpoints = ARMY_INIT_HITPOINTS
  override fun takeDamage(damage: Int) {
    hitpoints -= damage
  }
  override fun isDead() = hitpoints <= 0
}

// a worker unit
class Worker(position: Pair<Int, Int>): BoardUnit(position) {
  private var killed = false
  override fun takeDamage(damage: Int) {
    killed = true
  }
  override fun isDead() = killed
}

/**
 * A player and is associated state
 **/
class Player() {
  var offensiveStrength = 1.0
  var defensiveStrength = 1.0

  val cities = mutableListOf<City>()
  val workers = mutableListOf<Worker>()
  val armies = mutableListOf<Army>()

  val resources = mapOf(ResourceType.Food to 0, ResourceType.Production to 0, ResourceType.Trade to 0)

  // find all units nearby city
  fun findUnitsNearCity(city: City): List<BoardUnit> {
    val res = mutableListOf<BoardUnit>()
    for(a in armies) {
      if(city.distanceTo(a) <= CITY_NEARBY_DISTANCE) res.add(a)
    }
    for(w in workers) {
      if(city.distanceTo(w) <= CITY_NEARBY_DISTANCE) res.add(w)
    }

    return res
  }

  // calculate the fogged map of the player at this point
  // returns an array or array of booleans (same as game map, indexed [x][y]) or whether the cell is unfogged or not
  fun calculateUnfoggedMap(map: GameMap): Array<Array<Boolean>> {
    val res = Array(map.size) { Array(map.size) { false } }
    for(x in 0 until map.size) {
      for(y in 0 until map.size) {
        if(res[x][y]) continue
        for(u in cities) {
          if(u.distanceTo(Pair(x,y)) <= FOG_DISTANCE) {
            res[x][y] = true
            break
          }
        }
        if(res[x][y]) continue
        for(u in armies) {
          if(u.distanceTo(Pair(x,y)) <= FOG_DISTANCE) {
            res[x][y] = true
            break
          }
        }
        if(res[x][y]) continue
        for(u in workers) {
          if(u.distanceTo(Pair(x,y)) <= FOG_DISTANCE) {
            res[x][y] = true
            break
          }
        }
      }
    }

    return res
  }

}