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
let creador = false;
function myFunction()
{

    do{
    
        name=prompt("Inserta tu nombre","Nombre");
        
    }while(name == "Nombre");

}

let game;
let enPartida = false;
let salaP = null;
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

function salir(){

	game.context.clearRect(0, 0, 640, 480);
	var o = {

		funcion: "salirSala",
		params: [name]

	}
	game.socket.send(JSON.stringify(o));
	
	borrarDiv('#salaActual');
	document.getElementById("partidas-container").style.display = 'inline-block';
	salaP = null;

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
		} else{
			this.context.clearRect(0, 0, 640, 480);
		}
	}

	sala(jugadores,sala){
		
		game.context.clearRect(0, 0, 640, 480);
		var d = document.getElementById("salaActual");
		borrarDiv('#salaActual');
		this.context.font = "30px Verdana";
		this.context.fillStyle = "white";
		salaP = sala;
		this.context.fillText("Sala: " + sala, 50, 50);
		var alto = 50;
		var b1 = document.createElement("button");
		b1.textContent = "Salir";

		b1.addEventListener("click", salir);
		
		d.appendChild(b1);
		if(jugadores.length >= 2 && creador){

			var b2 = document.createElement("button");
			b2.textContent = "Comenzar juego";
			b2.addEventListener("click",function(){

				var ob = {

					funcion: "comenzarPartida",
					params: [salaP]

				}

				game.socket.send(JSON.stringify(ob));

			})
			d.appendChild(b2);

		}

		var inc = 0;
		for(var i = 0; i < jugadores.length; i++){

			inc += 50;
			this.context.fillText(jugadores[i], 50, alto + inc);

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
                    var ping = {
                        funcion: "ping",
                        params:[""]
                    }

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
								//this.sala(packet.name);
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
                                if(packet.enPartida)
                                    color = 'green';
                                else
                                    color = 'red';
								Console.log(packet.name.fontcolor(color) + " : " + packet.mensaje);
								break;
						case 'sala' :		
								document.getElementById("partidas-container").style.display = 'none';
								this.sala(JSON.parse(packet.players),packet.sala);
								break;
						case 'jugar' : 
								this.startGameLoop();
								break;

						case 'quitarSala': 
								//eliminamos el div de la partida porque se han salido todos los jugadores
								//Console.log(packet.sala);

								var node = document.getElementById(packet.sala);
								if(node !== null){
									node.parentNode.removeChild(node);
								}
								break;

						case 'senal' :
								Console.log(packet.contenido);
								break;

						case 'finJuego':
								Console.log(packet.contenido);
								salir();
								break;

                    }
            }
                    
	}
}

function post(d){

	document.getElementById("selector").style.display = 'none';
	var ob = {
		
		name: salaP,
		dif: d

	}
	$.ajax({

		method: "POST",
		url: "http://" + window.location.host + "/newGame",
		data: JSON.stringify(ob),
		processData: false,
		headers: {

			"Content-type":"application/json"

		}
	}).done(function(data){

		console.log("Creada partida: " + salaP);
		creador = true;
		partidas();
		
	});

}

function buscar(dif){
	
	document.getElementById("selector").style.display = 'none';
	var o = {
		
		funcion: "matchMaking",
		params:[dif] //PASAR LA DIFICULTAD

	}

	game.socket.send(JSON.stringify(o));

}
function selector(funcion){

	var dificultad; //facil:1,medio:2,dificil:4
	var sel = document.getElementById("selector");
	borrarDiv('#selector');
	sel.style.display = 'inline-flex';
	var p1 = document.createElement('p');
	p1.innerHTML = "SELECCIONE UNA DIFICULTAD";
	sel.appendChild(p1);
	var p2 = document.createElement('p');
	var facil = document.createElement('button');
	facil.textContent = "FÁCIL";
	facil.id = "f";
	facil.addEventListener("click",function(){

		dificultad = 1
		if(funcion=="post")
			post(dificultad);
		else
			buscar(dificultad);
	});
	p2.appendChild(facil);
	sel.appendChild(p2);
	
	var p3 = document.createElement('p');
	var medio = document.createElement('button');
	medio.textContent = "NORMAL";
	medio.id = "m";
	medio.addEventListener("click",function(){

		dificultad = 2
		if(funcion=="post")
			post(dificultad);
		else
			buscar(dificultad);
	});
	p3.appendChild(medio);
	sel.appendChild(p3);
	var p4 = document.createElement('p');
	var dificil = document.createElement('button');
	dificil.textContent = "DIFÍCIL";
	dificil.id = "d";
	dificil.addEventListener("click",function(){

		dificultad = 4
		if(funcion=="post")
			post(dificultad);
		else
			buscar(dificultad);
	});
	p4.appendChild(dificil);
	sel.appendChild(p4);

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

		salaP = p;
		selector("post");
		
    });
	$('#actualizar-btn').click(function(){
		
		partidas()
	});

	$('#buscar-btn').click(function(){
		
		selector();
		
	});
    
})

function partidas(){

    $.ajax({

        method:"GET",
        url:"http://" + window.location.host + "/partidas",

    }).done(function(data){
        
		borrarDiv('#partidas');

		var partidas = JSON.parse(data);
		for(var i = 0; i < partidas.length; i++){

			crearDiv(partidas[i]); //para que salga sin comillas parseamos (otra vez)

		}
    
    });

}

function borrarDiv(id){
    
    $(id).empty();
    
}

function crearDiv(info){

	//info = JSON.parse(info);
	var newDiv = document.createElement("div"); 
	newDiv.id = info[0];					//Id = nombreSala
	var newContent = document.createTextNode(info[0] + "     " + info[1] + "/4        " + info[2]); 
	newDiv.appendChild(newContent); //añade texto al div creado. 
	var boton = document.createElement("button");
	boton.type = "button";
	boton.textContent = "unirse";
	boton.style.alignSelf = "right";
	boton.id = "#unirse-btn"
	boton.addEventListener("click", function(){	
		var part = {
            funcion: "unirGame",
            params:[info[0],name]
        }

        game.socket.send(JSON.stringify(part));
	},false);
	salaP = info[0];
	newDiv.appendChild(boton);

	$('#partidas').append(newDiv);

}

game = new Game();

game.initialize()