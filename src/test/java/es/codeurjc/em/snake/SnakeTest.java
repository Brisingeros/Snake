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

public class SnakeTest {

	@BeforeClass
	public static void startServer() throws IOException{
            try {
                Application.main(new String[]{ "--server.port=9000" });
                
                String ndif = "{\"name\":\"prueba\", \"dif\":" + 1 + "}";
                /**/
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .body(ndif).asJson();/**/
                
                /*
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-Type", "application/json")
                        .field("dif", "1")
                        .field("name", "prueba")
                        .asJson();*/
                
            } catch (UnirestException ex) {
                Logger.getLogger(SnakeTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally{
                Unirest.shutdown();
            }
	}
		
	@Test
	public void testConnection() throws Exception {
		
		WebSocketClient wsc = new WebSocketClient();
		wsc.connect("ws://127.0.0.1:9000/snake");
                wsc.disconnect();		
	}
        
        
        /////////////////////////////////////////////////////////////////////////////////
        @Test
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

            System.out.println("Connected");
            
            //Unirlos todos a juego
            String nmsg3 = "{\"funcion\": \"unirGame\", \"params\": [\"" + "prueba" + "\"]}";
            for(int i = 0; i < wsc.length; i++){
            
                System.out.println(i + " Se conecta a la sala con msg: "+nmsg3);
                wsc[i].sendMessage(nmsg3);
                
            }
            

            Thread.sleep(2000);

            //Comprobar el mensaje
            String msg = firstMsg.get();

                    assertTrue("The fist message should contain 'update', but it is "+msg, msg.contains("update"));

            
            for(int i = 0; i < wsc.length; i++){
            
                wsc[i].disconnect();
            
            }
            
        }

}
