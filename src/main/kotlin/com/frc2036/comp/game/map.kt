package com.frc2036.comp.game

import kotlin.math.abs
import fastnoise.FastNoise
import kotlin.random.Random

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
      val noise = FastNoise()
      noise.SetSeed(Random.nextInt())
      noise.SetNoiseType(FastNoise.NoiseType.Perlin)
      val squareSize = size/2
      for(y in 0 until squareSize) {
        for(x in 0..y) {
          //val tile = listOf(TileType.Ocean, TileType.Grassland, TileType.Hills, TileType.Forest, TileType.Mountains).random()
          val res = noise.GetNoise((x.toFloat()*16.0).toFloat(), (y.toFloat()*16.0).toFloat())+0.5
          val tile = when {
            res < 0.2 -> TileType.Ocean
            res >= 0.2 && res < 0.4 -> TileType.Grassland
            res >= 0.4 && res < 0.6 -> TileType.Hills
            res >= 0.6 && res < 0.8 -> TileType.Forest
            res >= 0.8 -> TileType.Mountains
            else -> TileType.Ocean
          }
          contents[x][y] = tile
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

  fun getHarvestAmounts(position: Pair<Int, Int>): Map<ResourceType, Int> {
    val type = contents[position.first][position.second]

    /* harvest table */
    return when(type) {
      TileType.Ocean -> mapOf(ResourceType.Food to 1, ResourceType.Production to 0, ResourceType.Trade to 2)
      TileType.Grassland -> mapOf(ResourceType.Food to 2, ResourceType.Production to 1, ResourceType.Trade to 0)
      TileType.Hills -> mapOf(ResourceType.Food to 1, ResourceType.Production to 2, ResourceType.Trade to 1)
      TileType.Forest -> mapOf(ResourceType.Food to 1, ResourceType.Production to 3, ResourceType.Trade to 0)
      TileType.Mountains -> mapOf(ResourceType.Food to 0, ResourceType.Production to 1, ResourceType.Trade to 0)
      else -> throw AssertionError("map should not contain fogged tiles")
    }
  }
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

  val resources = mutableMapOf(ResourceType.Food to 0, ResourceType.Production to 0, ResourceType.Trade to 0)

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

  /* add single harvest to total resources */
  fun addHarvest(harvest: Map<ResourceType, Int>) {
    resources[ResourceType.Food] = resources[ResourceType.Food]!! + harvest[ResourceType.Food]!!
    resources[ResourceType.Production] = resources[ResourceType.Production]!! + harvest[ResourceType.Production]!!
    resources[ResourceType.Trade] = resources[ResourceType.Trade]!! + harvest[ResourceType.Trade]!!
  }

  /* do a harvest of all workers and cities */
  fun doHarvest(map: GameMap) {
    val harvested = Array(map.size) { Array(map.size) { false } }
    val hasCity = Array(map.size) { Array(map.size) { false } }

    for(c in cities) {
      if(!harvested[c.position.first][c.position.second]) addHarvest(map.getHarvestAmounts(c.position))
      harvested[c.position.first][c.position.second] = true
      hasCity[c.position.first][c.position.second] = true
    }

    for(w in workers) {
      if(!harvested[w.position.first][w.position.second]) addHarvest(map.getHarvestAmounts(w.position))
      harvested[w.position.first][w.position.second] = true
      if(hasCity[w.position.first][w.position.second]) resources[ResourceType.Trade] = resources[ResourceType.Trade]!! + 2
    }
  }

  /* make units eat and potentially starve */
  fun doEat() {
    // feed workers, then armies
    val workersRandomized = workers.shuffled()
    for(w in workersRandomized) {
      resources[ResourceType.Food] = resources[ResourceType.Food]!! - WORKER_FOOD
      if(resources[ResourceType.Food]!! < 0) {
        //TODO: display some notification that worker died
        workers.remove(w)
      }
    }
    val armiesRandomized = armies.shuffled()
    for(a in armiesRandomized) {
      resources[ResourceType.Food] = resources[ResourceType.Food]!! - ARMY_FOOD
      if(resources[ResourceType.Food]!! < 0) {
        //TODO: display some notification that army died
        armies.remove(a)
      }
    }
    if(resources[ResourceType.Food]!! < 0) resources[ResourceType.Food] = 0
  }

}