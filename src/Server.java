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
  private static ConcurrentHashMap<String, Valores> keyValueStore = new ConcurrentHashMap<>();

  public static void main(String[] args) throws IOException {

    try (Scanner scanner = new Scanner(System.in)) {
      System.out.print("Enter the host name: ");
      String host = scanner.nextLine();
      System.out.print("Enter the port number: ");
      int port = scanner.nextInt();
      System.out.print("Enter the leader port number: ");
      int leaderPort = scanner.nextInt();

      String leaderHost = host;

      try (ServerSocket serverSocket = new ServerSocket(port)) {
        while (true) {
          Socket clientSocket = serverSocket.accept();
          new Thread(() -> handleClientRequest(clientSocket, host, port, leaderHost, leaderPort)).start();
        }
      }
    }
  }

  private static void handleClientRequest(Socket clientSocket, String host, int port, String leaderHost,
      int leaderPort) {
    try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
      Mensagem mensagem = (Mensagem) in.readObject();

      if (host.equals(leaderHost) && port == leaderPort && mensagem.method.equals("PUT")) {
        System.out.println("Cliente [IP]:[porta] PUT key: " + mensagem.key + " value: " + mensagem.value);
        int[] servers = { 10097, 10098, 10099 };

        Valores item = new Valores(mensagem.value, mensagem.timestamp);
        keyValueStore.put(mensagem.key, item);

        if (replicateValue(mensagem, servers)) {
          System.out.println(
              "Enviando PUT_OK ao Cliente [IP]:[porta] da key: " + mensagem.key + " ts: " + mensagem.timestamp);
          out.writeObject(new Mensagem("PUT_OK", mensagem.timestamp));
        } else {
          out.writeObject(new Mensagem("PUT_ERROR"));
        }
      } else if (mensagem.method.equals("PUT")) {
        System.out.println("Encaminhando PUT key: " + mensagem.key + " value: " + mensagem.value);

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
        Valores data = keyValueStore.get(mensagem.key);

        System.out.println("Cliente [IP]:[porta] GET key:" + mensagem.key + " ts:" + mensagem.timestamp + ". Meu ts é "
            + data.timestamp + ", portanto devolvendo "
            + (mensagem.timestamp < data.timestamp ? "TRY_OTHER_SERVER_OR_LATER" : "GET_OK " + data.value));
        out.writeObject(new Mensagem(mensagem.timestamp < data.timestamp ? "TRY_OTHER_SERVER_OR_LATER" : "GET_OK",
            data.value, data.timestamp));
      } else if (host.equals(leaderHost) && port == leaderPort && mensagem.method.equals("REPLICATION")) {
        out.writeObject(new Mensagem("REPLICATION_OK"));
      } else if (mensagem.method.equals("REPLICATION")) {
        System.out
            .println("REPLICATION key:" + mensagem.key + " value: " + mensagem.value + " ts: " + mensagem.timestamp);

        Valores item = new Valores(mensagem.value, mensagem.timestamp);
        keyValueStore.put(mensagem.key, item);

        out.writeObject(new Mensagem("REPLICATION_OK"));
      }
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  private static boolean replicateValue(Mensagem msg, int[] ports) {
    List<Thread> threads = new ArrayList<>();

    for (int port : ports) {
      Thread thread = new Thread(() -> {
        try (Socket socket = new Socket("localhost", port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
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

    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    return true;
  }

  private static class Valores {
    public String value;
    public long timestamp;

    public Valores(String value, long timestamp) {
      this.value = value;
      this.timestamp = timestamp;
    }
  }
}