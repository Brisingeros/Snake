package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Snake {

	private static final int DEFAULT_LENGTH = 5;

        private boolean enEspera;
	private final int id;
        private ObjectMapper mapper = new ObjectMapper();
        private int puntos;
	private Location head;
	private final Deque<Location> tail = new ArrayDeque<>();
	private int length = DEFAULT_LENGTH;

	private String hexColor;
	private Direction direction;

	private final WebSocketSession session;
        private final String name;
        private boolean inGame;
        
	public Snake(int id, String name, WebSocketSession session) {
		this.id = id;
		this.session = session;
		this.hexColor = SnakeUtils.getRandomHexColor();
                this.name = name;
                this.puntos = 0;
                this.inGame = false;
                this.enEspera = false;
		resetState();
	}

        public boolean isEnEspera() {
            return enEspera;
        }

        public void setEnEspera(boolean enEspera) {
            this.enEspera = enEspera;
        }

        public int getPuntos() {
            return puntos;
        }

        public void setPuntos(int puntos) {
            this.puntos += puntos;
        }

	public void resetState() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
                this.hexColor = SnakeUtils.getRandomHexColor();
                this.puntos = 0;
	}
        
        public void muerte() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
                
                if(this.puntos >= 20)
                    
                    this.puntos -= 20;
                else
                    this.puntos = 0;
	}

	private synchronized void kill() throws Exception {
		//resetState();
                muerte();
                ObjectNode n = mapper.createObjectNode();
                n.put("type","dead");
                n.put("id",this.id);
                n.put("puntos",this.puntos);
		sendMessage(n.toString());
	}

	private synchronized void reward(Snake s) throws Exception { //Ya no aumenta de tamaño por colisión positiva, sino al consumir comida
		//this.length++;
		ObjectNode n = mapper.createObjectNode();
                n.put("type","kill");
                n.put("id",s.getId());
                n.put("puntos",s.getPuntos());
		sendMessage(n.toString());
	}

	protected synchronized void sendMessage(String msg) throws Exception {
            if(this.session.isOpen())
		this.session.sendMessage(new TextMessage(msg));
	}

	public synchronized void update(Collection<Snake> snakes) throws Exception {

		Location nextLocation = this.head.getAdjacentLocation(this.direction);

		if (nextLocation.x >= Location.PLAYFIELD_WIDTH) {
			nextLocation.x = 0;
		}
		if (nextLocation.y >= Location.PLAYFIELD_HEIGHT) {
			nextLocation.y = 0;
		}
		if (nextLocation.x < 0) {
			nextLocation.x = Location.PLAYFIELD_WIDTH;
		}
		if (nextLocation.y < 0) {
			nextLocation.y = Location.PLAYFIELD_HEIGHT;
		}

		if (this.direction != Direction.NONE) {
			this.tail.addFirst(this.head);
			if (this.tail.size() > this.length) {
				this.tail.removeLast();
			}
			this.head = nextLocation;
		}

		handleCollisions(snakes);
	}

	private void handleCollisions(Collection<Snake> snakes) throws Exception {

		for (Snake snake : snakes) {/////////////////////////////////////

			boolean headCollision = this.id != snake.id && snake.getHead().equals(this.head);

			boolean tailCollision = snake.getTail().contains(this.head);

			if (headCollision || tailCollision) {
				kill();
				if (this.id != snake.id) {
					snake.reward(this);
				}
			}
		}
	}

	public synchronized Location getHead() {
		return this.head;
	}

	public synchronized Collection<Location> getTail() {
		return this.tail;
	}

	public synchronized void setDirection(Direction direction) {
		this.direction = direction;
	}

	public int getId() {
		return this.id;
	}

	public String getHexColor() {
		return this.hexColor;
	}

        public WebSocketSession getSession() {   
            return session;
        }

        public String getName() {
            return name;
        }

        public boolean isInGame() {
            return inGame;
        }

        public void setInGame(boolean inGame) {
            this.inGame = inGame;
        }

        public void aumLength(){
            this.length++;
        }
        
        
}
