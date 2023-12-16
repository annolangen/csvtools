package org.anno.csvtools;

import org.anno.csvtools.CsvParser.ParsedCell;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import static java.nio.charset.StandardCharsets.UTF_8;
import static com.google.common.truth.Truth.assertThat;

public class CsvParserTest {

  CsvParser parserFor(String csv) throws IOException {
    return new CsvParser(Channels.newChannel(new ByteArrayInputStream(csv.getBytes(UTF_8))));
  }

  @Test
  public void testCommaSeparatedCells() throws IOException {
    CsvParser parser = parserFor("cell1,666\n\ra21,a22");
    assertThat(parser.nextCell().toString()).isEqualTo("cell1");
    assertThat(parser.isEndOfRow()).isFalse();
    assertThat(parser.nextCell().toDouble()).isEqualTo(666);
    assertThat(parser.isEndOfRow()).isTrue();

    assertThat(parser.nextCell().toString()).isEqualTo("a21");
    assertThat(parser.isEndOfRow()).isFalse();
    assertThat(parser.nextCell().toString()).isEqualTo("a22");
    assertThat(parser.isEndOfRow()).isTrue();
  }

  @Test
  public void testQuotedCells() throws IOException {
    CsvParser parser = parserFor("\"line1\nfirst, second\",\"27\"\" - 32\"\" TVs\"\n");
    ParsedCell cell = parser.nextCell();
    assertThat(parser.isEndOfRow()).isFalse();
    assertThat(cell.toString()).isEqualTo("\"line1\nfirst, second\"");
    assertThat(cell.unescape().toString()).isEqualTo("line1\nfirst, second");

    assertThat(parser.nextCell().unescape().toString()).isEqualTo("27\" - 32\" TVs");
    assertThat(parser.isEndOfRow()).isTrue();
  }
}