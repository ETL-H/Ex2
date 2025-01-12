package Code;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;

/**
 * JUnit tests for Ex2Sheet and SCell classes.
 */
public class Ex2SheetTests {

    private Ex2Sheet sheet;

    @BeforeEach
    void setUp() {
        sheet = new Ex2Sheet(10, 10); // Initialize a 10x10 sheet
    }

    @Test
    void testInitialization() {
        assertNotNull(sheet);
        assertEquals(10, sheet.width());
        assertEquals(10, sheet.height());
        assertEquals("", sheet.value(0, 0));
    }

    @Test
    void testSetValueAndRetrieve() {
        sheet.set(0, 0, "5");
        assertEquals("5.0", sheet.value(0, 0));

        sheet.set(1, 1, "Hello");
        assertEquals("Hello", sheet.value(1, 1));

        sheet.set(2, 2, "=1+2");
        assertEquals("3.0", sheet.value(2, 2));
    }

    @Test
    void testFormulaEvaluation() {
        sheet.set(0, 0, "5");
        sheet.set(0, 1, "10");
        sheet.set(0, 2, "=A0+A1");

        assertEquals("15.0", sheet.value(0, 2));
    }

    @Test
    void testErrorHandlingInvalidReference() {
        sheet.set(0, 0, "=Z1"); // Invalid reference
        assertEquals("ERR_FORM!", sheet.value(0, 0));
    }

    @Test
    void testErrorHandlingCycle() {
        sheet.set(0, 0, "=A1");
        sheet.set(0, 1, "=A0");
        assertEquals("ERR_CYCLE!", sheet.value(0, 0));
        assertEquals("ERR_CYCLE!", sheet.value(0, 1));
    }

    @Test
    void testSaveAndLoad() throws IOException {
        sheet.set(0, 0, "5");
        sheet.set(0, 1, "=A0+10");

        File file = new File("test_sheet.csv");
        sheet.save(file.getAbsolutePath());

        Ex2Sheet loadedSheet = new Ex2Sheet();
        loadedSheet.load(file.getAbsolutePath());

        assertEquals("5.0", loadedSheet.value(0, 0));
        assertEquals("15.0", loadedSheet.value(0, 1));

        file.delete(); // Cleanup test file
    }

    @Test
    void testComplexFormulas() {
        sheet.set(0, 0, "5");
        sheet.set(0, 1, "10");
        sheet.set(0, 2, "=A0*A1");
        assertEquals("50.0", sheet.value(0, 2));

        sheet.set(1, 0, "=A0+A1+A2");
        assertEquals("65.0", sheet.value(1, 0));
    }
}
