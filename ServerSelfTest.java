package com.musix;
import com.musix.server.MusixHttpServer;
import java.io.*;
import java.net.*;

public class ServerSelfTest {
    public static void main(String[] args) {
        try {
            MusixHttpServer server = new MusixHttpServer(8085);
            server.start();
            Thread.sleep(1000);

            // Send raw HTTP request via Socket to 127.0.0.1:8085
            Socket s = new Socket("127.0.0.1", 8085);
            OutputStream out = s.getOutputStream();
            out.write("GET /api/status HTTP/1.1\r\nHost: localhost:8085\r\n\r\n".getBytes());
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
                if (line.startsWith("{")) break;
            }
            s.close();
            server.stop();

            System.out.println("SELF TEST RESPONSE:\n" + sb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
