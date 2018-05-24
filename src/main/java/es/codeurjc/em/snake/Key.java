package es.codeurjc.em.snake;

import java.util.Objects;

/**
 *
 * @author Brisin
 */
public class Key {
    
    private final String k1;    //Nombre
    private final String k2;    //Session
    
    public Key(String i1, String i2){
    
        this.k1 = i1;
        this.k2 = i2;
        
    }

    public String getK1() {
        return k1;
    }

    public String getK2() {
        return k2;
    }
    
    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        
        Key key = (Key) o;
        
        if(key.getK1().equals(this.getK1())){
            return true;
        } else if(key.getK2().equals(this.getK2())){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.k2);
        return hash;
    }
    
    
    
}
