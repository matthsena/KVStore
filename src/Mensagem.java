
// Mensagem.java 
import java.io.Serializable;

public class Mensagem implements Serializable {
  String method;
  String key;
  String value;
  long timestamp;

  // PUT - client
  public Mensagem(String method, String key, String value) {
    this.method = method;
    this.key = key;
    this.value = value;
  }

  // GET - client
  public Mensagem(String method, String key, long timestamp) {
    this.method = method;
    this.key = key;
    this.timestamp = timestamp;
  }

  // GET_OK - server
  public Mensagem(String method, String key, String value, long timestamp) {
    this.method = method;
    this.key = key;
    this.value = value;
    this.timestamp = timestamp;
  }

  // PUT_OK - server
  public Mensagem(String method, long timestamp) {
    this.method = method;
    this.timestamp = timestamp;
  }

  // PUT ERROR - server
  // TRY_OTHER_SERVER_OR_LATER - server
  // REPLICATION_OK - server
  public Mensagem(String method) {
    this.method = method;
  }
}
