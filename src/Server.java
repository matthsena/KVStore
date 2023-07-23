import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
    }
  }

  private static void handleClientRequest(Socket clientSocket, String host, int port, String leaderHost,
      int leaderPort) {
    try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
      Mensagem mensagem = (Mensagem) in.readObject();

      if (mensagem == null) {
        return;
      }

      System.out.println("Received message from client: " + mensagem.method);

      // Check if this server is the leader
      if (host.equals(leaderHost) && port == leaderPort && mensagem.method.equals("PUT")) {
        System.out.println("Receive PUT");
        // This server is the leader and the method is PUT, handle the request
        int[] servers = { 10098, 10099 };

        keyValueStore.put(mensagem.key, mensagem.value);
        // Replicate the value to the other servers
        if (replicateValue(mensagem, servers)) {
          System.out.println("Replication successful");
          out.writeObject(new Mensagem("PUT_OK", mensagem.timestamp));
        } else {
          System.out.println("Replication failed");
          out.writeObject(new Mensagem("PUT_ERROR"));
        }

      } else if (mensagem.method.equals("PUT")) {
        System.out.println("redirect to leader...");
        // This server is not the leader and the method is PUT, redirect the request to
        // the leader
        try (Socket socket = new Socket(leaderHost, leaderPort);
            ObjectOutputStream outToLeader = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inFromLeader = new ObjectInputStream(socket.getInputStream())) {
          outToLeader.writeObject(mensagem);
          Mensagem response = (Mensagem) inFromLeader.readObject();
          out.writeObject(response);
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
        }
      } else if (mensagem.method.equals("GET")) {
        // This server received a GET request, return the corresponding value
        String keyValue = keyValueStore.get(mensagem.key);
        System.out.println("Receive GET with key: " + mensagem.key + " and value: " + keyValue);
        if (!keyValue.isEmpty()) {
          out.writeObject(new Mensagem("GET_OK", mensagem.key, keyValue, 10000));
        } else {
          out.writeObject(new Mensagem("Key not found"));
        }
      } else if (mensagem.method.equals("REPLICATION")) {
        // This server received a replication request, update the local
        // ConcurrentHashMap
        System.out.println("Receive REPLICATION with values: " + mensagem.key + " " + mensagem.value);
        keyValueStore.put(mensagem.key, mensagem.value);
        System.out.println("Updated local key-value store");

        for (String key : keyValueStore.keySet()) {
          System.out.println(key + " " + keyValueStore.get(key));
        }

        System.out.println("Sending REPLICATION_OK");

        out.writeObject(new Mensagem("REPLICATION_OK", mensagem.timestamp));
      } else {
        // This server is not the leader or the method is not PUT, GET, or REPLICATION,
        // process the request
        out.writeObject(new Mensagem("Server received message: " + mensagem.method));
      }
    } catch (EOFException e) {
      // EOFException is fine here, it means the client has closed its end of the
      // socket
      System.out.println("Client closed connection.");
      e.printStackTrace();

    } catch (IOException | ClassNotFoundException e) {
      System.err.println("Error handling client request from " + clientSocket.getInetAddress() + ":"
          + clientSocket.getPort() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static boolean replicateValue(Mensagem msg, int[] ports) {
    List<Thread> threads = new ArrayList<>();

    for (int port : ports) {
      Thread thread = new Thread(() -> {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
          socket = new Socket("localhost", port);
          out = new ObjectOutputStream(socket.getOutputStream());
          in = new ObjectInputStream(socket.getInputStream());

          msg.method = "REPLICATION";

          out.writeObject(msg);

          Mensagem mensagem = (Mensagem) in.readObject();

          if (!mensagem.method.equals("REPLICATION_OK")) {
            throw new RuntimeException("Replication failed on port " + port);
          }
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
        }
      });
      threads.add(thread);
      thread.start();
    }

    // Wait for all threads to finish
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return true;
  }
}