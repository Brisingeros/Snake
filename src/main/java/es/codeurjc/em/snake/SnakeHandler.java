package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SnakeHandler extends TextWebSocketHandler {
    
        private static final Executor executor = Executors.newCachedThreadPool();

	private static final String SNAKE_ATT = "snake";
        private static final String SALA_ATT = "sala";
        
        
        private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger snakeIds = new AtomicInteger(0);  //Sirve para dar el id a las serpientes
        private Gson gson = new Gson();

        
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

                            synchronized(s){
                                s.sendMessage(difusion.toString());
                            }
                            
                        }
                        
                    }catch(IOException e){

                        System.out.println("Error: " + e.getMessage());

                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
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
                    
                    Snake snek = (Snake) session.getAttributes().get(SNAKE_ATT);
                    String nombreSala = (String) session.getAttributes().get(SALA_ATT);
                    
                    SnakeGame sala = salas.get(nombreSala);
                    
                    snek.setInGame(false);
                    
                    //Hasta aquí, la sala existe
                    
                    sala.removeSnake(snek);
                    
                    int num = sala.getNum();
                    boolean jugando = sala.isInGame();
                    
                    if(jugando){
                    
                        if(num <= 1){
                            
                            Snake aux = (Snake) sala.getSnakes().toArray()[0];
                            
                            sala.setInGame(false);
                            
                            try {
                                ObjectNode difusion = mapper.createObjectNode();
                                difusion.put("type","finJuego");
                                difusion.put("contenido", "Error en partida. Victoria por retirada");

                                synchronized(aux){
                                    aux.sendMessage(difusion.toString());
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (Exception ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            
                        } else{
                            
                            String msg = String.format("{\"type\": \"leave\", \"id\": %d}", snek.getId());

                            for(Snake sk : sala.getSnakes()){
                                try {
                                    synchronized(sk){
                                        sk.sendMessage(msg);
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (Exception ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        
                        }
                        
                    } else{
                        
                        if(num == 0){
                            
                            salas.remove(nombreSala);
                            ObjectNode n = mapper.createObjectNode();
                            n.put("type","quitarSala");
                            n.put("sala",nombreSala);
                            
                            for(Snake sk : sessions.values()){
                                    synchronized(sk){
                                        try {
                                            sk.sendMessage(n.toString());
                                        } catch (Exception ex) {
                                            Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                            }
                            
                        }
                        
                    }

                }
            
            });
            this.Funciones.put("unirGame", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: ID snakeGame, //////////////param[1]: nombre player
                    
                    boolean entra = false;
                    Semaphore sem = new Semaphore(0);
                    
                    while(!entra){
                        
                        SnakeGame sala = salas.get(params[0]);
                            
                            if(sala.getNum() < 4){
                                try {
                                    entra = true;

                                    Snake ss = (Snake) session.getAttributes().get(SNAKE_ATT);
                                    ss.setInGame(true);
                                    sala.addSnake(ss);

                                    session.getAttributes().replace(SALA_ATT, params[0]);

                                    ArrayList<String> jugadores = new ArrayList<>();
                                    for(Snake s : sala.getSnakes()){

                                        jugadores.add(s.getName());
                                    }
                                    ObjectNode difusion = mapper.createObjectNode();
                                    String players = gson.toJson(jugadores);
                                    difusion.put("players",players);
                                    difusion.put("sala",params[0]);
                                    difusion.put("type","sala");

                                    sala.broadcast(difusion.toString());
                                    
                                    if(sala.isInGame()){
                                        ObjectNode n = mapper.createObjectNode();
                                        n.put("type","jugar");
                                        session.sendMessage(new TextMessage(n.toString()));
                                    }

                                    StringBuilder sb = new StringBuilder();
                                    for (Snake snake : sala.getSnakes()) {
                                        sb.append(String.format("{\"id\": %d, \"color\": \"%s\",\"name\":\"%s\",\"puntos\": %d}", snake.getId(), snake.getHexColor(),snake.getName(), snake.getPuntos()));
                                        sb.append(',');
                                    }
                                    sb.deleteCharAt(sb.length()-1);
                                    String msg = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
                                    sala.broadcast(msg);
                                    
                                    if(sala.getNum() == 4){
                                        
                                        ObjectNode n = mapper.createObjectNode();
                                        n.put("type","jugar");
                                        sala.broadcast(n.toString());
                                        
                                    }
                                    
                                    
                                    
                                } catch (Exception ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            } else{
                                
                                try {
                                    if(sem.tryAcquire(5, 1000, TimeUnit.MILLISECONDS)){
                                        
                                        ObjectNode difusion = mapper.createObjectNode();
                                        difusion.put("type","senal");
                                        difusion.put("contenido", "No pudo encontrarse una partida que se ajuste a las características. Crea una propia");

                                        session.sendMessage(new TextMessage(difusion.toString()));

                                        return;
                                        
                                    } else{
                                    
                                        sem.release();
                                        
                                    }
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            }
                        
                    }
                    
                    

                }            
            
            });
            
            this.Funciones.put("crearSerpiente", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: nombre player

                        int id = snakeIds.getAndIncrement();

                        Snake s = new Snake(id, params[0], session);
                        Key newKey = new Key(params[0], session.toString());

                        if(sessions.containsKey(newKey)){
                        
                            try {
                                //Mensaje acá pidiendo otro nombre
                                ObjectNode difusion = mapper.createObjectNode();
                                difusion.put("type","falloNombre");
                                
                                session.sendMessage(new TextMessage(difusion.toString()));
                                
                            } catch (IOException ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            } finally{
                                return;
                            }
                            
                        } else{
                        
                            sessions.put(newKey, s);
                            session.getAttributes().put(SNAKE_ATT, s);
                            
                        }
                    
                }            
            
            });
            
            this.Funciones.put("comenzarPartida",new Function(){
                
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    
                    try {
                        SnakeGame sala = salas.get(params[0]);
                        sala.setInGame(true);
                        ObjectNode n = mapper.createObjectNode();
                        n.put("type","jugar");
                        
                        sala.broadcast(n.toString());
                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            
            this.Funciones.put("matchMaking",new Function(){
                
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {  //Params: longDif       ///////////nombrePlayer
                    
                    String[] para = {"none"};
                    long lg = Long.parseLong(params[0]);
                    
                    for(String s : salas.keySet()){
                        
                        SnakeGame sg = salas.get(s);
                        
                        if(sg.getNum() > 0 && sg.getNum() < 4 && sg.getDif() == lg){
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
                
                Runnable tarea = () -> f.ExecuteAction(i.getParams(), session);
                executor.execute(tarea);
            
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

                    salas.get(s).broadcast(msg);
                    
                }
                
                //Quitamos la serpiente de sesiones
                Key newKey = new Key("placeholder", session.toString());
                sessions.remove(newKey);
            
	}
        
        public ArrayList<String[]> getNombrePartidas(){
            
            ArrayList<String[]> sol = new ArrayList<>();
            String[] aux = new String[3];
            for(String s : this.salas.keySet()){
                aux[0] = s;
                aux[1] = String.valueOf(salas.get(s).getNum());
                aux[2] = String.valueOf(salas.get(s).getDif());
                
                sol.add(aux);
            }
            
            return sol;

        }
        
        public void addGame(String name, long dif){

            SnakeGame p = new SnakeGame(dif);
            this.salas.put(name, p);

        }

}
