package org.anno.csvtools;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Optional;
import org.anno.csvtools.CsvCut.CutSpec;
import org.anno.csvtools.CsvCut.ExitFn;
import org.anno.csvtools.CsvCut.FileOpener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CsvCutTest {

  @Mock ExitFn exitFn;

  ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
  PrintStream testOut = new PrintStream(testOutput);

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  FileOpener getFileOpener(String csv) {
    return (ignoredFilename) ->
        Optional.of(Channels.newChannel(new ByteArrayInputStream(csv.getBytes(UTF_8))));
  }

  CutSpec doCut(String[] args, String csv) throws Exception {
    CutSpec spec = CsvCut.getCutSpec(args, testOut, getFileOpener(csv), exitFn);
    if (spec != null) {
      CsvCut.doCut(spec, testOut);
    }
    return spec;
  }

  @Test
  void testGetCutSpecParsesExplicitIndices() throws Exception {
    CutSpec spec =
        doCut(new String[] {"cut", "-K", "1,4", "test.csv"}, "h1,h2\na11,a12\na21,a22\n");

    assertThat(spec).isNotNull();
    assertThat(spec.in).isNotNull();
    assertThat(spec.columFunction.apply(Arrays.asList())).isEqualTo(new int[] {0, 3});
  }

  @Test
  void testIndicesDependOnHeader() throws Exception {
    CutSpec spec = doCut(new String[] {"cut", "test.csv"}, "h1,h2,h3\na11,a12,a13\n");

    assertThat(spec).isNotNull();
    assertThat(spec.in).isNotNull();
    assertThat(spec.columFunction.apply(Arrays.asList("h1", "h2", "h3")))
        .isEqualTo(new int[] {0, 1, 2});
  }

  @Test
  void testSwapsColumns() throws Exception {
    doCut(new String[] {"cut", "-K", "2,1"}, "h1,h2\na11,a12\na21,a22\n");

    assertThat(testOutput.toString(UTF_8)).isEqualTo("h2,h1\na12,a11\na22,a21\n");
  }

  @Test
  void testHelp() throws Exception {
    doCut(new String[] {"cut", "-h"}, "");

    assertThat(testOutput.toString(UTF_8))
        .isEqualTo(
            "Usage: csvcut [options] [file]\n"
                + "Options:\n"
                + "-K 1,4  Keep columns 1 and 4\n"
                + "-h, -?   Print this help message\n");
    verify(exitFn).exit(1);
  }
}
