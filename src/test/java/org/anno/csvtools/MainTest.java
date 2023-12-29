package org.anno.csvtools;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MainTest {

  @Mock
  Main.ExitHandler exitHandler;
  @Mock
  Main.DelegatedMain csvCut;
  private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private PrintStream testOut = new PrintStream(outContent);

  private void run(String[] args) throws Exception {
    new Main(csvCut, exitHandler, testOut).run(args);
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testHelpOption() throws Exception {
    String[] args = { "-h" };
    run(args);
    verify(exitHandler).exit(1);
    assertThat(outContent.toString()).contains("Usage: csvtools (cut|explain)? [options]");
  }

  @Test
  void testCutFunction() throws Exception {
    String[] args = { "cut" };
    run(args);
    verify(csvCut).run(args);
  }
}
