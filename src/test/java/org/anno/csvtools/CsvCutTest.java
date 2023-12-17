package org.anno.csvtools;

import org.anno.csvtools.CsvCut.FileOpener;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Optional;

class CsvCutTest {

  FileOpener getFileOpener(String csv) {
    return (ignoredFilename) -> Optional.of(Channels.newChannel(new ByteArrayInputStream(csv.getBytes(UTF_8))));
  }

  ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
  PrintStream testOut = new PrintStream(testOutput);

  @Test
  void testGetCutSpecParsesExplicitIndices() throws Exception {
    CsvCut.CutSpec cutSpec = CsvCut.getCutSpec(new String[] { "cut", "-K", "1,4", "test.csv" }, testOut,
        getFileOpener("h1,h2\na11,a12\na21,a22\n"));

    assertThat(cutSpec).isNotNull();
    assertThat(cutSpec.in).isNotNull();
    assertThat(cutSpec.columFunction.apply(Arrays.asList())).isEqualTo(new int[] { 0, 3 });
  }

  @Test
  void testGetCutSpecEmptyHasStdinAndIndicesDependOnHeader() throws Exception {
    CsvCut.CutSpec cutSpec = CsvCut.getCutSpec(new String[] {}, testOut,
        getFileOpener("h1,h2,h3\na11,a12,a13\n"));

    assertThat(cutSpec).isNotNull();
    assertThat(cutSpec.in).isNotNull();
    assertThat(cutSpec.columFunction.apply(Arrays.asList("h1", "h2", "h3"))).isEqualTo(new int[] { 0, 1, 2 });
  }

  @Test
  void testSwapsColumns() throws Exception {
    CsvCut.doCut(CsvCut.getCutSpec(new String[] { "cut", "-K", "2,1" }, testOut,
        getFileOpener("h1,h2\na11,a12\na21,a22\n")), testOut);

    assertThat(testOutput.toString(UTF_8.name())).isEqualTo("h2,h1\na12,a11\na22,a21\n");
  }
}