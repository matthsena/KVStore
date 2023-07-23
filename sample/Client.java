import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class Client {
  private static final int TIMEOUT = 5000;
  private static final int PORT1 = 10097;
  private static final int PORT2 = 10098;
  private static final int PORT3 = 10099;

  private static String ip1;
  private static String ip2;
  private static String ip3;
  private static int port1 = PORT1;
  private static int port2 = PORT2;
  private static int port3 = PORT3;
  private static long timestamp = 0;

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter IP of server 1: ");
    ip1 = scanner.nextLine();
    System.out.print("Enter IP of server 2: ");
    ip2 = scanner.nextLine();
    System.out.print("Enter IP of server 3: ");
    ip3 = scanner.nextLine();

    while (true) {
      System.out.println("Choose an option:");
      System.out.println("1. PUT");
      System.out.println("2. GET");
      System.out.println("3. EXIT");
      int option = scanner.nextInt();
      scanner.nextLine();
      if (option == 1) {
        System.out.print("Enter key: ");
        String key = scanner.nextLine();
        System.out.print("Enter value: ");
        String value = scanner.nextLine();
        Socket socket = new Socket(getRandomIp(), getRandomPort());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(new Mensagem("PUT", key, value, ++timestamp));
        socket.setSoTimeout(TIMEOUT);
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        Mensagem mensagem = (Mensagem) in.readObject();
        if (mensagem.type.equals("PUT_OK")) {
          System.out
              .println("PUT_OK key: " + mensagem.key + " value: " + mensagem.value + " timestamp: " + mensagem.timestamp
                  + " performed on the server " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        }
        socket.close();
      } else if (option == 2) {
        System.out.print("Enter key: ");
        String key = scanner.nextLine();
        Socket socket = new Socket(getRandomIp(), getRandomPort());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(new Mensagem("GET", key, null, timestamp));
        socket.setSoTimeout(TIMEOUT);
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        Mensagem mensagem = (Mensagem) in.readObject();
        if (mensagem.type.equals("GET_OK")) {
          System.out.println("GET key: " + mensagem.key + " value: " + mensagem.value + " obtained from the server "
              + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ", my timestamp " + timestamp
              + " and the server " + mensagem.timestamp);
        } else if (mensagem.type.equals("TRY_OTHER_SERVER_OR_LATER")) {
          System.out.println("TRY_OTHER_SERVER_OR_LATER key: " + mensagem.key);
        }
        socket.close();
      } else if (option == 3) {
        break;
      }
    }
  }

  private static String getRandomIp() {
    Random random = new Random();

    int randomInt = random.nextInt(3);

    if (randomInt == 0) {
      return ip1;
    } else if (randomInt == 1) {
      return ip2;
    } else {
      return ip3;
    }
  }

  private static int getRandomPort() {
    Random random = new Random();

    int randomInt = random.nextInt(3);

    if (randomInt == 0) {
      return port1;
    } else if (randomInt == 1) {
      return port2;
    } else {
      return port3;
    }
  }
}