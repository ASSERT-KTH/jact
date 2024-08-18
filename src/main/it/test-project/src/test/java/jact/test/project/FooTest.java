package jact.test.project;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FooTest {
    @Test
    public void barTest() {
        Foo foo = new Foo();
        assertTrue(foo.bar(11));
    }
}