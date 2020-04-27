# API Description

This is the description of the api for the game. A description of rules is located elsewhere.

All programs interacting with the api has a key (a unique, secret string). Keys are distributed by an external mechanism before a game starts. There are two types of keys:

- Player keys -- a key assigned to a player program.

- Observer keys -- a key assigned to a program that is observing the state of the game (such as the default web viewer).

If you are writing a program to play in the game, you will receive a player key.

All routes respond with a valid JSON response. If an error occurred, the response will be of the form:
```
{"error": "whatever error message"}
```
A successful request will contain a null error field:
```
{"error": null, /*other fields...*/}
```

Each player is given an index -- they are either player 0, 1, 2, or 3

## Types:
`TileType` - An integer, corresponding to the type of tile. Options are:
* `0` - Ocean
* `1` - Grassland
* `2` - Hills
* `3` - Mountains
* `4` - Forest
* `-1` - Type hidden by fog of war

`ProduceType` - An integer, corresponding to the type of thing to produce. Options are:
* `0` - Army
* `1` - Worker
* `2` - City

## Routes
### Game State Routes
#### `GET /api/board - params(key: String)`

Get the current board state. If using a player key, some tiles may be obscured by the fog of war. If using an observer key, no tiles will be obscured.

Response is of the form:
```json
{"error": null,
 "size": Int, // length of one side of the sqquare board
 "board": [
   [TileType, TileType, ...],
   [TileType, TileType, ...],
   ...
 ] // board is indexed [x][y]
}
```

#### `GET /api/cities - params(key: String)`

Get a list of cities present on the board. If using a player key, cities obscured by the fog of war are not included.

Response is of the form:
```json
{"error": null,
 "cities": [
   [{"x": Int, "y": Int}, ...],  // player 0
   [{"x": Int, "y": Int}, ...],  // player 1
   [...],  // player 2
   [...]   // player 3
 ]
}
```

#### `GET /api/armies - params(key: String)`

Get a list of armies present on the board. If using a player key, armies obscured by the fog of war are not included.

Response is of the form:
```json
{"error": null,
 "armies": [
   [{"x": Int, "y": Int}, ...],  // player 0
   [{"x": Int, "y": Int}, ...],  // player 1
   [...],  // player 2
   [...]   // player 3
 ]
}
```

#### `GET /api/workers - params(key: String)`

Get a list of workers present on the board. If using a player key, workers obscured by the fog of war are not included.

Response is of the form:
```json
{"error": null,
 "workers": [
   [{"x": Int, "y": Int}, ...],  // player 0
   [{"x": Int, "y": Int}, ...],  // player 1
   [...],  // player 2
   [...]   // player 3
 ]
}
```

#### `GET /api/resources - params(key: String)`

Get the current number of resources (production, food, trade) held by each player.

Response is of the form:
```json
{"error": null,
 "resources": [
   {"production": Int, "food": Int, "trade": Int},  // player 0
   {"production": Int, "food": Int, "trade": Int},  // player 1
   {"production": Int, "food": Int, "trade": Int},  // player 2
   {"production": Int, "food": Int, "trade": Int}   // player 3
 ]}
```

#### `GET /api/resources - params(key: String)`

Get the current number of resources (production, food, trade) held by each player.

Response is of the form:
```json
{"error": null,
 "resources": [
   {"production": Int, "food": Int, "trade": Int},  // player 0
   {"production": Int, "food": Int, "trade": Int},  // player 1
   {"production": Int, "food": Int, "trade": Int},  // player 2
   {"production": Int, "food": Int, "trade": Int}   // player 3
 ]}
```

#### `GET /api/current_turn - params(key: String)`

Get which player's turn it is to go.

Response is of the form:
```json
{"error": null, "turn": Int /* 0 for player 0, 1 for player 1, etc. -1 if game hasn't started */}
```

### Game movement routes
Note: all of these require a player's key, and can only be called while it is your turn.

The sequence of taking a turn:
1. Wait until `/api/current_turn` indicates it is your turn
2. Make any number of `/api/produce`, `/api/move_worker`, and `/api/move_army` calls..
3. Call `/api/end_turn` to end your turn. You must call this.

#### `POST /api/produce - params(key: String, type: ProductionType, x: Int, y: Int)`

Produce the given thing (army, worker, or city) at the given location.

If producing a army or worker, the tile referred to by `x` and `y` must be a city owned by you.

If producing a city, the tile referred to by `x` any `y` must be not obscured by the fog of war.

If you don't have enough production units, an error will be returned:
```json
{"error": "not enough production/invalid location"}
```
Otherwise:
```json
{"error":null}
```

#### `POST /api/move_worker - params(key: String, srcX: Int, srcY: Int, dstX: Int, dstY: Int)`

Move a worker from the (`srcX`,`srcY`) tile to the (`dstX`, `dstY`) tile. The dst tile must be adjacent to the src tile.

If there are multiple workers on a tile, you must call this multiple times for each.

A worker can only be moved once per turn.

#### `POST /api/move_army - params(key: String, srcX: Int, srcY: Int, dstX: Int, dstY: Int)`

Move an army from the (`srcX`,`srcY`) tile to the (`dstX`, `dstY`) tile. The dst tile must be adjacent to the src tile.

If there are multiple armies on a tile, you must call this multiple times for each.

An army can only be moved once per turn.

If you move an army to a tile with an opponent on it, combat will occur.

#### `POST /api/end_turn - params(key: String)`

End your turn. This must be called, or all the other players will be waiting for you.