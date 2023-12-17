package org.anno.csvtools;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length == 0 || args[0].equals("-h") || args[0].equals("-?")) {
      printHelp();
      System.exit(1);
      return;
    }
    if (args[0].equals("cut")) {
      CsvCut.main(args);
      return;
    }
  }

  private static void printHelp() {
    System.out.println("Usage: java Main [options]");
    System.out.println("Options:");
    System.out.println("-h, -?   Print this help message");
  }
}
