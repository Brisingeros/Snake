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
    
        public Random rnd = new Random(System.currentTimeMillis());
        private ObjectMapper mapper = new ObjectMapper();

	private final static long TICK_DELAY = 100;

	private ConcurrentHashMap<Integer, Snake> snakes; 
	private AtomicInteger numSnakes; //Hacer que sólo se juegue cuando hayan 2 jugadores o más
        //Si sólo hay un jugador, joder la partida
        
        //Dificultad y modo tiene que ir aquí.
        //Dificultad = 1 fácil, 2 medio, 4 difícil
        private long difficulty = 1;
        
        private boolean inGame;

	private ScheduledExecutorService scheduler;
        
        private Comida food;
        private Gson gson = new Gson();
        
        public SnakeGame(long dif){
        
            snakes = new ConcurrentHashMap<>();
            numSnakes = new AtomicInteger();
            inGame = false;
            difficulty = dif;
            
        }
	public void addSnake(Snake snake) {

		snakes.put(snake.getId(), snake);

		int count = numSnakes.getAndIncrement();

		if (count == 0) {
			startTimer();
		}
	}

	public Collection<Snake> getSnakes() {
		return snakes.values();
	}

	public void removeSnake(Snake snake) {

		snakes.remove(Integer.valueOf(snake.getId()));

		int count = numSnakes.decrementAndGet();

		if (count == 0) {
			stopTimer();
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
                            food = new Comida();
                            
                            //Mandarle puntos a esa serpiente
                            ObjectNode n = mapper.createObjectNode();
                            n.put("type","sumaPuntos");
                            n.put("id",come.getId());
                            n.put("puntos",10);
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

	public void broadcast(String message) throws Exception {

		for (Snake snake : getSnakes()) {
			try {

                                synchronized(snake){
                                    System.out.println("Sending message " + message + " to " + snake.getId());
                                    snake.sendMessage(message);
                                }

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
		scheduler.scheduleAtFixedRate(() -> tick(), TICK_DELAY/difficulty, TICK_DELAY/difficulty, TimeUnit.MILLISECONDS);
                scheduler.schedule(() -> stopTimer(), 3, TimeUnit.MINUTES);
	}

	public void stopTimer() {
            
            try {
                if (scheduler != null) {
                    scheduler.shutdown();
                }
                
                //Avisa a los js que ha acabado la partida
                ObjectNode n = mapper.createObjectNode();
                n.put("type","finPartida");
                broadcast(n.toString());
                
            } catch (Exception ex) {
                Logger.getLogger(SnakeGame.class.getName()).log(Level.SEVERE, null, ex);
            }
            
	}
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        public int getNum(){
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
        
        
}
