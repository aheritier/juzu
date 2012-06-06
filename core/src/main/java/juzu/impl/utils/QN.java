package juzu.impl.utils;

import java.io.Serializable;
import java.util.Iterator;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class QN implements CharSequence, Serializable, Iterable<String> {

  /** . */
  public static QN EMPTY = new QN("", new String[0]);

  public static QN parse(CharSequence value) {
    return parse(value, 0, 0);
  }

  private static QN parse(CharSequence value, int from, int size) {
    int len = value.length();
    if (from < len) {
      int to = -1;
      for (int current = from;current < len;current++) {
        if (value.charAt(current) == '.') {
          to = current;
          break;
        }
      }
      if (to == -1) {
        String[] names = new String[size + 1];
        names[size] = value.subSequence(from, len).toString();
        return new QN(value.toString(), names);
      }
      else if (to - from < 1 || len - to < 2) {
        throw new IllegalArgumentException(" " + (to - from) + " " + (len - to));
      }
      else {
        QN that = parse(value, to + 1, size + 1);
        that.names[size] = value.subSequence(from, to).toString();
        return that;
      }
    }
    else {
      return new QN(value.toString(), new String[size], size);
    }
  }

  /** The original value. */
  private final String value;

  /** The names. */
  private final String[] names;

  /** . */
  private final int size;

  QN(String value, String[] names) {
    this.value = value;
    this.names = names;
    this.size = names.length;
  }

  QN(String value, String[] names, int size) {
    this.value = value;
    this.names = names;
    this.size = size;
  }

  public int length() {
    return value.length();
  }

  public char charAt(int index) {
    return value.charAt(index);
  }

  public CharSequence subSequence(int start, int end) {
    return value.subSequence(start, end);
  }

  public String get(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Index " + index + " cannot be negative");
    }
    else if (index < size) {
      return names[index];
    }
    else {
      throw new IndexOutOfBoundsException("Index " + index + " cannot be greater than bound " + size);
    }
  }

  public int size() {
    return size;
  }

  /** The cached parent. */
  private QN parent;

  public Iterator<String> iterator() {
    return Tools.iterator(0, size, names);
  }

  /**
   * Returns the parent qn or null if it does not exist.
   *
   * @return the parent qn
   */
  public QN getParent() {
    if (parent == null) {
      switch (size) {
        case 0:
          break;
        case 1:
          parent = EMPTY;
          break;
        default:
          parent = new QN(value.substring(0, value.length() - names[size - 1].length() - 1), names, size - 1);
          break;
      }
    }
    return parent;
  }

  public String getValue() {
    return value;
  }

  public QN append(QN suffix) throws NullPointerException {
    return append(suffix.names, suffix.size);
  }

  public QN append(String... suffix) throws NullPointerException, IllegalArgumentException {
    return append(suffix, suffix.length);
  }

  private QN append(String[] suffix, int size) throws NullPointerException, IllegalArgumentException {
    if (suffix == null) {
      throw new NullPointerException("No null names accepted");
    }
    if (size == 0) {
      return this;
    }
    else {
      StringBuilder sb = new StringBuilder(value);
      for (int i = 0;i < size;i++) {
        String s = suffix[i];
        if (s == null) {
          throw new IllegalArgumentException("No null name accepted");
        }
        if (s.isEmpty()) {
          throw new IllegalArgumentException("No empty name accepted");
        }
        if (s.indexOf('.') != -1) {
          throw new IllegalArgumentException("A name cannot contain a '.'");
        }
        sb.append('.');
        sb.append(s);
      }
      String[] foo = new String[size + suffix.length];
      System.arraycopy(names, 0, foo, 0, size);
      System.arraycopy(suffix, 0, foo, size, suffix.length);
      return new QN(sb.toString(), foo, foo.length);
    }
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }

  public boolean isPrefix(QN qn) {
    if (qn.parent == this) {
      // The fast way
      return true;
    }
    else if (size <= qn.size) {
      for (int i = 0;i < size;i++) {
        if (!names[i].equals(qn.names[i])) {
          return false;
        }
      }
      return true;
    }
    else {
      return false;
    }
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof QN) {
      QN that = (QN)obj;
      return size == that.size && value.equals(that.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
