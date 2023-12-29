package org.anno.csvtools;

import java.io.PrintStream;

public class Main {

  private final DelegatedMain csvCut;
  private final ExitHandler exitHandler;
  private final PrintStream out;

  public Main(DelegatedMain csvCut, ExitHandler exitHandler, PrintStream out) {
    this.csvCut = csvCut;
    this.exitHandler = exitHandler;
    this.out = out;
  }

  public static void main(String[] args) throws Exception {
    new Main(DelegatedMain.CSV_CUT, ExitHandler.SYSTEM, System.out).run(args);
  }

  // VisibleForTesting
  void run(String[] args) throws Exception {
    if (args.length == 0 || args[0].equals("-h") || args[0].equals("-?")) {
      printHelp();
      exitHandler.exit(1);
      return;
    }
    if (args[0].equals("cut")) {
      csvCut.run(args);
      return;
    }
  }

  private void printHelp() {
    out.println("Usage: csvtools (cut|explain)? [options]");
    out.println("Options:");
    out.println("-h, -?   Print this help message");
  }

  interface ExitHandler {
    void exit(int status);

    static ExitHandler SYSTEM = status -> System.exit(status);
  }

  interface DelegatedMain {
    void run(String[] args) throws Exception;

    static DelegatedMain CSV_CUT = args -> CsvCut.main(args);
  }
}
