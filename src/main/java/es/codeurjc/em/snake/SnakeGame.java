package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnakeGame {
    
    //////////////////////Puntos y comportamiento de serpientes se manejan en java y no en javascript/////////////////////////////
    
        public Random rnd = new Random(System.currentTimeMillis());
        private ObjectMapper mapper = new ObjectMapper();

	private final static long TICK_DELAY = 100;

	private ConcurrentHashMap<Integer, Snake> snakes; 
	private AtomicInteger numSnakes; //Hacer que sólo se juegue cuando hayan 2 jugadores o más
        
        private long difficulty = 1;
        private String creador;
        private boolean inGame;

	private ScheduledExecutorService scheduler;
        
        private Comida food;
        private Gson gson = new Gson();
        
        public SnakeGame(long dif, String c){
        
            snakes = new ConcurrentHashMap<>();
            numSnakes = new AtomicInteger();
            inGame = false;
            difficulty = dif;
            creador = c;
            
        }
	public void addSnake(Snake snake) {

                synchronized(snake){
                    snakes.put(snake.getId(), snake);
                }
                
                numSnakes.getAndIncrement();

	}

	public Collection<Snake> getSnakes() {
		return snakes.values();
	}

	public void removeSnake(Snake snake) {

            synchronized(snake){
		snakes.remove(Integer.valueOf(snake.getId()));
            }
                
		int count = numSnakes.decrementAndGet();

                
		if (count == 0) {
                        if (scheduler != null) {
                            scheduler.shutdown();
                        }
		}
	}

	private void tick() {

		try {

			for (Snake snake : getSnakes()) {
                            snake.update(getSnakes());
			}
                        
			StringBuilder sb = new StringBuilder();
			for (Snake snake : getSnakes()) {
				sb.append(getLocationsJson(snake));
				sb.append(',');
			}
			sb.deleteCharAt(sb.length()-1);
                        
                        
                        //Meter comportamiento de comida
                        
                        Snake come = food.update(getSnakes());
                        
                        if(come != null){
                            
                            come.setPuntos(10);
                            food = new Comida();
                            
                            //Mandarle puntos a esa serpiente
                            ObjectNode n = mapper.createObjectNode();
                            n.put("type","sumaPuntos");
                            n.put("id",come.getId());
                            n.put("puntos",come.getPuntos());
                            broadcast(n.toString());
                        }
                        
                        //Hacer un stringBuilder de comida
                        StringBuilder fb = new StringBuilder();
                        fb.append(food.getPosX());
                        fb.append(',');
                        fb.append(food.getPosY());
                        fb.append(',');
                        fb.append('\"');
                        fb.append(food.getColor());
                        fb.append('\"');
                        
                        //Modificar para mandar ambos
			String msg = String.format("{\"type\":\"update\",\"data\":[%s],\"food\":[%s]}", sb.toString(), fb.toString());

			broadcast(msg);

		} catch (Throwable ex) {
			System.err.println("Exception processing tick()");
			ex.printStackTrace(System.err);
		}
	}

	private String getLocationsJson(Snake snake) {

		synchronized (snake) {

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("{\"x\": %d, \"y\": %d}", snake.getHead().x, snake.getHead().y));
			for (Location location : snake.getTail()) {
				sb.append(",");
				sb.append(String.format("{\"x\": %d, \"y\": %d}", location.x, location.y));
			}

			return String.format("{\"id\":%d,\"body\":[%s]}", snake.getId(), sb.toString());
		}
	}

	public synchronized void broadcast(String message) throws Exception {

		for (Snake snake : getSnakes()) {
			try {
                            
                                System.out.println("Sending message " + message + " to " + snake.getId());
                                snake.sendMessage(message);

			} catch (Throwable ex) {
				System.err.println("Execption sending message to snake " + snake.getId());
				ex.printStackTrace(System.err);
				removeSnake(snake);
			}
		}
	}

	public void startTimer() {
            
            food = new Comida();
            
		scheduler = Executors.newScheduledThreadPool(2);
		scheduler.scheduleAtFixedRate(() -> tick(), TICK_DELAY/difficulty, TICK_DELAY/difficulty, TimeUnit.MILLISECONDS);   //La dificultad aumenta la velocidad
                scheduler.schedule(() -> stopTimer(), 1, TimeUnit.MINUTES);                         //Nuevo schedule para parar el juego tras 1 minuto
	}

	public void stopTimer() {
            
            try {
                if (scheduler != null) {
                    scheduler.shutdown();
                }
                
                //Avisa a los js que ha acabado la partida
                String[] aux = this.getMayorPuntuacion();
                
                SnakeHandler.addPunto(aux);
                
                this.setInGame(false);
                
                ObjectNode n = mapper.createObjectNode();
                n.put("type","finPartida");
                n.put("ganador", aux[0]);
                n.put("puntos", aux[1]);
                broadcast(n.toString());
                
            } catch (Exception ex) {
                Logger.getLogger(SnakeGame.class.getName()).log(Level.SEVERE, null, ex);
            }
            
	}
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        public synchronized int getNum(){
            return numSnakes.get();
        }
        
        public float getDif(){
            return difficulty;
        }

        public boolean isInGame() {
            return inGame;
        }

        public void setInGame(boolean inGame) {
            this.inGame = inGame;
        }

        public String getCreador() {
            return creador;
        }        
        
        public String[] getMayorPuntuacion(){           //Encontramos la mayor puntuación, y la almacenamos en SnakeHandler
            
            String[] aux = new String[2];
            int pts = 0;
            
            for(Snake s : this.getSnakes()){
                
                synchronized(s.getSession()){
                
                    if(s.getPuntos() > pts){
                        pts = s.getPuntos();

                        aux[0] = s.getName();
                        aux[1] = Integer.toString(pts);
                        
                    }
                    
                }
                
            }
            
            return aux;
            
        }

        
}
