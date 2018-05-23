var Console = {};

Console.log = (function(message) {
	var console = document.getElementById('console');
	var p = document.createElement('p');
	p.style.wordWrap = 'break-word';
        
        //p.style.color = '#ffa500';
	p.innerHTML = message;
	console.appendChild(p);
	while (console.childNodes.length > 25) {
		console.removeChild(console.firstChild);
	}
	console.scrollTop = console.scrollHeight;
});

var name;
function myFunction()
{

    do{
    
        name=prompt("Inserta tu nombre","Nombre");
        
    }while(name == "Nombre");

}

let game;

class Snake {

	constructor() {
		this.snakeBody = [];
		this.color = null;
	}

	draw(context) {
		for (var pos of this.snakeBody) {
			context.fillStyle = this.color;
			context.fillRect(pos.x, pos.y,
				game.gridSize, game.gridSize);
		}
	}
}

class Game {

	constructor(){
	
		this.fps = 30;
		this.socket = null;
		this.nextFrame = null;
		this.interval = null;
		this.direction = 'none';
		this.gridSize = 10;
		
		this.skipTicks = 1000 / this.fps;
		this.nextGameTick = (new Date).getTime();
	}

	initialize() {	
	
		this.snakes = [];
		let canvas = document.getElementById('playground');
		if (!canvas.getContext) {
			Console.log('Error: 2d canvas not supported by this browser.');
			return;
		}
		
		this.context = canvas.getContext('2d');
		window.addEventListener('keydown', e => {
			
			var code = e.keyCode;
			if (code > 36 && code < 41) {
				switch (code) {
				case 37:
					if (this.direction != 'east')
						this.setDirection('west');
					break;
				case 38:
					if (this.direction != 'south')
						this.setDirection('north');
					break;
				case 39:
					if (this.direction != 'west')
						this.setDirection('east');
					break;
				case 40:
					if (this.direction != 'north')
						this.setDirection('south');
					break;
				}
			}
		}, false);
		
		this.connect();
	}

	setDirection(direction) {
		this.direction = direction;
                var dir = {
                    
                    funcion:"direccion",
                    params:[this.direction]
                    
                }
		this.socket.send(JSON.stringify(dir));
		//Console.log('Sent: Direction ' + direction);
	}

	startGameLoop() {
	
		this.nextFrame = () => {
			requestAnimationFrame(() => this.run());
		}
		
		this.nextFrame();		
	}

	stopGameLoop() {
		this.nextFrame = null;
		if (this.interval != null) {
			clearInterval(this.interval);
		}
	}

	draw() {
		this.context.clearRect(0, 0, 640, 480);
		for (var id in this.snakes) {			
			this.snakes[id].draw(this.context);
		}
	}

	addSnake(id, color) {
		this.snakes[id] = new Snake();
		this.snakes[id].color = color;
	}

	updateSnake(id, snakeBody) {
		if (this.snakes[id]) {
			this.snakes[id].snakeBody = snakeBody;
		}
	}

	removeSnake(id) {
		this.snakes[id] = null;
		// Force GC.
		delete this.snakes[id];
	}

	run() {
	
		while ((new Date).getTime() > this.nextGameTick) {
			this.nextGameTick += this.skipTicks;
		}
		this.draw();
		if (this.nextFrame != null) {
			this.nextFrame();
		}
	}

	connect() {

            this.socket = new WebSocket('ws://'+ window.location.host +'/snake');

            this.socket.onopen = () => {

                    // Socket open.. start the game loop.
                    Console.log('Info: WebSocket connection opened.');
                    Console.log('Info: Press an arrow key to begin.');

                    myFunction();
                    var newSnake = {
                        funcion: "crearSerpiente",
                        params: [name]
                    
                    }
                    this.socket.send(JSON.stringify(newSnake));
                    this.startGameLoop();
                    var ping = {
                        funcion: "ping",
                        params:[""]
                    }

					crearDiv("prueba");
                    setInterval(() => this.socket.send(JSON.stringify(ping)), 5000);
            }

            this.socket.onclose = () => {
                    Console.log('Info: WebSocket closed.');
                    this.stopGameLoop();
            }

            this.socket.onmessage = (message) => {

                    var packet = JSON.parse(message.data);

                    switch (packet.type) {
                        case 'update':
                                for (var i = 0; i < packet.data.length; i++) {
                                        this.updateSnake(packet.data[i].id, packet.data[i].body);
                                }
                                break;
                        case 'join':
                                for (var j = 0; j < packet.data.length; j++) {
                                        this.addSnake(packet.data[j].id, packet.data[j].color);
                                }
                                break;
                        case 'leave':
                                this.removeSnake(packet.id);
                                break;
                        case 'dead':
                                Console.log('Info: Your snake is dead, bad luck!');
                                this.direction = 'none';
                                break;
                        case 'kill':
                                Console.log('Info: Head shot!');
                                break;

                        case 'chat':
                                var color;
                                if(packet.partida)
                                    color = 'green';
                                else
                                    color = 'red';
                                Console.log(packet.name.fontcolor(color) + " : " + packet.mensaje);

                    }
            }
                    
	}
}

$(document).ready(function(){
    $('#send-btn').click(function() {
        var object = {
            funcion: "Chat",
            params:[name, $('#message').val()]
        }

        game.socket.send(JSON.stringify(object));
        $('#message').val('');
    });
    $('#crear-btn').click(function(nombrePartida) {
		var p;

		do{

			p =prompt("Inserta el nombre de la sala","Nombre");

		}while(p =="Nombre");

		$.ajax({

			method: "POST",
			url: "http://" + window.location.host + "/newGame",
			data: JSON.stringify(p),
			processData: false,
			headers: {

				"Content-type":"application/json"

			}
		}).done(function(data, textStatus, jqXHR){

			console.log(textStatus+" " + jqXHR.statusCode());
			
		}).fail(function(data, textStatus, jqXHR){

			console.log(textStatus + " " + jqXHR.statusCode());

		});

		partidas();

    });
    $('#actualizar-btn').click(function() { //actualizar partidas
        
    });
    
})

function partidas(){

    $.ajax({

        methos:"GET",
        url:"http://" + window.location.host + "/partidas",
        processData:false,
        headers:{

            "Content-type":"application/json"
        
        }

    }).done(function(data, textStatus, jqXHR){

		var partidas = JSON.parse(data);
		for(var i = 0; i < partidas.length; i++){

			crearDiv(partidas[i]);

		}
    
    }).fail(function(data, textStatus,jqXHR){

        console.log(textStatus + " " + jqXHR.statusCode());
    
    });

}

function crearDiv(nombreP){

	var newDiv = document.createElement("div"); 
	var newContent = document.createTextNode(nombreP); 
	newDiv.appendChild(newContent); //añade texto al div creado. 
	var boton = document.createElement("button");
	boton.type = "button";
	boton.textContent = "unirse";
	boton.style.alignSelf = "right";
	boton.id = "#unirse-btn"
	boton.addEventListener("click", function(){	
		var part = {
            funcion: "unirGame",
            params:[nombreP]
        }

        game.socket.send(JSON.stringify(part));
	},false);
	newDiv.appendChild(boton);
	
	document.body.appendChild(newDiv);

}
game = new Game();

game.initialize()