package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.*;

@RestController
public class APIRest {
    
    private static SnakeHandler handler;
    private final Gson gson = new Gson();
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    public static void setSnakeHandler(SnakeHandler hand){
        handler = hand;
    }
    
    @RequestMapping(value = "/newGame", method = RequestMethod.POST)
    public void crearNuevaPartida(@RequestBody String nameGame){

        try {
            JsonNode node = mapper.readTree(nameGame);
            
            handler.addGame(node.get("name").asText(), node.get("dif").asLong(), node.get("creador").asText());
        } catch (IOException ex) {
            Logger.getLogger(APIRest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    @RequestMapping(value = "/partidas", method = RequestMethod.GET)
    public String getPartidas(){
        
        ArrayList<String[]> aux = handler.getNombrePartidas();
        
        return gson.toJson(aux);
        
    }
    
    @RequestMapping(value = "/muroPuntos", method = RequestMethod.GET)
    public String getMuroPuntos(){
        
        CopyOnWriteArrayList<String[]> aux = handler.getMuro();
        
        for(String[] i : aux){
            System.out.println("//////////////////////////////////////////////////////////////Nombre: " + i[0] + ",    Puntos: " + i[1]);
        }
        
        return gson.toJson(aux);
        
    }
    
}
