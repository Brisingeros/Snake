package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SnakeHandler extends TextWebSocketHandler {

	private static final String SNAKE_ATT = "snake";
        private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger snakeIds = new AtomicInteger(0);  //Sirve para dar el id a las serpientes
        private Gson gson = new Gson();
        //Crear un ConcurrentHashMap <session, Snake>, así le podemos dar nombre a la serpiente desde el textHandler
        
        //Aquí hacemos un ConcurrentHashMap<string nombre, snakeGame>, que sean las salas
        private ConcurrentHashMap<String, SnakeGame> salas;
        private ConcurrentHashMap<String, Snake> sessions;
	//private SnakeGame snakeGame = new SnakeGame();
        //private ConcurrentHashMap<String, SnakeGame> snakeGame;
        
        //Diccionario de funciones
        private ConcurrentHashMap<String, Function> Funciones;
        
        public SnakeHandler(){
            this.Funciones = new ConcurrentHashMap<>();
            this.sessions = new ConcurrentHashMap<>();
            this.salas = new ConcurrentHashMap<>();
            
            
            this.Funciones.put("Chat", new Function(){
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    try{
                        
                        ObjectNode difusion = mapper.createObjectNode();
                        difusion.put("name",params[0]);
                        difusion.put("mensaje",params[1]);
                        difusion.put("type","chat");

                        for(Snake s : sessions.values()){

                            s.getSession().sendMessage(new TextMessage(difusion.toString()));
                            
                        }
                        
                    }catch(IOException e){

                        System.out.println("Error: " + e.getMessage());

                    }
                }
            });
            this.Funciones.put("ping", new Function(){
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    return;
                }
            });
            
            this.Funciones.put("direccion", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);

                    Direction d = Direction.valueOf(params[0].toUpperCase());
                    s.setDirection(d);
                    
                }            
            
            });
            
            this.Funciones.put("unirGame", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: ID snakeGame, param[1]: nombre player

                    SnakeGame sala = salas.get(params[0]);
                    Snake player = sessions.get(params[1]);
                    System.out.println("jugador: " + params[1]);
                    sala.addSnake(player);
                    
                    try{
                        
                        ArrayList<String> jugadores = new ArrayList<>();
                        for(Snake s : sala.getSnakes()){
                        
                            jugadores.add(s.getName());
                        }
                        ObjectNode difusion = mapper.createObjectNode();
                        String players = gson.toJson(jugadores);
                        difusion.put("players",players);
                        difusion.put("sala",params[0]);
                        difusion.put("type","sala");

                        for(Snake s : sala.getSnakes()){

                            s.getSession().sendMessage(new TextMessage(difusion.toString()));

                        }
                            
                    } catch (IOException ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }            
            
            });
            
            this.Funciones.put("crearSerpiente", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: nombre player
                    
                    try {

                        int id = snakeIds.getAndIncrement();

                        Snake s = new Snake(id, params[0], session);

                        sessions.put(params[0], s);
                        session.getAttributes().put(SNAKE_ATT, s);

                        /*//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        snakeGame.addSnake(s);

                        StringBuilder sb = new StringBuilder();
                        for (Snake snake : snakeGame.getSnakes()) {
                            sb.append(String.format("{\"id\": %d, \"color\": \"%s\"}", snake.getId(), snake.getHexColor()));
                            sb.append(',');
                        }
                        sb.deleteCharAt(sb.length()-1);
                        String msg = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());

                        snakeGame.broadcast(msg);
                        */
                        
                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }            
            
            });
        }

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            
            
            
	}
        
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            //Hacer un diccionario para los distintos tipos de mensajes
            //Movs como ahora
            //Añadirte a una sala
            //Chat: verde jugando, rojo no jugando
            //etc
            
            try{
            
                String msg = message.getPayload();

                Instruccion i = gson.fromJson(msg, Instruccion.class);//mapper.readValue(msg, Instruccion.class);
                Function f = Funciones.get(i.getFuncion());
                System.out.println(i.getFuncion() + " " + i.getParams());
                f.ExecuteAction(i.getParams(), session);
            
            }catch (Exception e) {
                System.err.println("Exception processing message " + message.getPayload());
                e.printStackTrace(System.err);
            }
            
            
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

            
		System.out.println("Connection closed. Session " + session.getId());

		Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);

                /*/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		snakeGame.removeSnake(s);

		String msg = String.format("{\"type\": \"leave\", \"id\": %d}", s.getId());
		
		snakeGame.broadcast(msg);*/
            
	}
        
        public ArrayList<String> getNombrePartidas(){
            
            ArrayList<String> sol = new ArrayList<>();
            for(String s : this.salas.keySet()){
                sol.add(s);
            }
            
            return sol;

        }
        
        public void addGame(String name){

            SnakeGame p = new SnakeGame();
            this.salas.put(name, p);

        }

}
