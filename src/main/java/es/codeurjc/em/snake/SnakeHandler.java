package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SnakeHandler extends TextWebSocketHandler {

	private static final String SNAKE_ATT = "snake";
        private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger snakeIds = new AtomicInteger(0);  //Sirve para dar el id a las serpientes

        //Crear un ConcurrentHashMap <session, Snake>, así le podemos dar nombre a la serpiente desde el textHandler
        
        //Aquí hacemos un ConcurrentHashMap<string nombre, snakeGame>, que sean las salas
        private ConcurrentHashMap<WebSocketSession, Snake> sessions = new ConcurrentHashMap<>();
	private SnakeGame snakeGame = new SnakeGame();
        //private ConcurrentHashMap<String, SnakeGame> snakeGame;
        
        //Diccionario de funciones
        ConcurrentHashMap<String, Function> Funciones;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            //Creamos una serpiente, que ahora guardará el nombre. La sala ya se la daremos

		int id = snakeIds.getAndIncrement();

		Snake s = new Snake(id, session);

                sessions.put(session, s);
		session.getAttributes().put(SNAKE_ATT, s);

		snakeGame.addSnake(s);

		StringBuilder sb = new StringBuilder();
		for (Snake snake : snakeGame.getSnakes()) {			
			sb.append(String.format("{\"id\": %d, \"color\": \"%s\"}", snake.getId(), snake.getHexColor()));
			sb.append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		String msg = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
		
		snakeGame.broadcast(msg);
                
            
	}
    
    private void Chat(WebSocketSession session, TextMessage message) throws Exception{
        
            try{
                
                JsonNode mens = mapper.readTree(message.getPayload());
                ObjectNode difusion = mapper.createObjectNode();
                difusion.put("name",mens.get("name").asText());
                difusion.put("mensaje",mens.get("message").asText());
                difusion.put("type","chat");
                
                for(Entry<WebSocketSession, Snake> s : sessions.entrySet()){

                        s.getKey().sendMessage(new TextMessage(difusion.toString()));
                
                }
            }catch(IOException e){
        
                System.out.println("Error: " + e.getMessage());
        
            }
            
        }
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            //Hacer un diccionario para los distintos tipos de mensajes
            //Movs como ahora
            //Añadirte a una sala
            //Chat: verde jugando, rojo no jugando
            //etc

		try {

			String payload = message.getPayload();
                        if(payload.contains("chat")){
                        
                            Chat(session,message);
                        
                        }else{
                            
                            if (payload.equals("ping")) {
                                    return;
                            }

                            Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);

                            Direction d = Direction.valueOf(payload.toUpperCase());
                            s.setDirection(d);
                        }

		} catch (Exception e) {
			System.err.println("Exception processing message " + message.getPayload());
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

            
		System.out.println("Connection closed. Session " + session.getId());

		Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);

		snakeGame.removeSnake(s);

		String msg = String.format("{\"type\": \"leave\", \"id\": %d}", s.getId());
		
		snakeGame.broadcast(msg);
            
	}
        
        /*
        this.Funciones.put("FuncionBuscarPartida", new Function(){   
            public void ExecuteAction(String[] params,WebSocketSession session){
                for(Map.Entry<Integer, Partida> p: Partidas.entrySet()){
                    if(!p.getValue().Iniciada()){
                        try {
                            p.getValue().IniciarPartida(session,id-1);
                            return;
                        } catch (IOException ex) {
                            Logger.getLogger(WebSocketManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                
                }                                      
                Partidas.put(id,new Partida(session));
                id++;
                        
                
                    
            } 
        });
        */
        
        public ArrayList<String[]> getPartidas(){
            //Esto está mal, debería devolver el nombre de la partida, dificultad, numJugadores
            /*
            ArrayList<SnakeGame> aux = new ArrayList<>();
            
            for(SnakeGame s : snakeGame.values()){
                aux.add(s);
            }
            
            return aux;*/
            
            ArrayList<String[]> sol = new ArrayList<>();
            String[] aux = new String[3];
            
            /*for(String s : snakeGame.keySet()){
                aux[0] = s;
                aux[1] = snakeGame.get(s).getDif();
                aux[2] = "" + snakeGame.get(s).getNum();
                
                sol.add(aux);
                aux = new String[3];
            }*/
            
            return sol;
        }

}
