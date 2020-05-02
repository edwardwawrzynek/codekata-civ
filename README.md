# Civilization Game

A game where players compete to expand their civilizations and conquer others.

See `civ-ruls.md` for the rules.

See `civ-api.md` for the api description.

## Running
Run `./gradlew bootRun` to start the server. It serves a frontend on `http://localhost:8080`.

The default player keys are `secret0`, `secret1`, `secret2`, `secret3`.

The default observation key is `observe0`.

## Writing A Player
See the `civ-api.md` for documentation on all the mentioned roots.

An api should:
1. Obtain a player key (for testing `secret0`, `secret1`, etc). For a competition these will be distributed beforehand
2. Call `/api/set_name` to give their AI a cool name (not required, but HIGHLY RECOMMENCED)
3. Call `/api/player_index` to get their player index (must calls return information with entries for each player index. You want to know which information is yours and which is your enemies)
4. Call `/api/info` and note the `playerRefreshRate` field. This is the time you should give between `/api/current_player` queries. This isn't technically required, but please do this. This allows the server to balance request volumes.
5. Query `/api/current_player` until it signals it is your turn. You should do this at the frequency obtained in step 4.
6. Reload the game state (`/api/cities`, `/api/armies`, `/api/workers`, `/api/resources`, `/api/players`, `/api/board`). All of these, even the board, should always be reloaded (fog of war may have changed).
7. Make your move via a combination of `/api/produce`, `/api/technology`, `/api/move_worker`, and `/api/move_army`
8. Tell the server your turn is doen by calling `/api/end_turn`
9. Goto step 5

# Questions, Issues?
Github issues and pull requests are welcome. If you find bugs, please report.

If you have questions, you can contact Edward or Henry.

# Credits
* Henry Westfall - initial game rules and frontend
* Edward Wawrzynek - game implementation and backend