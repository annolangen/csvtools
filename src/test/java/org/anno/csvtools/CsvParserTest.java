package org.anno.csvtools;

import org.anno.csvtools.CsvParser.ParsedCell;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static com.google.common.truth.Truth.assertThat;

public class CsvParserTest {

  CsvParser parserFor(String csv) throws IOException {
    return new CsvParser(Channels.newChannel(new ByteArrayInputStream(csv.getBytes(UTF_8))));
  }

  @Test
  public void testCommaSeparatedCells() throws IOException {
    CsvParser parser = parserFor("cell1,666\n\ra21,a22");
    List<ParsedCell> row1 = parser.nextRow();
    assertThat(row1.stream().map(Object::toString).toList()).containsExactly("cell1", "666");
    assertThat(row1.get(1).toDouble()).isEqualTo(666);

    assertThat(parser.nextRow().stream().map(Object::toString).toList()).containsExactly("a21", "a22");
    assertThat(parser.nextRow()).isNull();
  }

  @Test
  public void testQuotedCells() throws IOException {
    CsvParser parser = parserFor("\"line1\nfirst, second\",\"27\"\" - 32\"\" TVs\"\n");
    List<ParsedCell> row = parser.nextRow();
    assertThat(row).hasSize(2);
    assertThat(row.get(0).toString()).isEqualTo("\"line1\nfirst, second\"");
    assertThat(row.get(0).unescape().toString()).isEqualTo("line1\nfirst, second");
    assertThat(row.get(1).unescape().toString()).isEqualTo("27\" - 32\" TVs");
    assertThat(parser.nextRow()).isNull();
  }

  @Test
  public void testEmptyCells() throws IOException {
    CsvParser parser = parserFor("cell1,,cell3\n");
    List<ParsedCell> row = parser.nextRow();
    assertThat(row.stream().map(Object::toString).toList()).containsExactly("cell1", "", "cell3");
    assertThat(parser.nextRow()).isNull();
  }
}