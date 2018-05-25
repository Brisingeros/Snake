package es.codeurjc.em.snake;

import java.util.Collection;

/**
 *
 * @author Brisin
 */
public class Comida {
    
    private Location pos;
    private String color;
    
    
    public Comida(){
        this.pos = SnakeUtils.getRandomLocation();
        this.color = SnakeUtils.getRandomHexColor();
    }
    
    public Snake update(Collection<Snake> sneks){
        for(Snake s : sneks){
            if(s.getHead().x == this.pos.x && s.getHead().y == this.pos.y){
                s.aumLength(); //Aumentar tamaño serpiente al comer
                
                return s;
            }
        }
        
        return null;
    }

    public int getPosX() {
        return pos.x;
    }

    public int getPosY() {
        return pos.y;
    }

    public String getColor() {
        return color;
    }
    
    
    
}
