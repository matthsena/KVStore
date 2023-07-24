
// Mensagem.java 
import java.io.Serializable;

public class Mensagem implements Serializable {
  String method;
  String key;
  String value;
  String error;
  long timestamp;

  @Override
  public String toString() {
    return "Valores [method=" + method + ", key=" + key + ", value=" + value + ", timestamp=" + timestamp + ", error="
        + error + "]";
  }

  public Mensagem(String method, String key, String value, long timestamp) {
    this.method = method;
    this.key = key;
    this.value = value;
    this.timestamp = timestamp;
  }

  public Mensagem(String method, long timestamp) {
    this.method = method;
    this.timestamp = timestamp;
  }

  public Mensagem(String method, String key) {
    this.method = method;
    this.key = key;
  }

  public Mensagem(String method, String key, long timestamp) {
    this.method = method;
    this.key = key;
    this.timestamp = timestamp;
  }

  public Mensagem(String method) {
    this.method = method;
  }

  // public Mensagem(String error) {
  // this.error = error;
  // }
}
