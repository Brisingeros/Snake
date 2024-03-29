package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
    
    private static CopyOnWriteArrayList<String[]> puntuaciones = new CopyOnWriteArrayList<>();
    
        private static final Executor executor = Executors.newCachedThreadPool();

	private static final String SNAKE_ATT = "snake";
        private static final String SALA_ATT = "sala";
        
        
        private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger snakeIds = new AtomicInteger(0);  //Sirve para dar el id a las serpientes
        private Gson gson = new Gson();

        
        private ConcurrentHashMap<String, SnakeGame> salas;     //idSala y SnakeGame
        private ConcurrentHashMap<String, Snake> sessions;      //idSnake y serpiente
        
        //Diccionario de funciones
        private ConcurrentHashMap<String, Function> Funciones;
        
        public SnakeHandler(){
            this.Funciones = new ConcurrentHashMap<>();
            this.sessions = new ConcurrentHashMap<>();
            this.salas = new ConcurrentHashMap<>();
            
            this.Funciones.putIfAbsent("Chat", new Function(){ //Funcion que maneja el chat
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Params: nombre, mensaje
                    try{
                        
                        ObjectNode difusion = mapper.createObjectNode();
                        
                        Snake sn;
                        
                        
                        sn = (Snake) session.getAttributes().get(SNAKE_ATT);
                        
                        difusion.put("name",params[0]);
                        difusion.put("enPartida",sn.isInGame());
                        difusion.put("mensaje",params[1]);
                        difusion.put("type","chat");

                        for(Snake s : sessions.values()){ //Enviamos el mensaje a cada una de las serpientes en sessions

                            s.sendMessage(difusion.toString());
                            
                        }
                        
                    }catch(IOException e){

                        System.out.println("Error: " + e.getMessage());

                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            this.Funciones.put("ping", new Function(){                      //Funcion de ping comprobando que sigue habiendo conexión
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    return;
                }
            });
            
            this.Funciones.put("direccion", new Function(){                 //Función que maneja el cambio de dirección en el frontend
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    
                    Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);
                    
                    synchronized(s){
                        Direction d = Direction.valueOf(params[0].toUpperCase());
                        
                        s.setDirection(d);

                    }
                    
                }            
            
            });
            this.Funciones.put("salirSala",new Function(){ //salir de la sala: eliminamos de la sala el jugador. Si hay 0 jugadores al final, se elimina la sala. si se está jugando y queda 1, se elimina la sala
                                                            //Params:
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {
                    
                    Snake snek;
                    String nombreSala;
                    SnakeGame sala;
                    int num;
                    

                    snek = (Snake) session.getAttributes().get(SNAKE_ATT);
                    nombreSala = (String) session.getAttributes().get(SALA_ATT);
                    
                    sala = salas.get(nombreSala);
                    
                    
                    synchronized(snek){
                    
                        session.getAttributes().replace(SALA_ATT, "none");

                        snek.setInGame(false);
                        snek.resetState();
                    }
                    
                    
                    
                    synchronized(sala){
                        num = sala.getNum();
                        sala.removeSnake(snek);//Serpiente se va
                    }
                    
                    
                    //Mensaje vaciar sala serpiente que se va//
                    String vacio;
                    for(Snake snS : sala.getSnakes()){
                            try {
                                vacio = String.format("{\"type\": \"leave\", \"id\": %d}", snS.getId());

                                snek.sendMessage(vacio);
                                

                            }
                            catch (Exception ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                    
                    }
                    //---//
                    
                    //Mensaje al resto de serpientes//
                    String msg = String.format("{\"type\": \"leave\", \"id\": %d}", snek.getId());
                            
                    //System.out.println("//////////////////////////////////////////////////////////////Sale: " + snek.getId());
                    
                    try {
                        sala.broadcast(msg);
                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //---//
                    boolean jugando;
                    synchronized(sala){

                        jugando = sala.isInGame();
                    
                    
                        if(jugando){

                            if(num == 2){                                                                   //Comprobar si quedan dos jugadores, para al salir indicar al otro que debe salir y que gana por retirada
                                sala.setInGame(false);
                                Snake aux = (Snake) sala.getSnakes().toArray()[0];

                                try {
                                    ObjectNode difusion = mapper.createObjectNode();
                                    difusion.put("type","finJuego");
                                    difusion.put("contenido", "Error en partida. Victoria por retirada");

                                    aux.sendMessage(difusion.toString());
                                } catch (IOException ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (Exception ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }

                        } else{

                            if(num == 1){                                                                   //Comprobar si era el último para eliminar la sala
                                //Quitamos la sala de todos los divs JS//
                                ObjectNode n = mapper.createObjectNode();
                                n.put("type","quitarSala");
                                n.put("sala",nombreSala);

                                for(Snake sk : sessions.values()){
                                            try {
                                                sk.sendMessage(n.toString());
                                            } catch (Exception ex) {
                                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        
                                }
                                //---//

                                salas.remove(nombreSala, sala);
                            }
                        }
                        //}
                    }
                }
            
            });
            this.Funciones.put("unirGame", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: ID snakeGame
                    
                    boolean entra = false;
                    Semaphore sem = new Semaphore(0);   //Semáforo usado para controlar el tiempo e intentos de entrar en una sala ya ocupada
                    boolean espera = true;
                    
                    SnakeGame sala;
                    
                    sala = salas.get(params[0]);
                    
                    Snake ss = (Snake) session.getAttributes().get(SNAKE_ATT);
                    
                    if(!ss.isEnEspera())
                        ss.setEnEspera(true);
                    
                    while(!entra && ss.isEnEspera() && sala!=null){
                        
                            
                            System.out.println("NUMERO DE JUGADORES: " + sala.getNum());
                            
                            synchronized(sala){
                            
                            if(sala.getNum() < 4){
                                
                                try {
                                    entra = true;
                                    
                                    synchronized(ss){

                                        ss.setEnEspera(false);
                                        ss.setInGame(true);
                                        
                                    }
                                    

                                    session.getAttributes().replace(SALA_ATT, params[0]);

                                    
                                    //System.out.println("//////////////////////////////////////LA SALA ES: " + params[0]);

                                        sala.addSnake(ss);
                                        
                                        ObjectNode finEsp = mapper.createObjectNode();
                                        finEsp.put("type","finEspera");
                                        ss.sendMessage(finEsp.toString());
                                        

                                        ArrayList<String> jugadores = new ArrayList<>();
                                        for(Snake s : sala.getSnakes()){
                                            jugadores.add(s.getName());
                                        }
                                        ObjectNode difusion = mapper.createObjectNode();
                                        String players = gson.toJson(jugadores);
                                        difusion.put("players",players);
                                        difusion.put("sala",params[0]);
                                        difusion.put("type","sala");
                                        difusion.put("creador", sala.getCreador());

                                        sala.broadcast(difusion.toString());

                                        if(sala.isInGame()){
                                            ObjectNode n = mapper.createObjectNode();
                                            n.put("type","jugar");

                                            ss.sendMessage(n.toString());

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
                                        
                                        Funciones.get("comenzarPartida").ExecuteAction(params, session);
                                        
                                    }
                                    
                                    
                                    
                                } catch (Exception ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            } else{
                                
                                try {

                                        synchronized(ss){
                                            if(espera && ss.isEnEspera()){

                                                ObjectNode n = mapper.createObjectNode();
                                                n.put("type","esperaEntrar");
                                                espera = false;
                                                ss.sendMessage(n.toString());

                                            }
                                        
                                    
                                            if(ss.isEnEspera() && sem.tryAcquire(5, 1000, TimeUnit.MILLISECONDS)){

                                                ObjectNode difusion = mapper.createObjectNode();
                                                difusion.put("type","senal");
                                                difusion.put("contenido", "No pudo encontrarse una partida que se ajuste a las características. Crea una propia");



                                                synchronized(ss){
                                                    ss.setEnEspera(false);
                                                }

                                                ss.sendMessage(difusion.toString());

                                                return;

                                            } else{

                                                sem.release();
                                                //System.out.println("////////////////////////////////////////Intenta entrar");

                                            }
                                        }
                                        
                                } catch (InterruptedException | IOException ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (Exception ex) {
                                    Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            
                            }
                        }
                    }
                }            
            
            });
            this.Funciones.putIfAbsent("cancelarEspera", new Function(){ //Params: 
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {              //Método para indicar a la serpiente esperando que ya no debe esperar, se ha cancelado su espera desde JS
                                        
                        Snake sn = (Snake) session.getAttributes().get(SNAKE_ATT);
                        synchronized(sn){
                            sn.setEnEspera(false);
                        }

                    
                }
            
            });
            this.Funciones.putIfAbsent("crearSerpiente", new Function(){
            
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param 0: nombre player

                        int id = snakeIds.getAndIncrement();

                        Snake s = new Snake(id, params[0], session);

                        if(sessions.containsKey(params[0])){                        //En caso de que el nombre ya exista, pedir otro
                        
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
                            
                        } else{                                                     //Si no existe, crear la serpiente
                        
                            sessions.putIfAbsent(params[0], s);
                            session.getAttributes().putIfAbsent(SNAKE_ATT, s);
                            
                            //Mensaje conexión
                            ObjectNode difusion = mapper.createObjectNode();
                            ArrayList<String> jugadores = new ArrayList<>();
                            for(Snake snk : sessions.values()){
                            
                                jugadores.add(snk.getName());

                            
                            }
                            difusion.put("type","jugadorConecta");
                            difusion.put("names", gson.toJson(jugadores));
                            
                            for(Snake snk : sessions.values()){                     //Enviar mensaje para actualizar la lista de jugadores conectados
                                    try {
                                        snk.sendMessage(difusion.toString());
                                    } catch (Exception ex) {
                                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                            }
                            
                        }
                    
                }            
            
            });
            
            this.Funciones.putIfAbsent("comenzarPartida",new Function(){
                
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) { //Param: NombrePartida
                    
                    try {
                        
                        SnakeGame sala;
                        
                        sala = salas.get(params[0]);

                        
                        synchronized(sala){
                            
                            if(sala != null && !sala.isInGame()){
                        
                                sala.setInGame(true);
                                sala.startTimer();
                                ObjectNode n = mapper.createObjectNode();
                                n.put("type","jugar");
                                
                                sala.broadcast(n.toString());                       //Indicamos a todas las serpientes de la sala que comience el juego
                                
                            }
                        }
                           
                    } catch (Exception ex) {
                        Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            
            this.Funciones.putIfAbsent("matchMaking",new Function(){
                
                @Override
                public void ExecuteAction(String[] params, WebSocketSession session) {  //Params: longDif
                    
                    String[] para = {"none"};
                    long lg = Long.parseLong(params[0]);
                    
                    for(String s : salas.keySet()){
                        
                        SnakeGame sg;
                        

                        sg = salas.get(s);

                        
                        synchronized(sg){
                        
                            if(sg != null && sg.getNum() > 0 && sg.getNum() < 4 && sg.getDif() == lg){
                                para[0] = s;

                                Funciones.get("unirGame").ExecuteAction(para, session);                         //Una vez se ha selecionado una sala válida, seguimos con "unirSala"

                                return;
                            }
                            
                        }
                        
                    }
                    
                    if(para[0].equals("none")){                                                                 //En caso de no existir una sala válida, se lo indicamos al usuario
                        try {
                            
                            ObjectNode difusion = mapper.createObjectNode();
                            difusion.put("type","senal");
                            difusion.put("contenido", "No pudo encontrarse una partida que se ajuste a las características. Crea una propia");
                            
                            Snake snek = (Snake) session.getAttributes().get(SNAKE_ATT);
                            
                            snek.sendMessage(difusion.toString());
                            
                        } catch (IOException ex) {
                            Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                }
                
            });
        }

	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            
            session.getAttributes().putIfAbsent(SALA_ATT, "none");
            
	}
        
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            
            try{
            
                String msg = message.getPayload();

                Instruccion i = gson.fromJson(msg, Instruccion.class);
                Function f = Funciones.get(i.getFuncion());
                System.out.println(i.getFuncion() + " " + i.getParams());
                
                Runnable tarea = () -> f.ExecuteAction(i.getParams(), session);                         //Cada tarea se ejecuta en un hilo
                executor.execute(tarea);
            
            }catch (Exception e) {
                System.err.println("Exception processing message " + message.getPayload());
                e.printStackTrace(System.err);
            }
            
            
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

            
		System.out.println("Connection closed. Session " + session.getId());

                String s;
                Snake snek;
                String name;
                
                //Cogemos ambos attribs
                    s = (String) session.getAttributes().get(SALA_ATT);
                    snek = (Snake) session.getAttributes().get(SNAKE_ATT);
                    
                    
                synchronized(sessions.values()){
                
                if(snek != null){
                    
                    name = snek.getName();
                    //Quitamos la serpiente de la sala y mandamos mensaje
                    
                    if(!s.equals("none")){
                        
                        String[] vac = null;
                        this.Funciones.get("salirSala").ExecuteAction(vac, session);

                    }


                    //Mensaje desconexión
                    ObjectNode difusion = mapper.createObjectNode();
                    difusion.put("type","jugadorDesconecta");
                    difusion.put("name", name);

                    //Quitamos la serpiente de sesiones
                    sessions.remove(snek.getName(), snek);

                    //Mandamos mensaje, ya sincronizado en sendmessage
                    for(Snake snk : sessions.values()){
                            try {
                                snk.sendMessage(difusion.toString());
                            } catch (Exception ex) {
                                Logger.getLogger(SnakeHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                    }

                }
                }
                
                //System.out.println("//////////////////////////////////////////////////////////////////////////////////Sesioneeeeees: " + sessions.size());
                
	}
        
        public ArrayList<String[]> getNombrePartidas(){
            
            ArrayList<String[]> sol = new ArrayList<>();
            String[] aux;
            for(String s : this.salas.keySet()){
                
                SnakeGame sala = salas.get(s);
                
                aux = new String[3];
                aux[0] = s;
                
                synchronized(sala){
                
                    aux[1] = String.valueOf(salas.get(s).getNum());
                    aux[2] = String.valueOf(salas.get(s).getDif());
                
                }
                
                sol.add(aux);
                
            }
            
            return sol;

        }
        
        public void addGame(String name, long dif, String creador){

            SnakeGame p = new SnakeGame(dif, creador);
            this.salas.putIfAbsent(name, p);

        }
        
        public static CopyOnWriteArrayList<String[]> getMuro(){         //APIRest obtiene los puntos desde aquí
            return puntuaciones;
        }
        
        public static void addPunto(String[] nP){                       //Añade los puntos al ranking y controla su tamaño y orden
            
            if(colRanking(nP)){
            
                Collections.sort(puntuaciones, new Comparator(){

                    @Override
                    public int compare(Object o1, Object o2) {
                        String[] aux = (String[])o1;
                        int p1 = Integer.parseInt(aux[1]);

                        aux = (String[])o2;
                        int p2 = Integer.parseInt(aux[1]);

                        return (p1 > p2) ? -1:1;
                    }

                });

                CopyOnWriteArrayList<String[]> aux = new CopyOnWriteArrayList<>();
                int tamaño = (10>=puntuaciones.size()) ? puntuaciones.size():10;

                for(int i = 0; i < tamaño; i++){
                    aux.add(puntuaciones.get(i));
                }

                puntuaciones = aux;
                
            }
            
        }
        
        
        public static boolean colRanking(String[] nP){              //Método que inserta las puntuaciones, y evita que se repitan nombres
            
            if(puntuaciones.isEmpty()){
                
                puntuaciones.add(nP);

            } else{
            
                for(String[] st : puntuaciones){
            
                    if(st[0].equals(nP[0])){

                        if(Integer.parseInt(st[1]) < Integer.parseInt(nP[1])){
                            puntuaciones.remove(st);
                            puntuaciones.add(nP);
                            return true;
                        } else{
                            return false;
                        }

                    }
            
                }
                
            }
            
             return false;

        }

}