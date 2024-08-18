package jact.test.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class MainClassTest {
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testMain() {
        MainClass MC = new MainClass();
        MC.main(new String[0]);
    }
}
