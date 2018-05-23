package es.codeurjc.em.snake;

import com.google.gson.Gson;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.*;

@RestController
public class APIRest {
    
    private static SnakeHandler handler;
    private Gson gson = new Gson();
    public static void setSnakeHandler(SnakeHandler hand){
        handler = hand;
    }
    
    @PostMapping("/newGame")
    public void crearNuevaPartida(@RequestBody String nameGame){
        handler.addGame(nameGame);
    }
    
    @GetMapping("/partidas")
    public String getPartidas(){
        ArrayList<String> aux = handler.getNombrePartidas();
        String[] sol = null;
        
        String[] solu = aux.toArray(sol);
        
        return gson.toJson(solu);
    } 
    
}
