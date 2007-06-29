
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("All tests");
        suite.addTest(com.dumbhippo.AllTests.suite());
        suite.addTest(com.dumbhippo.identity20.AllTests.suite());
        suite.addTest(com.dumbhippo.persistence.AllTests.suite());        
        suite.addTest(com.dumbhippo.server.AllTests.suite());
        suite.addTest(com.dumbhippo.web.AllTests.suite());           
        return suite;
    }
}
