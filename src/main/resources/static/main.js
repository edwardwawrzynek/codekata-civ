var canvas;
const apiKey = "observe0";


let printed = false;
let boardDrawn = false;

// display constants
const TILE_SIZE = 32;
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

function drawBoard(board) {
    // TODO: change terrain colors to images
    const terrainColors = ["#006994", "#7cfc00", "#ffa500", "#228B22", "#808080", "#000000"]
    board.forEach((row, x) => {
        row.forEach((tile, y) => {
            let left = x * TILE_SIZE;
            let top = y * TILE_SIZE;
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

function drawCities(cities) {
    cities.forEach((playerCities, index) => {
        let color = getColorFromPlayerIndex(index);
        playerCities.forEach(city => {
            let rect = new fabric.Rect({
                left: city.x * TILE_SIZE,
                top: city.y * TILE_SIZE,
                fill: color,
                width: TILE_SIZE,
                height: TILE_SIZE
            });
            canvas.add(rect);
        })
    })
}

async function main() {
    // make an api request
    const board = await JSON.parse(await (await fetch(`/api/board?key=${apiKey}`)).text());
    const cities = await JSON.parse(await (await fetch(`/api/cities?key=${apiKey}`)).text());
    
    // clear canvas
    canvas.clear();

    // draw terrain
    if (board.error == null) {
        canvas.setWidth(board.board.length * TILE_SIZE);
        canvas.setHeight(board.board[0].length * TILE_SIZE);

        drawBoard(board.board);
    }
     
    // draw cities
    if (cities.error == null) {
        drawCities(cities.cities);
    }
};

window.onload = () => {
    loadTerrainImages();
    canvas = new fabric.Canvas("screen");
    window.setInterval(main, 1000);
}
