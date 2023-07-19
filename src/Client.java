import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Client {
  public static void main(String[] args) throws IOException {
    List<String> servers = new ArrayList<>();

    try (Scanner scanner = new Scanner(System.in)) {
      for (int i = 0; i < 3; i++) {
        System.out.print("Enter server host and port (e.g. localhost:1234): ");
        String input = scanner.nextLine();
        servers.add(input);
      }

      Random random = new Random();
      int index = random.nextInt(servers.size());
      String server = servers.get(index);

      System.out.println("Connecting to server " + server);
      String[] parts = server.split(":");
      String host = parts[0];
      int port = Integer.parseInt(parts[1]);

      Socket socket = new Socket(host, port);

      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

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
        System.out.println("3. Exit");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 1) {
          System.out.print("Enter key-value item (e.g. key=value): ");
          String input = scanner.nextLine();

          String[] parts2 = input.split("=");
          String key = parts2[0];
          String value = parts2[1];

          Mensagem mensagem = new Mensagem("PUT", key, value, timestamp[0]);
          out.writeObject(mensagem);

          Mensagem response = (Mensagem) in.readObject();
          if (response.method.equals("PUT_OK")) {
            System.out.println("PUT request successful");
          } else {
            System.out.println("PUT request failed:" + response.value);
          }
        } else if (choice == 2) {
          System.out.print("Enter key: ");
          String key = scanner.nextLine();

          Mensagem mensagem = new Mensagem("GET", key);
          out.writeObject(mensagem);

          Mensagem response = (Mensagem) in.readObject();
          if (response.method.equals("GET_OK")) {
            System.out.println("Value for key " + key + " (timestamp " + response.timestamp + "): " + response.value);
          } else {
            System.out.println("GET request failed: " + response.method + " -- " + response.value);
          }
        } else if (choice == 3) {
          break;
        } else {
          System.out.println("Invalid choice");
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