import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Client {
  private static class ServerInfo {
    public final String host;
    public final int port;

    public ServerInfo(String host, int port) {
      this.host = host;
      this.port = port;
    }
  }

  public static void main(String[] args) throws IOException {
    List<ServerInfo> servers = new ArrayList<>();

    try (Scanner scanner = new Scanner(System.in)) {
      for (int i = 0; i < 3; i++) {
        System.out.print("Enter server host: ");
        String host = scanner.nextLine();

        System.out.print("Enter server port: ");
        int port = scanner.nextInt();
        scanner.nextLine();

        servers.add(new ServerInfo(host, port));
      }

      Socket socket = null;
      ObjectOutputStream out = null;
      ObjectInputStream in = null;

      int[] timestamp = { 0 };

      Thread thread = new Thread(() -> {
        while (true) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          timestamp[0]++;
        }
      });
      thread.start();

      while (true) {
        System.out.println("Select an option:");
        System.out.println("1. PUT");
        System.out.println("2. GET");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 1) {
          // RANDOM
          Random random = new Random();
          int index = random.nextInt(servers.size());
          ServerInfo serverInfo = servers.get(index);

          socket = new Socket(serverInfo.host, serverInfo.port);

          out = new ObjectOutputStream(socket.getOutputStream());
          in = new ObjectInputStream(socket.getInputStream());
          // END RANDOM

          System.out.print("Enter key-value item (e.g. key=value): ");
          String input = scanner.nextLine();

          String[] parts2 = input.split("=");
          String key = parts2[0];
          String value = parts2[1];

          Mensagem mensagem = new Mensagem("PUT", key, value, timestamp[0]);
          out.writeObject(mensagem);

          Mensagem response = (Mensagem) in.readObject();
          if (response.method.equals("PUT_OK")) {
            System.out.println("PUT_OK key: " + key + " value " + value + " timestamp " + timestamp
                + " realizada no servidor " + serverInfo.host + ":" + serverInfo.port);
          } else {
            System.out.println("PUT request failed:" + response.value);
          }
        } else if (choice == 2) {
          // RANDOM
          Random random = new Random();
          int index = random.nextInt(servers.size());
          ServerInfo serverInfo = servers.get(index);

          socket = new Socket(serverInfo.host, serverInfo.port);
          out = new ObjectOutputStream(socket.getOutputStream());
          in = new ObjectInputStream(socket.getInputStream());
          // END RANDOM

          System.out.print("Enter key: ");
          String key = scanner.nextLine();

          Mensagem mensagem = new Mensagem("GET", key, timestamp[0]);
          out.writeObject(mensagem);

          Mensagem response = (Mensagem) in.readObject();

          if (response == null) {
            System.out.println("GET key: " + key + " value: null obtido do servidor " + serverInfo.host + ":"
                + serverInfo.port + ", meu timestamp " + timestamp[0] + " and server timestamp " + "serverTimestamp");
          } else if (response.method.equals("GET_OK")) {
            System.out.println("GET key: " + key + " value: " + response.value + " obtido do servidor "
                + serverInfo.host + ":"
                + serverInfo.port + ", meu timestamp " + timestamp[0] + " and server timestamp " + "serverTimestamp");
          } else if (response.method.equals("TRY_OTHER_SERVER_OR_LATER")) {
            System.out.println("TRY_OTHER_SERVER_OR_LATER");
          }
        } else {
          break;
        }
      }

      thread.interrupt();
      out.close();
      in.close();
      socket.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}