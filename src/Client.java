import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
  private static class ServerInfo {
    public final String host;
    public final int port;

    public ServerInfo(String host, int port) {
      this.host = host;
      this.port = port;
    }
  }

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    // Cria uma lista vazia de informações de servidor e um ConcurrentHashMap vazio
    // para armazenar as keys e timestamp de informaçoes
    List<ServerInfo> servers = new ArrayList<>();
    ConcurrentHashMap<String, Long> keyTimestamp = new ConcurrentHashMap<>();

    try (Scanner scanner = new Scanner(System.in)) {
      while (true) {
        System.out.println("Selecione uma opção:");
        System.out.println("1. INIT");
        System.out.println("2. PUT");
        System.out.println("3. GET");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 1) {
          // Insercao de informações de servidor para cada um dos três
          for (int i = 0; i < 3; i++) {
            System.out.printf("Digite o IP do servidor %: ", i + 1);
            String host = scanner.nextLine();

            System.out.printf("Digite a porta do servidor %: ", i + 1);
            int port = scanner.nextInt();
            scanner.nextLine();

            servers.add(new ServerInfo(host, port));
          }
        } else if (choice == 2 || choice == 3) {
          // SEMPRE Seleciona aleatoriamente um servidor da lista de servidores
          Random random = new Random();
          ServerInfo serverInfo = servers.get(random.nextInt(servers.size()));

          try (Socket socket = new Socket(serverInfo.host, serverInfo.port);
              ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
              ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            if (choice == 2) {
              // PUT
              System.out.print("Digite a chave e valor do item (e.g. key:value): ");
              String input = scanner.nextLine();

              String[] parts2 = input.split(":");
              String key = parts2[0];
              String value = parts2[1];

              // Cria uma nova mensagem PUT e a envia para o servidor
              Mensagem mensagem = new Mensagem("PUT", key, value);
              out.writeObject(mensagem);

              // Resposta do servidor
              Mensagem response = (Mensagem) in.readObject();
              if (response.method.equals("PUT_OK")) {
                System.out.println("PUT_OK key: " + key + " value " + value + " timestamp " + response.timestamp
                    + " realizada no servidor " + serverInfo.host + ":" + serverInfo.port);

                // Armazena a key e o timestamp no ConcurrentHashMap
                keyTimestamp.put(key, response.timestamp);

              }
            } else if (choice == 3) {
              // GET
              System.out.print("Digite a chave: ");
              String key = scanner.nextLine();

              // Obtém o timestamp associado a chave do ConcurrentHashMap
              // Caso nao exista retorna o valor 0
              long clientKeyTimestamp = keyTimestamp.getOrDefault(key, 0L);

              // GET para o servidor com o ts que o cliente tem daquela chave
              Mensagem mensagem = new Mensagem("GET", key, clientKeyTimestamp);
              out.writeObject(mensagem);

              // Resposta do servidor
              Mensagem response = (Mensagem) in.readObject();

              if (response == null) {
                System.out.println("KEY_NOT_FOUND");
              } else if (response.method.equals("GET_OK")) {
                System.out.println("GET key: " + response.key + " value: " + response.value + " obtido do servidor "
                    + serverInfo.host + ":" + serverInfo.port + ", meu timestamp " + clientKeyTimestamp
                    + " and server timestamp " + response.timestamp);

                // Armazena o novo timestamp no ConcurrentHashMap
                keyTimestamp.put(key, response.timestamp);
              } else if (response.method.equals("TRY_OTHER_SERVER_OR_LATER")) {
                System.out.println("TRY_OTHER_SERVER_OR_LATER");
              }
            }
          }
        } else {
          break;
        }
      }
    }
  }
}