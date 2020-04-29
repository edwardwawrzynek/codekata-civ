# Rules

## Board
The game map is a square board of dimensions N x N. Each tile on the board can be one of five types:
* Ocean
* Grassland
* Hills
* Mountains
* Forests

Each tile may be occupied by at most one city, and any number of workers and/or armies.

## Players

Each player owns a number of cities, workers, and armies.

Each player has an offensive and defensive combat strength (initially 1).

## Turns

Players take turns in a linear fashion. Each turn consists of:

### Harvesting (no player interaction)
The tiles that cities are on are harvested. Any tile with a worker on it that is within manhattan distance 2 of a city is harvested.

Each tile yields:
Terrain|Food|Production|Trade
-|-|-|-
Ocean|1|0|2
Grassland|2|1|0
Hills|1|2|1
Forest|1|3|0
Mountains|0|1|0

A worker that shares a tile with a city harvests 2 trade.

Each worker and army eats 2 food. If a army or worker doesn't eat (no food left), it dies. Armies will starve before workers.

### Production
A player spends its harvested production. 

An army cost 10 production. 
A worker costs 10 production.
A city costs 30 production.

Armies and workers must be placed on a city controlled by the player. Each army starts with 100 hitpoints.

A city may be placed on any tile not covered by the fog of war (below).

### Technology
A player spends its harvested trade.

Defensive research (20 trade) -- increases defensive combat strength by 0.3;

Offensive research (20 trade) -- increases offensive combat strength by 0.3;

### Movement
A player may choose to move all workers and armies north, south, east, or west or not at all.

If a army or worker is moved onto a tile with an opponent army, or worker, combat begins.

An army trying to move onto a tile is an attacking army. An army on that tile is a defending army.

### Combat
Workers are automatically killed if they get involved in combat (attacking or defending).

If there are multiple armies attacking, they are considered part of the same attacking unit.

If there are multiple armies defending, they are considered part of the same defending unit.

Each combat unit deals damage to each army in the opposing unit equal to:
$$
\frac{myOffensiveStrength}{theirDefensiveStrength} * myNumArmies * \frac{1}{theirNumArmies} * 100
$$

Any army with 0 or less hitpoint dies at the end of combat.

If all defending armies are killed, the attacking units move into that tile. 

### Capturing a city
If an army successfully moves into an opponent's city, they gain ownership of it and any of the opponent's workers and armies within manhattan distance 2.

### Fog of War
All tiles are hidden from a player, unless they are within manhattan distance of 3 from a city, worker, or army.

## Winning
The game is won once one player is the only one that controls any cities.

TODO: after number of turns, score cities, workers, and armies
