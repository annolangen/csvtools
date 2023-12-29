package org.anno.csvtools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.anno.csvtools.CsvParser.ParsedCell;

/**
 * A command line tool to cut columns from a CSV file. The first row is assumed to contain the
 * headers. The output preserves the distinction between quoted and unquoted cells.
 */
public class CsvCut {
  public static void main(String[] args) {
    try {
      doCut(getCutSpec(args, System.out, FileOpener.REGULAR, System::exit), System.out);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  // VisibleForTesting
  static void doCut(CutSpec spec, PrintStream out) throws IOException {
    CsvParser parser = new CsvParser(spec.in);
    List<ParsedCell> rawHeaders = parser.nextRow();
    List<String> headers =
        rawHeaders.stream().map(ParsedCell::unescape).map(Object::toString).toList();
    RowProcessor columnPrinter = RowProcessor.printColumns(spec.columFunction.apply(headers), out);
    columnPrinter.process(rawHeaders);
    for (List<ParsedCell> row = parser.nextRow(); row != null; row = parser.nextRow()) {
      columnPrinter.process(row);
    }
  }

  interface RowProcessor {
    void process(List<ParsedCell> row);

    static RowProcessor printColumns(int[] columns, PrintStream out) {
      return (List<ParsedCell> row) -> {
        String sep = "";
        for (int i : columns) {
          out.print(sep);
          out.print(i < row.size() ? row.get(i) : "");
          sep = ",";
        }
        out.println();
      };
    }
  }

  // VisibleForTesting
  static CutSpec getCutSpec(String[] args, PrintStream out, FileOpener openFile, ExitFn exitFn)
      throws IOException {
    ReadableByteChannel channel = Channels.newChannel(System.in);
    Function<List<String>, int[]> columnFn =
        (List<String> headers) -> IntStream.range(0, headers.size()).toArray();
    // args[0] == "cut"
    for (int i = 1; i < args.length; i++) {
      if (args[i].equals("-K") && i + 1 < args.length) {
        String toKeep = args[++i];
        columnFn =
            (ignored) ->
                Stream.of(toKeep.split(",")).mapToInt(Integer::parseInt).map(k -> k - 1).toArray();
      }
      if (args[i].equals("-k") && i + 1 < args.length) {
        String toKeep = args[++i];
        columnFn =
            (headers) ->
                Stream.of(toKeep.split(","))
                    .mapToInt(h -> headers.indexOf(h))
                    .filter(k -> k > 0)
                    .toArray();
      }
      if (i == args.length - 1) {
        Optional<ReadableByteChannel> maybeChannel = openFile.openFile(args[i]);
        if (maybeChannel.isPresent()) {
          channel = maybeChannel.get();
        }
      }
      if (args[i].equals("-h") || args[i].equals("-?")) {
        printHelp(out);
        exitFn.exit(1);
        return null;
      }
    }
    return new CutSpec(columnFn, channel);
  }

  private static void printHelp(PrintStream out) {
    out.println("Usage: csvcut [options] [file]");
    out.println("Options:");
    out.println("-K 1,4  Keep columns 1 and 4");
    out.println("-h, -?   Print this help message");
  }

  static class CutSpec {
    Function<List<String>, int[]> columFunction;
    ReadableByteChannel in;

    CutSpec(Function<List<String>, int[]> columFunction, ReadableByteChannel in) {
      this.columFunction = columFunction;
      this.in = in;
    }
  }

  public interface FileOpener {
    Optional<ReadableByteChannel> openFile(String path);

    static FileOpener REGULAR =
        (path) -> {
          return Optional.of(Path.of(path))
              .filter(p -> p.toFile().exists())
              .map(
                  p -> {
                    try {
                      return FileChannel.open(p);
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  });
        };
  }

  public interface ExitFn {
    void exit(int status);
  }
}
