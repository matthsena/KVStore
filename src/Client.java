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
    // para armazenar os carimbos de data/hora das chaves
    List<ServerInfo> servers = new ArrayList<>();
    ConcurrentHashMap<String, Long> keyTimestamp = new ConcurrentHashMap<>();

    try (Scanner scanner = new Scanner(System.in)) {
      // Pede ao usuário para inserir informações de servidor para cada um dos três
      // servidores
      for (int i = 0; i < 3; i++) {
        System.out.print("Enter server host: ");
        String host = scanner.nextLine();

        System.out.print("Enter server port: ");
        int port = scanner.nextInt();
        scanner.nextLine();

        servers.add(new ServerInfo(host, port));
      }

      // Loop principal do cliente
      while (true) {
        // Pede ao usuário para selecionar uma opção (PUT ou GET)
        System.out.println("Select an option:");
        System.out.println("1. PUT");
        System.out.println("2. GET");

        int choice = scanner.nextInt();
        scanner.nextLine();

        // Seleciona aleatoriamente um servidor da lista de servidores
        Random random = new Random();
        ServerInfo serverInfo = servers.get(random.nextInt(servers.size()));

        // Cria um novo soquete e fluxos de entrada/saída de objeto para se comunicar
        // com o servidor selecionado
        try (Socket socket = new Socket(serverInfo.host, serverInfo.port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

          if (choice == 1) {
            // Lida com uma solicitação PUT
            System.out.print("Enter key-value item (e.g. key=value): ");
            String input = scanner.nextLine();

            String[] parts2 = input.split("=");
            String key = parts2[0];
            String value = parts2[1];

            // Cria uma nova mensagem PUT e a envia para o servidor
            Mensagem mensagem = new Mensagem("PUT", key, value);
            out.writeObject(mensagem);

            // Lê a resposta do servidor e lida com ela de acordo
            Mensagem response = (Mensagem) in.readObject();
            if (response.method.equals("PUT_OK")) {
              System.out.println("PUT_OK key: " + key + " value " + value + " timestamp " + response.timestamp
                  + " realizada no servidor " + serverInfo.host + ":" + serverInfo.port);

              // Armazena o carimbo de data/hora da chave no ConcurrentHashMap
              keyTimestamp.put(key, response.timestamp);

            } else {
              System.out.println("PUT request failed:" + response.value);
            }
          } else if (choice == 2) {
            // Lida com uma solicitação GET
            System.out.print("Enter key: ");
            String key = scanner.nextLine();

            // Obtém o carimbo de data/hora da chave do ConcurrentHashMap
            long clientKeyTimestamp = keyTimestamp.getOrDefault(key, 0L);

            // Cria uma nova mensagem GET com o carimbo de data/hora da chave e a envia para
            // o servidor
            Mensagem mensagem = new Mensagem("GET", key, clientKeyTimestamp);
            out.writeObject(mensagem);

            // Lê a resposta do servidor e lida com ela de acordo
            Mensagem response = (Mensagem) in.readObject();

            if (response == null) {
              System.out.println("KEY_NOT_FOUND");
            } else if (response.method.equals("GET_OK")) {
              System.out.println("GET key: " + key + " value: " + response.value + " obtido do servidor "
                  + serverInfo.host + ":" + serverInfo.port + ", meu timestamp " + clientKeyTimestamp
                  + " and server timestamp " + response.timestamp);

              // Armazena o carimbo de data/hora da chave no ConcurrentHashMap
              keyTimestamp.put(key, response.timestamp);
            } else if (response.method.equals("TRY_OTHER_SERVER_OR_LATER")) {
              System.out.println("TRY_OTHER_SERVER_OR_LATER");
            }
          } else {
            // Sai do loop principal se o usuário selecionar uma opção inválida
            break;
          }
        }
      }
    }
  }
}