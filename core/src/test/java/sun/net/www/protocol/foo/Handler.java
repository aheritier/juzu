package sun.net.www.protocol.foo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * Foo protocol for testing purposes.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class Handler extends URLStreamHandler {

  /** . */
  private static final Map<String, byte[]> state = new HashMap<String, byte[]>();

  public static void bind(String key, byte[] bytes) {
    state.put(key, bytes);
  }

  public static void clear() {
    state.clear();
  }

  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    final byte[] bytes = state.get(u.getFile());
    return new URLConnection(u) {
      @Override
      public void connect() throws IOException {
        if (bytes == null) {
          throw new IOException("No content");
        }
      }

      @Override
      public InputStream getInputStream() throws IOException {
        if (bytes == null) {
          throw new IOException("No content");
        }
        return new ByteArrayInputStream(bytes);
      }
    };
  }
}
