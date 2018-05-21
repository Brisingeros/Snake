package es.codeurjc.em.snake;

import org.springframework.web.socket.WebSocketSession;

public interface Function {
    public void ExecuteAction(String[] params,WebSocketSession session);
}
