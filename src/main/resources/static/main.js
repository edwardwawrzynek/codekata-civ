var canvas;
const apiKey = "observe0";


let printed = false;
let boardDrawn = false;

// display constants
const TILE_SIZE = 32;
const SHOWN_TILES = 16;

class Camera {
    constructor() {
        this.x = 0
        this.y = 0
        this.shift_amount = 1;
        this.board_size;
    }

    update(evt) {
        console.log(evt.key);
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
            default:
                // do nothing, key didn't matter
        }

        this.y = Math.max(this.y, 0);
        this.x = Math.max(this.x, 0);

        if (this.board_size != undefined) {
            console.log(this.board_size - SHOWN_TILES)
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
    const sources = ["./ocean.png", "./grassland.png", "./hills.png"];//, "./forest.png", "./mountains.png"]
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

function drawUnits(units, label) {
    // TODO: make graphics a list of images, not strings
    units.forEach((playerUnits, index) => {
        let color = getColorFromPlayerIndex(index);
        playerUnits.forEach(unit => {
            let rect = new fabric.Rect({
                left: (unit.x - camera.x) * TILE_SIZE,
                top: (unit.y - camera.y) * TILE_SIZE,
                fill: color,
                width: TILE_SIZE,
                height: TILE_SIZE,
                rx: TILE_SIZE / 2,
                ry: TILE_SIZE / 2
            });

            let text = fabricText(label, rect)

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
    for (player=1; player<=4; ++player) {
        let spectateID = `spectate-${player}`;
        let inputElement = document.getElementById(spectateID);
        if (inputElement.checked) { // they are spectating this player
            // remove fog
            let playerUnits = units[player - 1];
            playerUnits.forEach((unit) => {
                for (shiftX = -2; shiftX <= 2; ++shiftX) {
                    let trueX = shiftX + unit.x;
                    if (trueX < 0 || trueX >= fog.length) {
                        continue;
                    }
                    for (shiftY = -2; shiftY <= 2; ++shiftY) {
                        let trueY = shiftY + unit.y;
                        if (trueY < 0 || trueY >= fog[0].length) {
                            continue;
                        }

                        console.log("remove fog")
                        // remove fog from here
                        fog[trueX][trueY] = false;
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

async function main() {
    // make an api request
    const board = await JSON.parse(await (await fetch(`/api/board?key=${apiKey}`)).text());
    const cities = await JSON.parse(await (await fetch(`/api/cities?key=${apiKey}`)).text());
    const armies = await JSON.parse(await (await fetch(`/api/armies?key=${apiKey}`)).text());
    const workers = await JSON.parse(await (await fetch(`/api/workers?key=${apiKey}`)).text());

    // clear canvas
    canvas.clear();

    // draw terrain
    if (board.error == null) {
        canvas.setWidth(SHOWN_TILES * TILE_SIZE);
        canvas.setHeight(SHOWN_TILES * TILE_SIZE);

        camera.board_size = board.board.length
        drawBoard(board.board, camera);
    }
    
    let units = [Array(), Array(), Array(), Array()];

    // draw armies
    if (armies.error == null) {
        drawUnits(armies.armies, "A");
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
    document.addEventListener("keydown", (evt) => camera.update(evt));

    loadTerrainImages();
    canvas = new fabric.Canvas("screen");
    window.setInterval(main, 500)
}
