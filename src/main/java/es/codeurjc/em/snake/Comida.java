package es.codeurjc.em.snake;

import java.util.Collection;

/**
 *
 * @author Brisin
 */
public class Comida {
    
    private int posX;
    private int posY;
    
    public Comida(int x, int y){
        this.posX = x;
        this.posY  = y;
    }
    
    public Snake update(Collection<Snake> sneks){
        for(Snake s : sneks){
            if(s.getHead().x == this.posX && s.getHead().y == this.posY){
                return s;
            }
        }
        
        return null;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }
    
    
    
}
