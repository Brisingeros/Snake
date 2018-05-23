package es.codeurjc.em.snake;

import com.google.gson.Gson;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.*;




@RestController
public class APIRest {
    
    private static SnakeHandler handler;
    private final Gson gson = new Gson();
    
    public static void setSnakeHandler(SnakeHandler hand){
        handler = hand;
    }
    
    @RequestMapping(value = "/newGame", method = RequestMethod.POST)
    public void crearNuevaPartida(@RequestBody String nameGame){
        handler.addGame(nameGame);
        System.out.println("//////////////////////////////////////////NUEVO");
    }
    
    @RequestMapping(value = "/partidas", method = RequestMethod.GET)
    public String getPartidas(){
        ArrayList<String> aux = handler.getNombrePartidas();
        String[] sol = new String[aux.size()];
        
        aux.toArray(sol);
        
        System.out.println("//////////////////////////////////////////GAMES");
        
        //return gson.toJson(sol);
        return gson.toJson(aux);
    }
    
}
