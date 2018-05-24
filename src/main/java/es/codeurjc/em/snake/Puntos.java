package es.codeurjc.em.snake;

import org.springframework.stereotype.Component;

/**
 *
 * @author Brisin
 */
@Component
public class Puntos{
    
    private String name;
    private int punts;
    
    public Puntos(String nombre, int puntos){
    
        this.name = nombre;
        this.punts = puntos;
        
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPunts() {
        return punts;
    }

    public void setPunts(int punts) {
        this.punts = punts;
    }
    
    
    
}
