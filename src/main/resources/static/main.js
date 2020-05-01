var canvas;
const apiKey = "observe0";


let printed = false;
let boardDrawn = false;

// display constants
const TILE_SIZE = 32;
let SHOWN_TILES = 32;

class Camera {
    constructor() {
        this.x = 0
        this.y = 0
        this.shift_amount = 1;
        this.board_size;
    }

    update(evt) {
        let yBefore = this.y;
        let xBefore = this.x;

        switch (evt.key) {
            case "ArrowUp":
                this.y -= this.shift_amount;
                break;
            case "w":
                this.y -= this.shift_amount;
                break;

            case "ArrowDown":
                this.y += this.shift_amount;
                break;
            case "s":
                this.y += this.shift_amount;
                break;

            case "ArrowRight":
                this.x += this.shift_amount;
                break;
            case "d":
                this.x += this.shift_amount;
                break;
            
            case "ArrowLeft":
                this.x -= this.shift_amount;
                break;
            case "a":
                this.x -= this.shift_amount;
                break;
        }

        // stop bad scroll if it had an effect on the game
        if (yBefore != this.y || xBefore != this.x) {
            evt.preventDefault();
        }

        this.y = Math.max(this.y, 0);
        this.x = Math.max(this.x, 0);

        if (this.board_size != undefined) {
            this.y = Math.min(this.y, this.board_size - SHOWN_TILES)
            this.x = Math.min(this.x, this.board_size - SHOWN_TILES)
        }
    }
}
const camera = new Camera();

let terrainImages = [
    new Image(TILE_SIZE, TILE_SIZE), // ocean
    new Image(TILE_SIZE, TILE_SIZE), // grassland
    new Image(TILE_SIZE, TILE_SIZE), // hills
    new Image(TILE_SIZE, TILE_SIZE), // forest
    new Image(TILE_SIZE, TILE_SIZE) // mountains
]

function loadTerrainImages() {
    const sources = ["./images/ocean.png", "./images/grassland.png", "./images/hills.png", "./images/forest.png", "./images/mountains.png"];
    sources.forEach((source, index) => {
        terrainImages[index].src = source;
    })
}

function getColorFromPlayerIndex(index) {
    // pink, green, cyan, yellow
    return ["#ff6eee", "#00ff00", "#00ffff", "#ffff00"][index];
}

function fabricText(content, rect) {
    return new fabric.Text(content, {
        left: rect.left + 6, // ugly, but the alignment was not working and it was infuriating
        top: rect.top + 2,
        width: TILE_SIZE,
        height: TILE_SIZE,
        fontSize: 24,
        fill: "black",
        textAlign: "center"
    });
}

function drawBoard(board, camera) {
    // TODO: change terrain colors to images
    const terrainColors = ["#006994", "#7cfc00", "#ffa500", "#228B22", "#808080", "#000000"]

    board.forEach((col, y) => {
        col.forEach((tile, x) => {
            let left = (x - camera.x) * TILE_SIZE;
            let top = (y - camera.y) * TILE_SIZE;
            if (terrainImages[tile].src == "") {
                let rect = new fabric.Rect({
                    left: left,
                    top: top,
                    fill: terrainColors[tile],
                    width: TILE_SIZE,
                    height: TILE_SIZE
                });
                canvas.add(rect);
            }
            else {
                let image = terrainImages[tile];
                let imageInstance = new fabric.Image(image, {
                    left: left,
                    top: top,
                    width: TILE_SIZE,
                    height: TILE_SIZE
                });
                canvas.add(imageInstance);
            }
        })
    })
}

function drawUnits(units, label, countStrength=false) {
    // TODO: make graphics a list of images, not strings
    units.forEach((playerUnits, index) => {
        let color = getColorFromPlayerIndex(index);
        let totalStrength = 0;
        playerUnits.forEach(unit => {
            if (countStrength) {
                totalStrength += unit.hitpoints;
            }

            let rect = new fabric.Rect({
                left: (unit.x - camera.x) * TILE_SIZE,
                top: (unit.y - camera.y) * TILE_SIZE,
                fill: color,
                width: TILE_SIZE,
                height: TILE_SIZE,
                rx: TILE_SIZE / 2,
                ry: TILE_SIZE / 2
            });

            if (countStrength) {
                var text = fabricText(toString(totalStrength), rect);
            }
            else {
                var text = fabricText(label, rect)
            } 

            canvas.add(rect);
            canvas.add(text);
        })
    })
}

function layFog(units, board, camera) {
    // check if spectate full is on
    let element = document.getElementById("spectate-full");
    if (element.checked) {
        return;
    }

    const fog = board.map(col => col.map(tile => true)); // fog everything to start
    const fogDensity = 3
    for (player = 0; player < 4; ++player) {
        let spectateID = `spectate-${player}`;
        let inputElement = document.getElementById(spectateID);
        if (inputElement.checked) { // they are spectating this player
            // remove fog
            let playerUnits = units[player];
            playerUnits.forEach((unit) => {
                for (xShift = -fogDensity; xShift <= fogDensity; xShift++) {
                    let trueX = unit.x + xShift;
                    if (trueX < 0 || trueX >= board.length) {
                        continue;
                    }

                    for (yShift = -fogDensity; yShift <= fogDensity; yShift++) {
                        let trueY = unit.y + yShift;
                        if (trueY < 0 || trueY >= board[0].length) {
                            continue;
                        }
                        if (fog[trueX][trueY]) {
                            fog[trueX][trueY] = Math.abs(trueX - unit.x) + Math.abs(trueY - unit.y) > fogDensity;
                        }
                    }
                }
            });
        }
    }

    fog.forEach((col, x) => {
        col.forEach((tile, y) => {
            if (tile) {
                let rect = new fabric.Rect({
                    left: (x - camera.x) * TILE_SIZE,
                    top: (y - camera.y) * TILE_SIZE,
                    fill: "black",
                    width: TILE_SIZE + 1,
                    height: TILE_SIZE + 1
                });

                canvas.add(rect);
            }
        });
    });
}

function updatePlayers(players) {
    players.forEach((player, index) => {
        // update name
        let nameElement = document.getElementById(`player-${index}-name`);
        nameElement.innerHTML = player.name;

        // update offense
        let offenseElement = document.getElementById(`player-${index}-offense`);
        offenseElement.innerHTML = player.offense;

        // update defense
        let defenseElement = document.getElementById(`player-${index}-defense`);
        defenseElement.innerHTML = player.defense;

        // update background
        let divElement = document.getElementById(`player-${index}`);
        let color = getColorFromPlayerIndex(index);
        divElement.style["background-color"] = color;

        // update name in spectate options
        let spectateNameElement = document.getElementById(`spectate-${index}-label`);
        spectateNameElement.innerHTML = player.name;
        spectateNameElement.style.color = color;
    });
}

function updateResources(resources) {
    const resourceTypes = ["production", "food", "trade"];
    resources.forEach((playerResources, index) => {
        resourceTypes.forEach(type => {
            let spanElement = document.getElementById(`player-${index}-${type}`);
            spanElement.innerHTML = playerResources[type];
        });
    });
}

function updateShownTiles() {
    let inputElement = document.getElementById("map-display-size");
    let size = parseInt(inputElement.value)
    if (!isNaN(size)) {
        if (size > 1 && size <= 32) {
            SHOWN_TILES = size;
        }
    }
}

async function main() {
    // make api requests
    const board = await JSON.parse(await (await fetch(`/api/board?key=${apiKey}`)).text());
    const cities = await JSON.parse(await (await fetch(`/api/cities?key=${apiKey}`)).text());
    const armies = await JSON.parse(await (await fetch(`/api/armies?key=${apiKey}`)).text());
    const workers = await JSON.parse(await (await fetch(`/api/workers?key=${apiKey}`)).text());

    const players = await JSON.parse(await (await fetch(`/api/players?key=${apiKey}`)).text());
    const resources = await JSON.parse(await (await fetch(`/api/resources?key=${apiKey}`)).text());

    // update shown tiles
    updateShownTiles();

    // update player names, offense, defense, and bg color
    if (players.error == null) {
        updatePlayers(players.players);
    }

    // update resources
    if (resources.error == null) {
        updateResources(resources.resources);
    }

    // clear canvas
    canvas.clear();

    // draw terrain
    if (board.error == null) {
        canvas.setWidth(SHOWN_TILES * TILE_SIZE);
        canvas.setHeight(SHOWN_TILES * TILE_SIZE);

        camera.board_size = board.board.length;
        drawBoard(board.board, camera);
    }
    
    let units = [Array(), Array(), Array(), Array()];

    // draw armies
    if (armies.error == null) {
        drawUnits(armies.armies, "A", true);
        units.forEach((array, player) => {
            units[player] = array.concat(armies.armies[player]);
        })
    }

    // draw workers
    if (workers.error == null) {
        drawUnits(workers.workers, "W");
        units.forEach((array, player) => {
            units[player] = array.concat(workers.workers[player]);
        })
    }

    // draw cities (notice this is last so that cities are on top)
    if (cities.error == null) {
        drawUnits(cities.cities, "C");
        units.forEach((array, player) => {
            units[player] = array.concat(cities.cities[player]);
        })
    }

    // lay the fog
    layFog(units, board.board, camera);
}

window.onload = () => {
    // bind controls
    document.addEventListener("keydown", (evt) => camera.update(evt), false);

    loadTerrainImages();
    canvas = new fabric.Canvas("screen");
    window.setInterval(main, 500);
}
