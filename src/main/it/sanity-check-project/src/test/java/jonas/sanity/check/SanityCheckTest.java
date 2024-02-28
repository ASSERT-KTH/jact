package jonas.sanity.check;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


public class SanityCheckTest
{
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testMain(){
        SanityCheck SC = new SanityCheck();
        SC.main(new String[0]);
    }
}
