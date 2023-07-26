import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
  private static ConcurrentHashMap<String, Valores> keyValueStore = new ConcurrentHashMap<>();

  public static void main(String[] args) throws IOException {
    try (Scanner scanner = new Scanner(System.in)) {
      System.out.print("Digite o IP: ");
      String host = scanner.nextLine();
      System.out.print("Digite o número da porta: ");
      int port = scanner.nextInt();
      System.out.print("Digite o número da porta do LIDER: ");
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

      String clientIP = clientSocket.getInetAddress().getHostAddress();
      int clientPort = clientSocket.getPort();

      // define se o servidor é o líder
      boolean isLeader = host.equals(leaderHost) && port == leaderPort;

      if (isLeader && mensagem.method.equals("PUT")) {
        // Se este servidor é o líder e a mensagem é um PUT
        System.out.println(
            "Cliente " + clientIP + ":" + clientPort + " PUT key: " + mensagem.key + " value: " + mensagem.value);

        // Cria um novo timestamp para a mensagem
        long newTimestamp = Instant.now().toEpochMilli();

        // Adiciona o valor ao armazenamento de chave-valor
        Valores item = new Valores(mensagem.value, newTimestamp);
        keyValueStore.put(mensagem.key, item);

        // Atualiza o timestamp da mensagem
        mensagem.timestamp = newTimestamp;

        // Replica o valor em outros servidores
        if (replicateValue(mensagem, new int[] { 10097, 10098, 10099 })) {
          System.out.println("Enviando PUT_OK ao Cliente " + clientIP + ":" + clientPort + " da key: " + mensagem.key
              + " ts: " + newTimestamp);
          out.writeObject(new Mensagem("PUT_OK", newTimestamp));
        } else {
          out.writeObject(new Mensagem("PUT_ERROR"));
        }
      } else if (mensagem.method.equals("PUT")) {
        // Encaminha a mensagem para o líder
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
        // Obtém o valor da chave do armazenamento de chave-valor
        Valores data = keyValueStore.get(mensagem.key);

        // Verifica se o timestamp da mensagem é mais atualizado do que o timestamp do
        // valor armazenado
        if (mensagem.timestamp > data.timestamp) {
          // Se for maior, retorna TRY_OTHER_SERVER_OR_LATER
          System.out.println(
              "Cliente " + clientIP + ":" + clientPort + " GET key:" + mensagem.key + " ts:" + mensagem.timestamp
                  + ". Meu ts é " + data.timestamp + ", portanto devolvendo TRY_OTHER_SERVER_OR_LATER");
          out.writeObject(new Mensagem("TRY_OTHER_SERVER_OR_LATER"));
        } else {
          // Se for menor, retorna GET_OK com o valor e o timestamp atualizado
          System.out.println("Cliente " + clientIP + ":" + clientPort + " GET key:" + mensagem.key + " ts:"
              + mensagem.timestamp + ". Meu ts é " + data.timestamp + ", portanto devolvendo GET_OK " + data.value);
          out.writeObject(new Mensagem("GET_OK", mensagem.key, data.value, data.timestamp));
        }
      } else if (isLeader && mensagem.method.equals("REPLICATION")) {
        // Responde com uma mensagem de confirmação para a replicação
        out.writeObject(new Mensagem("REPLICATION_OK"));
      } else if (mensagem.method.equals("REPLICATION")) {
        // Adiciona o valor ao ConcurrentHashMap
        System.out
            .println("REPLICATION key:" + mensagem.key + " value: " + mensagem.value + " ts: " + mensagem.timestamp);

        Valores item = new Valores(mensagem.value, mensagem.timestamp);
        keyValueStore.put(mensagem.key, item);

        // Responde com uma mensagem de confirmação para a replicação
        out.writeObject(new Mensagem("REPLICATION_OK"));
      }
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  // Função para replicar o valor em outros servidores
  private static boolean replicateValue(Mensagem msg, int[] ports) {
    List<Thread> threads = new ArrayList<>();

    // Para cada porta de servidor, cria uma nova thread para replicar o valor
    for (int port : ports) {
      Thread thread = new Thread(() -> {

        try (Socket socket = new Socket("localhost", port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

          // Define a mensagem como uma mensagem de replicação
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

    // Aguarda todas as threads terminarem
    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    // Retorna true se todas as réplicas foram bem sucedidas
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