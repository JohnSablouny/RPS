import java.io.*;
import java.net.*;

public class rps {
    private static final int PORT = 12345;
    private static String serverAddress;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java rps <server-ip>");
            return;
        }
        
        serverAddress = args[0];

        try (Socket socket = new Socket(serverAddress, PORT)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            // Read initial server messages and commands from the user
            new Thread(new ServerReader(in)).start();

            // Handle user input
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
            }
        } catch (IOException e) {
            System.out.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ServerReader implements Runnable {
        private BufferedReader in;

        public ServerReader(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.out.println("Server reader exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
