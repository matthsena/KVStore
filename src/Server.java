import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Server {
  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(1234)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        Thread thread = new Thread(() -> handleClientRequest(clientSocket));
        thread.start();
      }
    }
  }

  private static void handleClientRequest(Socket clientSocket) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        System.out.println("Received message from client: " + inputLine);
        out.println("Server received message: " + inputLine);

        // Add a random delay between 0 and 10 seconds
        Random random = new Random();
        int delay = random.nextInt(10000);
        Thread.sleep(delay);

        System.out.println("End Delay: " + delay);
      }

      in.close();
      out.close();
      clientSocket.close();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}