var Console = {};

Console.log = (function(message) {
	var console = document.getElementById('console');
	var p = document.createElement('p');
	p.style.wordWrap = 'break-word';
        
        //p.style.color = '#ffa500';
	p.innerHTML = message;
	console.appendChild(p);
	/*while (console.childNodes.length > 25) {
		console.removeChild(console.firstChild);
	}
	console.scrollTop = console.scrollHeight;*/
});

var name;
let creador = false;
function myFunction()
{

    do{
    
        name=prompt("Inserta tu nombre","Nombre");
        
    }while(name == "Nombre" || name == "null");


	var newSnake = {
						
		funcion: "crearSerpiente",
		params: [name]
	
	}
	game.socket.send(JSON.stringify(newSnake));


}

let game;
let enPartida = false;
let salaP = null;
class Snake {

	constructor() {
		this.snakeBody = [];
		this.color = null;
		this.puntos = 0;
		this.nombre = null;

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

	
	if(game.nextFrame != null)
		game.stopGameLoop();
	else
		game.context.clearRect(0, 0, 640, 480);

	var o = {

		funcion: "salirSala",
		params: [name]

	}
	game.socket.send(JSON.stringify(o));
	
	borrarDiv('#salaActual');
	document.getElementById("partidas-container").style.display = 'inline-block';
	document.getElementById("serpiente").style.display = "block";
	document.getElementById("ranking").style.display = "block";
	salaP = null;

}
class Food {
	constructor () {
		this.x =-1;
		this.y=-1;
		this.color= null;
	}
	
	draw (context){
		
		context.fillStyle = this.color;
		context.fillRect (this.x,this.y,game.gridSize,game.gridSize);
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
		this.food = new Food();
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
		var space = 20;
		for (var id in this.snakes) {
			this.drawPoints(space, id);			
			this.snakes[id].draw(this.context);
			space += 40;
		}
		this.food.draw(this.context);
	}

	updateFood (x,y,color){

		this.food.x=x;
		this.food.y=y;
		this.food.color= color;

	}
	addSnake(id, color,name,ptos) {
		this.snakes[id] = new Snake();
		this.snakes[id].color = color;
		this.snakes[id].nombre = name;
		this.snakes[id].puntos = ptos;
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

	updatePuntos(id,ptos){

		this.snakes[id].puntos = ptos;

	}

	drawPoints(space, id){

		this.context.font = "20px Tw Cen MT";
		this.context.fillStyle = this.snakes[id].color;
                this.context.textAlign="left";
		this.context.fillText(this.snakes[id].nombre + ": " + this.snakes[id].puntos,19,space);

	}
	sala(jugadores,sala){
		
		document.getElementById("serpiente").style.display = "none";
		document.getElementById("ranking").style.display = "none";
		game.context.clearRect(0, 0, 640, 480);
		var d = document.getElementById("salaActual");
		borrarDiv('#salaActual');
		this.context.font = "20px Tw Cen MT";
		this.context.fillStyle = "white";
		this.context.textAlign = "center";
		salaP = sala;
		this.context.fillText("Sala: " + sala, 300, 50);
		var alto = 50;
		var b1 = document.createElement("button");
		b1.id = "salir";
		b1.textContent = "Salir";

		b1.addEventListener("click", salir);
		
		d.appendChild(b1);
		if(jugadores.length >= 2 && creador){

			var b2 = document.createElement("button");
			b2.textContent = "Comenzar juego";
			b2.id = "comenzar";
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
			this.context.font = "20px Tw Cen MT";
			this.context.fillText(jugadores[i], 300, alto + inc);

		}
	
	}

	connect() {

            this.socket = new WebSocket('ws://'+ window.location.host +'/snake');

            this.socket.onopen = () => {

                    // Socket open.. start the game loop.
                    Console.log('Info: WebSocket connection opened.');
                    Console.log('Info: Press an arrow key to begin.');

                    myFunction();
                    
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
								this.updateFood(packet.food[0], packet.food[1], packet.food[2]);
								
                                break;
						case 'join':
								//Console.log("Serpiente agregada");
                                for (var j = 0; j < packet.data.length; j++) {
                                        this.addSnake(packet.data[j].id, packet.data[j].color, packet.data[j].name, packet.data[j].puntos);
								}
								//this.sala(packet.name);
                                break;
                        case 'leave':
								this.removeSnake(packet.id);
                                break;
                        case 'dead':
                                Console.log('Info: Your snake is dead, bad luck!');
								this.direction = 'none';
								this.updatePuntos(packet.id,packet.puntos);
                                break;
                        case 'kill':
								Console.log('Info: Head shot!');
								this.updatePuntos(packet.id,packet.puntos);
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
								if(document.getElementById("comenzar")!= null)
									document.getElementById("salaActual").removeChild(document.getElementById("comenzar"));
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

						case 'falloNombre':
								myFunction();
								break;
						
						case 'sumaPuntos': 
								this.updatePuntos(packet.id,packet.puntos);
								//Console.log("Puntos: " + this.snakes[packet.id].puntos);
								break;

						case 'finPartida':

								this.stopGameLoop();
								window.setTimeout(function(){

									game.context.clearRect(0,0,640,480);
									game.context.font="20pt Verdana";
									game.context.fillStyle = "#CCCCCC";
									game.context.fillText("¡Ha ganado: " + packet.ganador + " con \n" + packet.puntos + " puntos!",90,240);
                                                                        
                                    window.setTimeout (salir, 2000);
								}, 2000);
								
								break;
						case 'jugadorConecta':	addListaJugadores(JSON.parse(packet.names));
								break;
						case 'jugadorDesconecta': removeListaJugadores(packet.name);
								break;
                    }
            }
                    
	}
}

function addListaJugadores(jugadores){

	$('#lista').empty();
	document.getElementById("lista").textContent = "Jugadores Online";
	for(var i = 0; i < jugadores.length; i++){
		var j = document.createElement("div");
		j.id = jugadores[i];
		j.textContent = jugadores[i];
		document.getElementById("lista").appendChild(j);
	}

}

function removeListaJugadores(jugador){

	var n = document.getElementById(jugador);
	document.getElementById("lista").removeChild(n);

}

function postPartida(d){

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

	document.getElementById("partidas-container").style.display = "none";
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
			postPartida(dificultad);
		else
			buscar(dificultad);

		document.getElementById("partidas-container").style.display = "inline-block";
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
			postPartida(dificultad);
		else
			buscar(dificultad);
		document.getElementById("partidas-container").style.display = "inline-block";
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
			postPartida(dificultad);
		else
			buscar(dificultad);
		document.getElementById("partidas-container").style.display = "inline-block";
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

		if(p !== null){
			salaP = p;
			selector("post");
		}
		
    });
	$('#actualizar-btn').click(function(){
		
		partidas()
	});

	$('#buscar-btn').click(function(){
		
		selector();
		
	});

	$('#ranking').click(function(){

		document.getElementById("serpiente").style.display = "none";
		document.getElementById("ranking").style.display = "none";
		//RANKING
		$.ajax({
			
			method:"GET",
			url:"http://" + window.location.host + "/muroPuntos",
	
		}).done(function(data){
			
			console.log(JSON.parse(data));
			game.context.clearRect(0, 0, 640, 480);
	
			var puntos = JSON.parse(data);
			for(var i = 0; i < puntos.length; i++){
	
				var array = JSON.parse(puntos[i]);
				var div = document.createElement("div");
				div.textContent = array[0] + " : " + array[1]; //0 nombre, 1 puntos
				document.getElementById("muro").appendChild(array);
	
			}

			var sal = document.createElement('button');
			sal.textContent = "Salir";
			sal.id = "salirRanking";
			sal.addEventListener("click",function(){

				borrarDiv('#muro');
				document.getElementById("botonesRanking").removeChild(sal);
				game.context.clearRect(0,0,640,480);
				document.getElementById("partidas-container").style.display = 'inline-block';
				document.getElementById("serpiente").style.display = "block";
				document.getElementById("ranking").style.display = "block";

			});

			document.getElementById("botonesRanking").appendChild(sal);

		
		});

	});
    
})

function partidas(){

    $.ajax({

        method:"GET",
        url:"http://" + window.location.host + "/partidas",

    }).done(function(data){
		
		console.log(JSON.parse(data));
		borrarDiv('#partidas');

		var partidas = JSON.parse(data);
		for(var i = 0; i < partidas.length; i++){

			crearDiv(partidas[i]); 

		}
    
    });

}

function borrarDiv(id){
    
    $(id).empty();
    
}

function crearDiv(info){

	//info = JSON.parse(info);
	var newDiv = document.createElement("div"); 
	var d = info[2]==1?"fácil":info[2]==2?"normal":"dificil";
	newDiv.id = info[0];					//Id = nombreSala
	var newContent = document.createTextNode(info[0] + "     " + info[1] + "/4        " + d); 
	newDiv.appendChild(newContent); //añade texto al div creado. 
	var boton = document.createElement("button");
	boton.type = "button";
	boton.textContent = "unirse";
	boton.style.alignSelf = "right";
	boton.id = "unirse-btn"
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