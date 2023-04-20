package proxy;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    
    private static final int BUFFER_SIZE = 1024;
    private static final int CACHE_SIZE = 5;
    private static Map<String, byte[]> cache = new HashMap<>();
    
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Web proxy server started on port 8080...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                
                // Read request from client
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                String requestString = "";
                while ((inputLine = in.readLine()) != null) {
                    requestString += inputLine + "\r\n";
                    if (inputLine.isEmpty()) {
                        break;
                    }
                }
                System.out.println("Received request from client:\n" + requestString);
                
                // Parse request to get hostname and port number
                String[] requestLines = requestString.split("\r\n");
                String[] requestLineParts = requestLines[0].split(" ");
                String url = requestLineParts[1];
                int portIndex = url.indexOf(":") + 1;
                int port = 80;
                if (portIndex > 0) {
                    int slashIndex = url.indexOf("/");
                    if (slashIndex == -1) {
                        slashIndex = url.length();
                    }
                    port = Integer.parseInt(url.substring(portIndex, slashIndex));
                }
                String hostname = url.substring(url.indexOf("/") + 2, url.indexOf(":", portIndex) != -1 ? url.indexOf(":", portIndex) : url.indexOf("/", portIndex));
                
                // Check if response is in cache
                if (cache.containsKey(url)) {
                    System.out.println("Response found in cache for " + url);
                    byte[] cachedResponse = cache.get(url);
                    OutputStream out = clientSocket.getOutputStream();
                    out.write(cachedResponse);
                    out.flush();
                    out.close();
                } else {
                    // Open connection to web server
                    Socket webSocket = new Socket(hostname, port);
                    OutputStream out = webSocket.getOutputStream();
                    out.write(requestString.getBytes());
                    out.flush();
                    
                    // Read response from web server
                    InputStream inStream = webSocket.getInputStream();
                    ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        responseBuffer.write(buffer, 0, bytesRead);
                    }
                    byte[] response = responseBuffer.toByteArray();
                    System.out.println("Received response from web server:\n" + new String(response));
                    
                    // Cache response
                    if (cache.size() >= CACHE_SIZE) {
                        String oldestUrl = cache.keySet().iterator().next();
                        cache.remove(oldestUrl);
                        System.out.println("Cache is full. Removing oldest entry for " + oldestUrl);
                    }
                    cache.put(url, response);
                    
                    // Send response to client
                    out = clientSocket.getOutputStream();
                    out.write(response);
                    out.flush();
                    out.close();
                    
                    // Close connections
                    in.close();
                    webSocket.close();
                }
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Exception caught: " + e);
        }
    }
}
