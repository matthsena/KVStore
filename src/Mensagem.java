
// Mensagem.java 
import java.io.Serializable;

public class Mensagem implements Serializable {
  String type;
  String key;
  String value;
  long timestamp;

  public Mensagem(String type, String key, String value, long timestamp) {
    this.type = type;
    this.key = key;
    this.value = value;
    this.timestamp = timestamp;
  }

  public Mensagem(String type, long timestamp) {
    this.type = type;
    this.timestamp = timestamp;
  }

  public Mensagem(String type, String key) {
    this.type = type;
    this.key = key;
  }
}
