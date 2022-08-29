package org.gtp.websocket.server;

import org.gtp.websocket.WebSocketConstants;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServer extends WebSocketServer {

	public SimpleServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //conn.send("Welcome to the server!"); //This method sends a message to the new client
        //broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
        if (message.contains("{\"OperationType\":\"Authenticate\",\"Username\":\"Hugo\",\"Password\":\"Password\"}")) {
            pause(5000L);
            broadcast("{\"OperationType\":\"Authenticate\",\"Username\":\"Hugo\",\"Password\":\"Password\",\"OperationResult\":\"Ok\"}");
        }
        if (message.contains("\"OperationType\":\"Request-Destination-Confirmation\"")) {
            pause(5000L);
            System.out.println("broadcasting message to "	+ conn.getRemoteSocketAddress());
            broadcast("{\"OperationType\":\"Destination-Confirmation\",\"OperationResult\":\"Ok\"}");
        }
        if (message.contains("\"OperationType\":\"Request-Package-Volume-Confirmation\"")) {
            System.out.println("broadcasting message to "	+ conn.getRemoteSocketAddress());
            pause(5000L);
            broadcast("{\"OperationType\":\"Package-Volume-Confirmation\",\"OperationResult\":\"Ok\"}");
        }
        if (message.contains("\"OperationType\":\"Request-Order-List\"")) {
            pause(5000L);
            broadcast("{\n" +
                    " \"OperationType\":\"Order-List\",\n" +
                    " \"Order-List\":\n" +
                    "  [\n" +
                    "   {\n" +
                    "    \"order-id\": \"BC123D12-E43C-A75E-192A-8AD4-18A8D7EC6E8D\",\n" +
                    "    \"task-order\": 3,\n" +
                    "    \"task-id\": \"023987AB-01EC-02AE-192F-8294-47859CBF8732\",\n" +
                    "    \"destination-name\": \"AD-051\",\n" +
                    "    \"destination-coordinates\": \"(55.27,-112.98,1.8)\",\n" +
                    "    \"product-sku\": \"7894900700046\",\n" +
                    "    \"product-description\": \"COCA-COLA ZERO LATA 350ML\",\n" +
                    "    \"product-weight\": 1256,\n" +
                    "    \"package-count\": 12,\n" +
                    "    \"distance\": 4280\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"order-id\": \"BC123D12-E43C-A75E-192A-8AD4-18A8D7EC6E8D\",\n" +
                    "    \"task-order\": 1,\n" +
                    "    \"task-id\": \"2A2580BE-2EA9-4E7B-A422-2651E231D1CB\",\n" +
                    "    \"destination-name\": \"AD-047\",\n" +
                    "    \"destination-coordinates\": \"(56.27,12.98,3.6)\",\n" +
                    "    \"product-sku\": \"7894900700078\",\n" +
                    "    \"product-description\": \"COCA-COLA LATA 350ML\",\n" +
                    "    \"product-weight\": 1256,\n" +
                    "    \"package-count\": 5,\n" +
                    "    \"distance\": 180\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"order-id\": \"BC123D12-E43C-A75E-192A-8AD4-18A8D7EC6E8D\",\n" +
                    "    \"task-order\": 2,\n" +
                    "    \"task-id\": \"407D77F6-AA0D-11E8-A137-529269FB1459\",\n" +
                    "    \"destination-name\": \"AD-059\",\n" +
                    "    \"destination-coordinates\": \"(45.27,-11.78,1.8)\",\n" +
                    "    \"product-sku\": \"7894900700027\",\n" +
                    "    \"product-description\": \"FANTA ZERO LATA 350ML\",\n" +
                    "    \"product-weight\": 1266,\n" +
                    "    \"package-count\": 7,\n" +
                    "    \"distance\": 280\n" +
                    "  }\n" +
                    "]\n" +
                    "}");
        }
    }

    private void pause(long timeoutMillis) {

        synchronized (SimpleServer.this) {
            try {
                wait(timeoutMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage( WebSocket conn, ByteBuffer message ) {
        System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("an error occurred on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
    }

    @Override
    public void onStart() {
        System.out.println("server started successfully");
    }

    @Override
    public void run() {

        /*ExecutorService service = Executors.newFixedThreadPool(4);
        System.out.println("l : ");

        service.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                while (true) {
                    System.out.println("Enter String: ");
                    try {
                        String s = br.readLine();
                        System.out.println("[Sending]: " + s);
                        broadcast(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error reading line: " + e.getMessage());
                    }
                }
            }
        });/**/
        InputThread inputThread = new InputThread();
        inputThread.start();

        System.out.println("ll : ");

        super.run();
    }

    public static void main(String[] args) {
        //String host = "localhost";
        //String host = "192.168.1.65";//"192.168.1.65";
        String host = getIpAddress();//"192.168.1.7";//"192.168.1.65";
        int port = WebSocketConstants.PORT;//8887;
        //System.err.println("IP = " + getIpAddress());

        InetSocketAddress i = new InetSocketAddress(host, port);
        WebSocketServer server = new SimpleServer(i);
        server.run();
    }

    private class InputThread extends Thread {

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println("Enter String: ");
                try {
                    String s = br.readLine();

                    if (s.equals("close")) {
                        for (WebSocket webSocket : getConnections()) {
                            webSocket.close();
                        }
                        System.out.println("[Sending]: " + s);
                        return;
                    }
                    System.out.println("[Sending]: " + s);
                    broadcast(s);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error reading line: " + e.getMessage());
                }
            }
        }
    }

    private static String getIpAddress() {

        /*try {
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements();)
            {
                NetworkInterface e = n.nextElement();

                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements();)
                {
                    InetAddress addr = a.nextElement();
                    System.out.println(" - " + addr.getHostAddress());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }/**/

        InetAddress IP = null;
        try {
            IP = InetAddress.getLocalHost();
            return IP.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*public static void main(String[] args){
        ServerSocket server = new ServerSocket(80);

        System.out.println("Server has started on 127.0.0.1:80.\r\nWaiting for a connection...");

        Socket client = server.accept();

        System.out.println("A client connected.");
    }/**/
}
