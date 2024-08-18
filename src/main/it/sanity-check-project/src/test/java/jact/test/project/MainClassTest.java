package jact.test.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class SanityCheckTest {
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testMain() {
        MainClass SC = new MainClass();
        SC.main(new String[0]);
    }
}
