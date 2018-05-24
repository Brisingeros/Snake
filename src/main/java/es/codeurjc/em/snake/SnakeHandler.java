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
        private static final String SALA_ATT = "sala";
        private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger snakeIds = new AtomicInteger(0);  //Sirve para dar el id a las serpientes
        private Gson gson = new Gson();
        //Crear un ConcurrentHashMap <session, Snake>, así le podemos dar nombre a la serpiente desde el textHandler
        
        //Aquí hacemos un ConcurrentHashMap<string nombre, snakeGame>, que sean las salas
        private ConcurrentHashMap<String, SnakeGame> salas;
        private ConcurrentHashMap<Key, Snake> sessions;
        
        //Diccionario de funciones
        private ConcurrentHashMap<String, Function> Funciones;
        
        public SnakeHandler(){
            this.Funciones = new ConcurrentHashMap<>();
            this.sessions = new ConcurrentHashMap<>();
            this.salas = new ConcurrentHashMap<>();
            
            
            this.Funciones.put("Chat", new Function(){
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Params: nombre, mensaje
                    try{
                        
                        ObjectNode difusion = mapper.createObjectNode();
                        
                        Snake sn = (Snake) session.getAttributes().get(SNAKE_ATT);
                        
                        difusion.put("name",params[0]);
                        difusion.put("enPartida",sn.isInGame());
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
            this.Funciones.put("salirSala",new Function(){ //salir de la sala: eliminamos de la sala el jugador. Si hay 0 jugadores al final, se elimina la sala
                                                            //Params: //////////////NombreUsuario, /////////////si está en partida, nombreSala
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    
                    Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);
                    s.setInGame(false);
                    String nombreSala = (String) session.getAttributes().get(SALA_ATT);
                    
                    SnakeGame sala = salas.get(nombreSala);
                    sala.removeSnake(s);

                    session.getAttributes().replace(SALA_ATT, "none");

                    s.resetState();
                    
                    if(sala.getNum() == 0 || (sala.getNum() == 1 && sala.isInGame())){
                        salas.remove(nombreSala);
                        ObjectNode n = mapper.createObjectNode();
                        n.put("type","quitarSala");
                        n.put("sala",nombreSala);
                        for(Snake sk : sessions.values()){

                            try {
                                sk.getSession().sendMessage(new TextMessage(n.toString()));
                            } catch (IOException ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }
                    } else if(!sala.isInGame()){

                            ArrayList<String> jugadores = new ArrayList<>();
                            for(Snake a : sala.getSnakes()){

                                jugadores.add(a.getName());
                            }
                            ObjectNode difusion = mapper.createObjectNode();
                            String players = gson.toJson(jugadores);
                            difusion.put("players",players);
                            difusion.put("sala",nombreSala);
                            difusion.put("type","sala");

                            for(Snake a : sala.getSnakes()){

                                try {
                                    a.getSession().sendMessage(new TextMessage(difusion.toString()));
                                } catch (IOException ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            }

                    }

                    if(sala.getNum() > 0){
                        String msg = String.format("{\"type\": \"leave\", \"id\": %d}", s.getId());

                        for(Snake sk : sala.getSnakes()){

                            try {
                                sk.getSession().sendMessage(new TextMessage(msg));
                            } catch (IOException ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }
                    }

                }
            
            });
            this.Funciones.put("unirGame", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: ID snakeGame, //////////////param[1]: nombre player

                    SnakeGame sala = salas.get(params[0]);
                    Snake ss = (Snake) session.getAttributes().get(SNAKE_ATT);
                    //Key newKey = new Key(ss.getName(), session.toString());
                    //Snake player = sessions.get(newKey);
                    ss.setInGame(true);
                    //System.out.println("jugador: " + params[1]);
                    sala.addSnake(ss);
                    
                    session.getAttributes().replace(SALA_ATT, params[0]);
                    
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
                        
                        StringBuilder sb = new StringBuilder();
                        for (Snake snake : sala.getSnakes()) {
                            sb.append(String.format("{\"id\": %d, \"color\": \"%s\"}", snake.getId(), snake.getHexColor()));
                            sb.append(',');
                        }
                        sb.deleteCharAt(sb.length()-1);
                        String msg = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
                        sala.broadcast(msg);
                            
                    } catch (IOException ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
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
                        Key newKey = new Key(params[0], session.toString());

                        sessions.put(newKey, s);
                        session.getAttributes().put(SNAKE_ATT, s);
                        
                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }            
            
            });
            
            this.Funciones.put("comenzarPartida",new Function(){
                
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    
                    SnakeGame sala = salas.get(params[0]);
                    sala.setInGame(true);
                    ObjectNode n = mapper.createObjectNode();
                    n.put("type","jugar");
                    try{
                        for(Snake s : sala.getSnakes()){

                            s.getSession().sendMessage(new TextMessage(n.toString()));

                        }
                    } catch (IOException ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
            });
            
            this.Funciones.put("matchMaking",new Function(){
                
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {  //Params: ///////////nombrePlayer
                    
                    String[] para = {"none"};
                    
                    for(String s : salas.keySet()){
                        
                        SnakeGame sg = salas.get(s);
                        
                        if(sg.getNum() > 0 && sg.getNum() < 4){
                            para[0] = s;
                            
                            Funciones.get("unirGame").ExecuteAction(para, session);
                            
                            return;
                        }
                        
                    }
                    
                    if(para[0].equals("none")){
                        try {
                            
                            ObjectNode difusion = mapper.createObjectNode();
                            difusion.put("type","senal");
                            difusion.put("contenido", "No pudo encontrarse una partida que se ajuste a las características. Crea una propia");
                            
                            session.sendMessage(new TextMessage(difusion.toString()));
                            
                        } catch (IOException ex) {
                            Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                }
                
            });
        }

	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            
            session.getAttributes().put(SALA_ATT, "none");
            
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

                //Cogemos ambos attribs
		String s = (String) session.getAttributes().get(SALA_ATT);
                Snake snek = (Snake) session.getAttributes().get(SNAKE_ATT);
                
                //Quitamos la serpiente de la sala y mandamos mensaje
                if(!s.equals("none")){
                    salas.get(s).removeSnake(snek);
                    String msg = String.format("{\"type\": \"leave\", \"id\": %d}", snek.getId());

                    for(Snake sk : salas.get(s).getSnakes()){

                            sk.getSession().sendMessage(new TextMessage(msg));

                    }
                }
                
                //Quitamos la serpiente de sesiones
                Key newKey = new Key("placeholder", session.toString());
                sessions.remove(newKey);

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
