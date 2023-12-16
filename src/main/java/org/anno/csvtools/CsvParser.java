package org.anno.csvtools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;

// A parser of CSV files. For effciency, this class works at the byte level. It allocated large buffers, where quoted cells are stripped of their quotes and unescaped. 
public class CsvParser {
  private final ReadableByteChannel in;
  private boolean eof = false;

  private final ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
  private ByteBuffer currentBuffer = ByteBuffer.allocate(1024 * 1024);

  CsvParser(ReadableByteChannel in) throws IOException {
    this.in = in;
    ensureFilled();
  }

  private void ensureFilled() throws IOException {
    while (currentBuffer.remaining() > 0 && !eof) {
      eof = in.read(currentBuffer) == -1;
    }
    currentBuffer.flip();
  }

  boolean isEndOfRow() {
    if (currentBuffer.remaining() == 0 && eof) {
      return true;
    }
    int c = currentBuffer.array()[currentBuffer.position()];
    return c == '\n' || c == '\r';
  }

  // Returns the next cell or null if there are no more cells.
  ParsedCell nextCell() throws IOException {
    if (!eof && currentBuffer.remaining() < 1024) {
      ByteBuffer newBuffer = ByteBuffer.allocate(1024 * 1024);
      newBuffer.put(currentBuffer);
      buffers.add(currentBuffer);
      currentBuffer = newBuffer;
      ensureFilled();
    }
    int p = currentBuffer.position();
    int limit = currentBuffer.limit();
    byte[] b = currentBuffer.array();
    while (p < limit && (b[p] == ',' || b[p] == '\n' || b[p] == '\r')) {
      p++;
    }
    int start = p;
    if (b[p] != '"') {
      while (p < limit && b[p] != ',' && b[p] != '\n') {
        p++;
      }
      currentBuffer.position(p < limit ? p + 1 : p);
      return new ParsedCell(b, start, p - start);
    }
    while (true) {
      p++;
      while (p < limit && b[p] != '"') {
        p++;
      }
      if (p == limit) {
        currentBuffer.position(limit);
        return null;
      }
      if (b[p + 1] != '"') {
        currentBuffer.position(p + 2);
        return new ParsedCell(b, start, p + 1 - start);
      }
      p++;
    }
  }

  class ParsedCell {
    final byte[] buffer;
    final int offset;
    final int length;

    private ParsedCell(byte[] buffer, int offset, int length) {
      this.buffer = buffer;
      this.offset = offset;
      this.length = length;
    }

    // Strips outer quotes and unescapes doubled quotes and retuns the contents.
    ParsedCell unescape() {
      if (buffer[offset] != '"') {
        return this;
      }
      int p = offset + 1;
      int q = offset + length - 1;
      while (p < q && buffer[p] != '"') {
        p++;
      }
      if (p == q) {
        return new ParsedCell(buffer, offset + 1, length - 2);
      }
      int d = p++;
      while (p < q) {
        buffer[d++] = buffer[p++];
        if (p < q && buffer[p] == '"') {
          p++;
        }
      }
      return new ParsedCell(buffer, offset + 1, d - offset - 1);
    }

    // Returns the contents as a string from UTF-8 encoded bytes.
    public String toString() {
      return new String(buffer, offset, length, UTF_8);
    }

    double toDouble() throws NumberFormatException {
      return Double.parseDouble(unescape().toString());
    }
  }
}
