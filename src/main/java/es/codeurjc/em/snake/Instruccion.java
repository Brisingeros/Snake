/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.codeurjc.em.snake;

/**
 *
 * @author Laura
 */
public class Instruccion {
    
    private String funcion;
    private String[] params;
    
    public Instruccion(String t, String[] p){
    
        this.funcion = t;
        this.params = p;
    
    }

    public String getFuncion() {
        return funcion;
    }

    public void setFuncion(String funcion) {
        this.funcion = funcion;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] param) {
        for(int i = 0; i < param.length; i++){
            this.params[i] = param[i];
        }
    }
    
    
}
