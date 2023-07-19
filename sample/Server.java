
// Server.java 
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Server {
    private static final int PORT = 10097;
    private static final int LEADER_PORT = 10098;
    private static final int REPLICATION_PORT = 10099;
    private static final String LEADER_IP = "127.0.0.1";
    private static final String REPLICATION_IP = "127.0.0.1";
    private static final int TIMEOUT = 5000;

    private static final Random RANDOM = new Random();

    private static Map<String, String> keyValueStore = new HashMap<>();
    private static Map<String, Long> timestampStore = new HashMap<>();
    private static boolean isLeader = false;
    private static int replicationCount = 0;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Scanner scanner = new Scanner(System.in);

        // System.out.print("Enter IP address: ");
        String ip = LEADER_IP;

        // System.out.print("Enter port number: ");
        int port = PORT;

        // System.out.print("Enter leader IP address: ");
        String leaderIp = LEADER_IP;

        // System.out.print("Enter leader port number: ");
        int leaderPort = LEADER_PORT;

        String replicationIp = REPLICATION_IP;

        int replicationPort = REPLICATION_PORT;

        if (ip.equals(leaderIp) && port == leaderPort) {
            isLeader = true;
        }
        ServerSocket serverSocket = new ServerSocket(port);
        ServerSocket replicationSocket = new ServerSocket(REPLICATION_PORT);
        Socket leaderSocket = new Socket(leaderIp, leaderPort);
        Socket replicationSocket1 = new Socket(replicationIp, replicationPort);
        Socket replicationSocket2 = new Socket(replicationIp, replicationPort);

        Thread replicationThread = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = replicationSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Mensagem mensagem = (Mensagem) in.readObject();
                    keyValueStore.put(mensagem.key, mensagem.value);
                    timestampStore.put(mensagem.key, mensagem.timestamp);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new Mensagem("REPLICATION_OK", mensagem.timestamp));
                    replicationCount++;
                    if (replicationCount == 2) {
                        replicationCount = 0;
                        ObjectOutputStream leaderOut = new ObjectOutputStream(leaderSocket.getOutputStream());
                        leaderOut.writeObject(
                                new Mensagem("REPLICATION_OK", mensagem.key, mensagem.value, mensagem.timestamp));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        replicationThread.start();

        while (true) {
            Socket socket = serverSocket.accept();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Mensagem mensagem = (Mensagem) in.readObject();
            if (mensagem.key == null || mensagem.key.isEmpty()) {
                continue;
            }
            if (mensagem.value == null || mensagem.value.isEmpty()) {
                continue;
            }
            if (mensagem.type.equals("PUT")) {
                if (isLeader) {
                    keyValueStore.put(mensagem.key, mensagem.value);
                    timestampStore.put(mensagem.key, mensagem.timestamp);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new Mensagem("PUT_OK", mensagem.key, mensagem.value, mensagem.timestamp));
                    ObjectOutputStream replicationOut1 = new ObjectOutputStream(replicationSocket1.getOutputStream());
                    replicationOut1
                            .writeObject(new Mensagem("REPLICATION", mensagem.key, mensagem.value, mensagem.timestamp));
                    ObjectOutputStream replicationOut2 = new ObjectOutputStream(replicationSocket2.getOutputStream());
                    replicationOut2
                            .writeObject(new Mensagem("REPLICATION", mensagem.key, mensagem.value, mensagem.timestamp));
                } else {
                    ObjectOutputStream leaderOut = new ObjectOutputStream(leaderSocket.getOutputStream());
                    leaderOut.writeObject(mensagem);
                }
            } else if (mensagem.type.equals("GET")) {
                if (keyValueStore.containsKey(mensagem.key)) {
                    long timestamp = timestampStore.get(mensagem.key);
                    if (timestamp >= mensagem.timestamp) {
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(
                                new Mensagem("GET_OK", mensagem.key, keyValueStore.get(mensagem.key), timestamp));
                    } else {
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(new Mensagem("TRY_OTHER_SERVER_OR_LATER", mensagem.key));
                    }
                } else {
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new Mensagem("GET_OK", mensagem.key, null, 0));
                }
            }
        }
    }
}
