import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
  private static ConcurrentHashMap<String, String> keyValueStore = new ConcurrentHashMap<>();

  public static void main(String[] args) throws IOException {
    String host;
    int port;
    int leaderPort;

    try (Scanner scanner = new Scanner(System.in)) {
      System.out.print("Enter the host name: ");
      host = scanner.nextLine();

      System.out.print("Enter the port number: ");
      port = scanner.nextInt();

      System.out.print("Enter the leader port number: ");
      leaderPort = scanner.nextInt();
    }

    String leaderHost = host;

    System.out.println("Starting server on " + host + ":" + port);
    System.out.println("Leader is " + leaderHost + ":" + leaderPort);

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(() -> handleClientRequest(clientSocket, host, port, leaderHost, leaderPort)).start();
      }
    } catch (IOException e) {
      System.err.println("Could not listen on port " + port);
      System.exit(-1);
    }
  }

  private static void handleClientRequest(Socket clientSocket, String host, int port, String leaderHost,
      int leaderPort) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        System.out.println("Received message from client: " + inputLine);

        // Parse the HTTP request method
        String[] parts = inputLine.split(" ");
        String method = parts[0];

        // Check if this server is the leader
        if (host.equals(leaderHost) && port == leaderPort && method.equals("PUT")) {
          System.out.println("Receive PUT");
          // This server is the leader and the method is PUT, handle the request
          parts = inputLine.split(" ");
          String key = parts[1];
          String value = parts[2];
          int[] servers = { 10098, 10099 };

          keyValueStore.put(key, value);

          // Replicate the value to the other servers
          replicateValue(key, value, servers);

          out.println("PUT_OK");
        } else if (method.equals("PUT")) {
          System.out.println("redirect to leader...");
          // This server is not the leader and the method is PUT, redirect the request to
          // the leader
          try (Socket socket = new Socket(leaderHost, leaderPort);
              PrintWriter outToLeader = new PrintWriter(socket.getOutputStream(), true);
              BufferedReader inFromLeader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            outToLeader.println(inputLine);
            String response = inFromLeader.readLine();
            out.println(response);
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else if (method.equals("GET")) {
          // This server received a GET request, return the corresponding value
          parts = inputLine.split(" ");
          String key = parts[1];

          String value = keyValueStore.get(key);
          if (value != null) {
            out.println(value);
          } else {
            out.println("Key not found");
          }
        } else if (method.equals("REPLICATION")) {
          // This server received a replication request, update the local
          // ConcurrentHashMap
          parts = inputLine.split(" ");
          String key = parts[1];
          String value = parts[2];

          keyValueStore.put(key, value);
        } else {
          // This server is not the leader or the method is not PUT, GET, or REPLICATION,
          // process the request
          out.println("Server received message: " + inputLine);
        }
      }

      in.close();
      out.close();
      clientSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void replicateValue(String key, String value, int[] ports) {
    // Replicate the value to the other servers using threads
    for (int port : ports) {
      new Thread(() -> {
        try (Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
          out.println("REPLICATION " + key + " " + value);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    }
  }
}