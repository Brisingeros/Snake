package es.codeurjc.em.snake;

import java.util.ArrayList;
import org.springframework.web.bind.annotation.*;

@RestController
public class APIRest {
    
    private static SnakeHandler handler;
    
    public static void setSnakeHandler(SnakeHandler hand){
        handler = hand;
    }
    
    @PostMapping("/newGame")
    public void crearNuevaPartida(@RequestBody String nameGame){
        
    }
    
    @GetMapping("/partidas")
    public ArrayList<String[]> getPartidas(){
        return handler.getPartidas();
    } 
    
}
