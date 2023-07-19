import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

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

          out.println("PUT " + timestamp[0] + " " + input);

          String response = in.readLine();
          if (response.equals("PUT_OK")) {
            System.out.println("PUT request successful");
          } else {
            System.out.println("PUT request failed:" + response);
          }
        } else if (choice == 2) {
          System.out.print("Enter key: ");
          String key = scanner.nextLine();

          out.println("GET " + key);

          String response = in.readLine();
          if (response.startsWith("GET_OK")) {
            String[] parts2 = response.split(" ");
            int timestamp2 = Integer.parseInt(parts2[1]);
            String value = parts2[2];
            System.out.println("Value for key " + key + " (timestamp " + timestamp2 + "): " + value);
          } else {
            System.out.println("GET request failed: " + response);
          }
        } else if (choice == 3) {
          break;
        } else {
          System.out.println("Invalid choice");
        }
      }

      thread.interrupt();
      in.close();
      out.close();
      socket.close();
    }
  }
}