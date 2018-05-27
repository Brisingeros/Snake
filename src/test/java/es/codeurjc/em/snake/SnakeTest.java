package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.net.URISyntaxException;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.DeploymentException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.socket.TextMessage;

public class SnakeTest {

	@BeforeClass
	public static void startServer() throws IOException{
            try {
                Application.main(new String[]{ "--server.port=9000" });
                
                String ndif = "{\"name\":\"prueba\", \"dif\":" + 1 + ", \"creador\":\"0\"}";
                /**/
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .body(ndif).asJson();/**/
                
                String ndif2 = "{\"name\":\"prueba2\", \"dif\":" + 1 + ", \"creador\":\"20\"}";
                /**/
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .body(ndif2).asJson();/**/
                
                String ndif3 = "{\"name\":\"prueba3\", \"dif\":" + 1 + ", \"creador\":\"40\"}";
                /**/
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .body(ndif3).asJson();/**/
                
                String ndif4 = "{\"name\":\"prueba4\", \"dif\":" + 1 + ", \"creador\":\"70\"}";
                /**/
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .body(ndif4).asJson();/**/
                
                /*
                String ndif5 = "{\"name\":\"prueba5\", \"dif\":" + 1 + ", \"creador\":\"0\"}";
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .body(ndif5).asJson();*/
                
            } catch (UnirestException ex) {
                Logger.getLogger(SnakeTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally{
                Unirest.shutdown();
            }
	}
		
	//@Test
	public void testConnection() throws Exception {
		
		WebSocketClient wsc = new WebSocketClient();
		wsc.connect("ws://127.0.0.1:9000/snake");
                wsc.disconnect();		
	}
        
        
        /////////////////////////////////////////////////////////////////////////////////
        //@Test
        public void testInicioAutom() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
            
            //https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
            
            AtomicReference<String> firstMsg = new AtomicReference<>();
            ObjectNode difusion = null;
		
            WebSocketClient wsc[] = new WebSocketClient[4];
                    
            //Crear 3 primero jugadores
            for(int i = 0; i < 3; i++){
                
                wsc[i] = new WebSocketClient();
                
                wsc[i].onMessage((session, msg) -> {
                    System.out.println("TestMessage: "+msg);
                    //firstMsg.compareAndSet(null, msg);
                });
                
                wsc[i].connect("ws://127.0.0.1:9000/snake");
                
                //Crear su serpiente
                String nmsg = "{\"funcion\": \"crearSerpiente\", \"params\": [\"" + i + "\"]}";
                wsc[i].sendMessage(nmsg);
                
            }
            
            //Crear 4 jugador, el único que mira msg
            wsc[3] = new WebSocketClient(); 
            wsc[3].onMessage((session, msg) -> {
                System.out.println("TestMessage: "+msg);
                firstMsg.set(msg);
            });
            wsc[3].connect("ws://127.0.0.1:9000/snake");
            
            //Crear su serpiente
            String nmsg2 = "{\"funcion\": \"crearSerpiente\", \"params\": [\"" + 3 + "\"]}";
            wsc[3].sendMessage(nmsg2);

            Thread.sleep(2000);
            System.out.println("Connected");
            
            //Unirlos todos a juego
            String nmsg3 = "{\"funcion\": \"unirGame\", \"params\": [\"" + "prueba" + "\"]}";
            for(int i = 0; i < wsc.length; i++){
            
                System.out.println(i + " Se conecta a la sala con msg: "+nmsg3);
                wsc[i].sendMessage(nmsg3);
                
            }
            

            Thread.sleep(1000);

            //Comprobar el mensaje
            String msg = firstMsg.get();

                    assertTrue("The fist message should contain 'update', but it is "+msg, msg.contains("update"));

            
            for(int i = 0; i < wsc.length; i++){
            
                wsc[i].disconnect();
            
            }
            
        }
        
        @Test
        public void testInicioManual() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
        
            AtomicReference<String> firstMsg = new AtomicReference<>();
            WebSocketClient wsc[] = new WebSocketClient[2];
            
            wsc[0] = new WebSocketClient();
            wsc[0].onMessage((session,msg)->{
            
                System.out.println("Message test: " + msg);
            
            });
            
            wsc[0].connect("ws://127.0.0.1:9000/snake");
            
            wsc[1] = new WebSocketClient();
            wsc[1].onMessage((session,msg)->{
                
                    firstMsg.set(msg);
                
            });
            wsc[1].connect("ws://127.0.0.1:9000/snake");
            
            for(int i = 0; i < wsc.length; i++){
            
                String nmsg = "{\"funcion\": \"crearSerpiente\", \"params\": [\"" + (i+20) + "\"]}";
                wsc[i].sendMessage(nmsg);
                
                

            }
            
            Thread.sleep(1000);
            
            for(int i = 0; i < wsc.length; i++){
            
                String nmsg2 = "{\"funcion\": \"unirGame\", \"params\": [\"" + "prueba2" + "\"]}";
                wsc[i].sendMessage(nmsg2);
                
            }
                        
            String nmsg3 = "{\"funcion\": \"comenzarPartida\", \"params\": [\"" + "prueba2" + "\"]}";
            wsc[0].sendMessage(nmsg3);
            
            Thread.sleep(5000);
            
            String msg = firstMsg.get();

                    assertTrue("The fist message should contain 'update', but it is "+msg, msg.contains("update"));
            
            for(int i = 0; i < wsc.length; i++){
            
                wsc[i].disconnect();
            
            }
            
        }
            
        @Test
        public void testFinJuego() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
        
            AtomicReference<String> firstMsg = new AtomicReference<>();
            WebSocketClient wsc[] = new WebSocketClient[2];
            
            wsc[0] = new WebSocketClient();
            wsc[0].onMessage((session,msg)->{
            
                System.out.println("Message test: " + msg);
            
            });
            wsc[0].connect("ws://127.0.0.1:9000/snake");
            
            wsc[1] = new WebSocketClient();
            wsc[1].onMessage((session,msg)->{
                
                if(msg.contains("finJuego")){
                    firstMsg.set(msg);
                    //Aquí debería mandarse un mensaje tipo salir, que sacase la serpiente y destruyese la sala. No hemos podido recrear este comportamiento.
                    //Por eso este test sigue mandado mensajes update
                }
                
            });
            wsc[1].connect("ws://127.0.0.1:9000/snake");
            
            for(int i = 0; i < wsc.length; i++){
            
                String nmsg = "{\"funcion\": \"crearSerpiente\", \"params\": [\"" + (i+40) + "\"]}";
                wsc[i].sendMessage(nmsg);

            }
            
            Thread.sleep(2000);
            
            for(int i = 0; i < wsc.length; i++){
                
                String nmsg2 = "{\"funcion\": \"unirGame\", \"params\": [\"" + "prueba3" + "\"]}";
                wsc[i].sendMessage(nmsg2);

            }


            String nmsg3 = "{\"funcion\": \"comenzarPartida\", \"params\": [\"" + "prueba3" + "\"]}";
            wsc[0].sendMessage(nmsg3);
            
            Thread.sleep(5000);
            
            String nmsg4 = "{\"funcion\": \"salirSala\", \"params\": [\"" + 40 + "\"]}";
            wsc[0].sendMessage(nmsg4);
            
            Thread.sleep(2000);
            String msg = firstMsg.get();

                    assertTrue("The fist message should contain 'finJuego', but it is "+msg, msg.contains("finJuego"));
                    
                    
            Thread.sleep(1000);
            
            for(int i = 0; i < wsc.length; i++){
            
                wsc[i].disconnect();
            
            }

        }
        
        
        @Test
        public void testEspera() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
            
            //https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
            
            AtomicReference<String> firstMsg = new AtomicReference<>();
            ObjectNode difusion = null;
		
            WebSocketClient wsc[] = new WebSocketClient[5];
                    
            //Crear 3 primero jugadores
            for(int i = 0; i < 4; i++){
                
                wsc[i] = new WebSocketClient();
                
                wsc[i].onMessage((session, msg) -> {
                    System.out.println("TestMessage: "+msg);
                    //firstMsg.compareAndSet(null, msg);
                });
                
                wsc[i].connect("ws://127.0.0.1:9000/snake");
                
                //Crear su serpiente
                String nmsg = "{\"funcion\": \"crearSerpiente\", \"params\": [\"" + (i+70) + "\"]}";
                wsc[i].sendMessage(nmsg);
                
            }
            
            //Crear 4 jugador, el único que mira msg
            wsc[4] = new WebSocketClient(); 
            wsc[4].onMessage((session, msg) -> {
                System.out.println("TestMessage: "+msg);
                firstMsg.set(msg);
            });
            wsc[4].connect("ws://127.0.0.1:9000/snake");
            
            //Crear su serpiente
            String nmsg2 = "{\"funcion\": \"crearSerpiente\", \"params\": [\"" + 74 + "\"]}";
            wsc[4].sendMessage(nmsg2);

            Thread.sleep(2000);
            System.out.println("Connected");
            
            //Unirlos todos a juego
            String nmsg3 = "{\"funcion\": \"unirGame\", \"params\": [\"" + "prueba4" + "\"]}";
            for(int i = 0; i < wsc.length-1; i++){
            
                System.out.println(i + " Se conecta a la sala con msg: "+nmsg3);
                wsc[i].sendMessage(nmsg3);
                
            }
            

            Thread.sleep(2000);
            
            System.out.println(4 + " Se intenta conectar a la sala con msg: "+nmsg3);
            wsc[4].sendMessage(nmsg3);
            
            Thread.sleep(1000);
            
            String nmsg4 = "{\"funcion\": \"salirSala\", \"params\": [\"" + 70 + "\"]}";
            wsc[0].sendMessage(nmsg4);
            
            
            Thread.sleep(2000); //Esperamos para que el que se encuentra en espera sepa que tiene un hueco

            //Comprobar el mensaje
            String msg = firstMsg.get();

                    assertTrue("The fist message should contain 'update', but it is "+msg, msg.contains("update"));

            
            for(int i = 0; i < wsc.length; i++){
            
                wsc[i].disconnect();
            
            }
            
        }

        
        //@Test
        public void testCarga(){
        
            //Nuestras partidas son generadas por APIRest
            //El creador de la sala es gestionado en JavaScript
            //No podemos simular el comportamiento de botones y creaciones empleado en JavaScript en este test
            
        }
}
