package es.codeurjc.em.snake;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import static java.awt.SystemColor.window;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

public class SnakeTest {

	@BeforeClass
	public static void startServer(){
            try {
                Application.main(new String[]{ "--server.port=9000" });
                
                Unirest.post("http://127.0.0.1:9000/newGame")
                        .header("Content-type", "application/json")
                        .body("\"name\": \"prueba\"," + "\"dif\": 1.0")
                        .asString();
                
            } catch (UnirestException ex) {
                Logger.getLogger(SnakeTest.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
		
	@Test
	public void testConnection() throws Exception {
		
		WebSocketClient wsc = new WebSocketClient();
		wsc.connect("ws://127.0.0.1:9000/snake");
                wsc.disconnect();		
	}
	
	@Test
	public void testJoin() throws Exception {
		
		AtomicReference<String> firstMsg = new AtomicReference<>();
		
		WebSocketClient wsc = new WebSocketClient();
		wsc.onMessage((session, msg) -> {
			System.out.println("TestMessage: "+msg);
			firstMsg.compareAndSet(null, msg);
		});
		
        wsc.connect("ws://127.0.0.1:9000/snake");
        
        System.out.println("Connected");
		
        /*Thread.sleep(1000);
        
        String msg = firstMsg.get();
        
		assertTrue("The fist message should contain 'join', but it is "+msg, msg.contains("join"));*/
		
        wsc.disconnect();		
	}
        
        @Test
        public void testInicioAutom() throws Exception {
            
            
            
        }

}
