var Console = {};

Console.log = (function(message) { //Consola para chat
	var console = document.getElementById('console');
	var p = document.createElement('p');
	p.style.wordWrap = 'break-word';
	p.innerHTML = message;
	console.appendChild(p);

});

var name;

function pedirNombre() //solicitar nombre de usuario
{

    do{
    
        name=prompt("Inserta tu nombre","Nombre"); //Nombre es el valor por defecto que tiene la variable name
        
	}while(name == "Nombre" || name == "null"); //mientras no escriba nada o lo deje en blanco se le sigue solicitando
												//pues el nombre es imprescindible para jugar


	var newSnake = {
						
		funcion: "crearSerpiente", //metodo que se llama desde el handleText
		params: [name]
	
	}
	game.socket.send(JSON.stringify(newSnake)); //envio de mensaje al socket


}

let game;
let enPartida = false; //indica si el jugador esta en partida (true) o en el lobby (false)
let salaP = null; //nombre de la sala donde se encuentra el usuario

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

function salir(){ //metodo para salir de una sala (este o no jugando)

	
	if(game.nextFrame != null) //si no se ha parado el juego, se para
		game.stopGameLoop();
	else //si el juego esta parado (esta en la sala esperando a entrar a jugar) se limpia el canvas
		game.context.clearRect(0, 0, 640, 480);

	var o = {

		funcion: "salirSala", //funcion en Java
		params: []

	}
	game.socket.send(JSON.stringify(o));
	
	borrarDiv('#salaActual'); //borramos el div que contiene los nombres de los jugadores en la sala
	document.getElementById("partidas-container").style.display = 'inline-block';
	document.getElementById("serpiente").style.display = "block";
	document.getElementById("ranking").style.display = "inline-block";
	salaP = null; //lo ponemos a null porque al salir de la sala, ya no se encuentra en esa partida
	partidas(); //hacemos get de las partidas que hay creadas (APIRest)

}

class Food { //clase para la comida 

	constructor () {
		this.x =-1; //posicion en x
		this.y=-1; //posicion en y
		this.color= null; //color
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

	updateFood (x,y,color){ //cuando se recoge una comida, se posiciona otra en el canvas

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

	updatePuntos(id,ptos){ //set de los puntos de cada serpiente

		this.snakes[id].puntos = ptos;

	}

	drawPoints(space, id){ //mostramos por pantalla el nombre del jugador y sus puntos

		this.context.font = "20px Tw Cen MT";
		this.context.fillStyle = this.snakes[id].color;
		this.context.textAlign="left";
		this.context.fillText(this.snakes[id].nombre + ": " + this.snakes[id].puntos,19,space); //space es la posicion en y de las letras

	}
	sala(jugadores,sala,creador){ //sala de espera para entrar a partida (se llama cada vez que entra un jugador en la sala)
		
		////OCULTAMOS LOS ELEMENTOS DEL LOBBY
		document.getElementById("serpiente").style.display = "none";
		document.getElementById("ranking").style.display = "none";
		////CREAMOS LOS ELEMENTOS DE LA SALA
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

		b1.addEventListener("click", salir); //boton de salir de la sala
		
		d.appendChild(b1);
		if(jugadores.length >= 2 && name === creador){ //si hay 2 o mas jugadores, y el usuario es el creador de la sala, le aparece el boton de comenzar partida

			var b2 = document.createElement("button");
			b2.textContent = "Comenzar juego";
			b2.id = "comenzar";
			b2.addEventListener("click",function(){ //comienza partida

				var ob = {

					funcion: "comenzarPartida", //metodo invocado en Java
					params: [salaP] //le pasamos el nombre de la sala para avisar a todos los jugadores que hay en ella

				}

				game.socket.send(JSON.stringify(ob));

			})
			d.appendChild(b2);

		}

		//MOSTRAMOS LOS JUGADORES QUE HAY EN LA SALA
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
					//solicitamos el nombre
                    pedirNombre();
                    
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
						case 'esperaEntrar': //hay 4 jugadores en sala y queremos esperar para unirnos
								espera();
								break;
						case 'update':
                                for (var i = 0; i < packet.data.length; i++) {

										this.updateSnake(packet.data[i].id, packet.data[i].body);
								}
								this.updateFood(packet.food[0], packet.food[1], packet.food[2]);
								
                                break;
						case 'join':
                                for (var j = 0; j < packet.data.length; j++) {
                                        this.addSnake(packet.data[j].id, packet.data[j].color, packet.data[j].name, packet.data[j].puntos);
								}
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

                        case 'chat': //mostramos el nombre del usuario en funcion de si esta en partida (verde) o en el lobby (rojo)
                                var color;
                                if(packet.enPartida)
                                    color = 'green';
                                else
                                    color = 'red';
								Console.log(packet.name.fontcolor(color) + " : " + packet.mensaje);
								break;
						case 'sala' : //sala de espera para empezar a jugar
								document.getElementById("partidas-container").style.display = 'none';//ocultamos las partidas que hay creadas para que no se una a otra
								this.sala(JSON.parse(packet.players),packet.sala,packet.creador);
								break;
						case 'jugar' : //el creador pulsa el boton de comenzar partida
								if(document.getElementById("comenzar")!= null)
									document.getElementById("salaActual").removeChild(document.getElementById("comenzar"));
								this.startGameLoop();
								break;

						case 'quitarSala': 
								//eliminamos el div de la partida porque se han salido todos los jugadores 
								var node = document.getElementById(packet.sala);
								if(node !== null){
									node.parentNode.removeChild(node);
								}
								break;

						case 'senal' : //enviamos un mensaje desde Java
								Console.log(packet.contenido);
								break;

						case 'finJuego': //el usuario se ha salido de la partida
								Console.log(packet.contenido);
								salir();
								break;

						case 'falloNombre': //el nombre ya existe. Se solicita otro
								pedirNombre();
								break;
						
						case 'sumaPuntos': //Actualizamos los puntos del usuario
								this.updatePuntos(packet.id,packet.puntos);
								break;

						case 'finPartida': //fin de la partida

								this.stopGameLoop();
								window.setTimeout(function(){

									game.context.clearRect(0,0,640,480);
									game.context.font="20pt Verdana";
									game.context.fillStyle = "#CCCCCC";

									if(packet.ganador == null)
										game.context.fillText("¡Empate!",90,240);
									else
									game.context.fillText("¡Ha ganado: " + packet.ganador + " con \n" + packet.puntos + " puntos!",90,240);
                                    window.setTimeout (salir, 2000);
								}, 2000);
								
								break;
						case 'jugadorConecta':	addListaJugadores(JSON.parse(packet.names)); //se conecta un nuevo jugador 
								break;
						case 'jugadorDesconecta': removeListaJugadores(packet.name); //se desconecta un jugador
								break;
						case 'finEspera': finEspera(); //el jugador ha entrado a partida tras estar esperando
								break;
                    }
            }
                    
	}
}

function finEspera(){ //eliminamos el boton de cancelar y el mensaje

	document.getElementById("cancelar").style.display = 'none';

}
function espera(){ //el usuario espera por si sale un jugador

	borrarDiv('#cancelar');
	var n = document.getElementById("cancelar");
	n.style.display = 'inline-block';
	n.innerHTML = "Intentando unirse \n a la sala...";
	var boton = document.createElement("button");
	boton.textContent = "Cancelar";
	boton.id = "cancelarEspera";
	boton.addEventListener("click", function(){ //si el usuario decide cancelar, se lo decimos al servidor

		finEspera();
		var ob = {

			funcion: "cancelarEspera",
			params: []

		}

		game.socket.send(JSON.stringify(ob));
		
		
	});

	n.appendChild(boton);

}
function addListaJugadores(jugadores){ //actualizamos la lista de jugadores online (se une un usuario)

	$('#lista').empty();
	document.getElementById("lista").textContent = "Jugadores Online";
	for(var i = 0; i < jugadores.length; i++){
		var j = document.createElement("div");
		j.id = jugadores[i];
		j.textContent = jugadores[i];
		document.getElementById("lista").appendChild(j);
	}

}

function removeListaJugadores(jugador){ //actualizamos la lista de jugadores online (se desconecta un usuario)

	var n = document.getElementById(jugador);
	document.getElementById("lista").removeChild(n);

}

function postPartida(d){ //post a APIRest para guardar la partida creada

	document.getElementById("selector").style.display = 'none';
	var ob = {
		
		name: salaP, //nombre de la partida
		dif: d, //dificultad de la partida
		creador: name //somos el creador de la partida

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
		partidas();
		
	});

}

function buscar(dif){ //Matchmaking
	
	document.getElementById("selector").style.display = 'none';
	var o = {
		
		funcion: "matchMaking",
		params:[dif] //dificultad seleccionada por el usuario

	}

	game.socket.send(JSON.stringify(o));

}
function selector(funcion){ //selector de dificultad

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
    $('#send-btn').click(function() { //boton de enviar del chat
        var object = {
            funcion: "Chat",
            params:[name, $('#message').val()]
        }

		game.socket.send(JSON.stringify(object));

        $('#message').val('');
    });
    $('#crear-btn').click(function(nombrePartida) { //boton de crear partida
		var p;
		
		do{

			p =prompt("Inserta el nombre de la sala","Nombre");

		}while(p =="Nombre" || p == ''); //se le pide un nombre mientras no introduzca algo

		if(p !== null){ //si el usuario cancela, p es null
			salaP = p;
			selector("post");
		}
		
    });
	$('#actualizar-btn').click(function(){ //boton de actualizar
		
		partidas()
	});

	$('#buscar-btn').click(function(){ //boton de buscar partida
		
		selector();
		
	});

	$('#ranking').click(function(){ //boton de ranking

		document.getElementById("serpiente").style.display = "none";
		document.getElementById("ranking").style.display = "none";

		$.ajax({ //get de los 10 mejores puntos
			
			method:"GET",
			url:"http://" + window.location.host + "/muroPuntos",
	
		}).done(function(data){
			
			console.log(JSON.parse(data));
			game.context.clearRect(0, 0, 640, 480);
                        document.getElementById("muro").style.display="inline-block";
                        document.getElementById("muro").innerHTML='Ranking';
			var puntos = JSON.parse(data);
			for(var i = 0; i < puntos.length; i++){
	
				var array = puntos[i];
				var div = document.createElement("div");
				div.textContent = array[0] + " : " + array[1]; //pos 0 nombre, pos 1 puntos
				document.getElementById("muro").appendChild(div);
	
			}

			var sal = document.createElement('button');
			sal.textContent = "Salir";
			sal.id = "salirRanking";
			sal.addEventListener("click",function(){

				document.getElementById("muro").style.display="none";
				document.getElementById("muro").innerHTML='';
				document.getElementById("botonesRanking").removeChild(sal);
				game.context.clearRect(0,0,640,480);
				document.getElementById("partidas-container").style.display = 'inline-block';
				document.getElementById("serpiente").style.display = "block";
				document.getElementById("ranking").style.display = "inline-block";

			});

			document.getElementById("botonesRanking").appendChild(sal);

		
		});

	});
    
})

function partidas(){ //get APIRest de las partidas creadas hasta el momento

    $.ajax({

        method:"GET",
        url:"http://" + window.location.host + "/partidas",

    }).done(function(data){
		
		console.log(JSON.parse(data));
		borrarDiv('#partidas');

		var partidas = JSON.parse(data);
		for(var i = 0; i < partidas.length; i++){ //las mostramos

			crearDiv(partidas[i]); 

		}
    
    });

}

function borrarDiv(id){ //borramos un div introducido por parametro
    
    $(id).empty();
    
}

function crearDiv(info){ //creamos los divs de las partidas

	var newDiv = document.createElement("div"); 
	var d = info[2]==1?"fácil":info[2]==2?"normal":"dificil";
	newDiv.id = info[0];					//pos 0 nombreSala
	var newContent = document.createTextNode(info[0] + "     " + info[1] + "/4        " + d); //pos 1 numero de jugadores conectados a la sala, pos 2 dificultad
	newDiv.appendChild(newContent); //añade texto al div creado. 
	var boton = document.createElement("button");
	boton.type = "button";
	boton.textContent = "unirse";
	boton.style.alignSelf = "right";
	boton.id = "unirse-btn"
	boton.addEventListener("click", function(){	 //se une a la sala (o lo intenta)
		var part = {
            funcion: "unirGame",
            params:[info[0]]
        }

        game.socket.send(JSON.stringify(part));
	},false);
	salaP = info[0];
	newDiv.appendChild(boton);

	$('#partidas').append(newDiv);

}

game = new Game();

game.initialize();