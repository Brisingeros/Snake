package es.codeurjc.em.snake;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.bind.annotation.*;

@RestController
public class APIRest {
    
    private static SnakeHandler handler;
    private final Gson gson = new Gson();
    
    private CopyOnWriteArrayList<Puntos> puntuaciones = new CopyOnWriteArrayList<>();
    
    public static void setSnakeHandler(SnakeHandler hand){
        handler = hand;
    }
    
    @RequestMapping(value = "/newGame", method = RequestMethod.POST)
    public void crearNuevaPartida(@RequestBody String nameGame){

        handler.addGame(gson.fromJson(nameGame, String.class));
        
    }
    
    @RequestMapping(value = "/partidas", method = RequestMethod.GET)
    public String getPartidas(){
        
        ArrayList<String> aux = handler.getNombrePartidas();
        String[] sol = new String[aux.size()];
        
        aux.toArray(sol);
        
        return gson.toJson(aux);
        
    }
    
    @RequestMapping(value = "/sendPuntos", method = RequestMethod.POST)
    public void nuevosPuntos(@RequestBody String pts){

        Puntos nuevos = gson.fromJson(pts, Puntos.class);
        
        puntuaciones.add(nuevos);
        Collections.sort(puntuaciones, (Puntos o1, Puntos o2) -> {
                return (o1.getPunts() > o2.getPunts())? 1:-1; });
        
        puntuaciones = (CopyOnWriteArrayList) puntuaciones.subList(0, 10);
        
    }
    
}
