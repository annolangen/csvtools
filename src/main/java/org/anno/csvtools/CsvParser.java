package org.anno.csvtools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A parser of CSV files. For effciency, this class works at the byte level. It
 * allocates large buffers, where quoted cells are stripped of their quotes and
 * unescaped, when needed.
 */
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

  List<ParsedCell> nextRow() throws IOException {
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

    List<ParsedCell> row = new ArrayList<>();
    while (true) {
      if (p == limit) {
        return null;
      }
      int start = p;
      if (b[p] == '"') {
        p += 1;
        while (p < limit && (b[p] != '"' || (++p < limit && b[p] == '"'))) {
          p++;
        }
        row.add(new ParsedCell(b, start, p - start));
      } else {
        while (p < limit && b[p] != ',' && b[p] != '\n' && b[p] != '\r') {
          p++;
        }
        row.add(new ParsedCell(b, start, p - start));
      }
      if (p < limit && b[p] == ',') {
        p++;
      }
      if (p == limit || b[p] == '\n' || b[p] == '\r') {
        while (p < limit && (b[p] == '\n' || b[p] == '\r')) {
          p++;
        }
        currentBuffer.position(p);
        return row;
      }
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
