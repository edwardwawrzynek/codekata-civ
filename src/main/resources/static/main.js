var canvas;
const apiKey = "observe0";

async function main() {
    // make an api request
    const board = await JSON.parse(await (await fetch(`/api/board?key=${apiKey}`)).text());
    console.log(board);
    if(board.error == null) {
        // do something with it

        //drawing example
        let rect = new fabric.Rect({
          left: 100,
          top: 100,
          fill: 'red',
          width: 20,
          height: 20
        });
        canvas.add(rect);

    }
};

window.onload = () => {
    canvas = new fabric.Canvas("screen");
    window.setInterval(main, 1000);
}
