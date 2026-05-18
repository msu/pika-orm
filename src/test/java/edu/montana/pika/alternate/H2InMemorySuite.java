package edu.montana.pika.alternate;


import edu.montana.pika.TestBase;
import org.junit.platform.suite.api.*;

@Suite
@SuiteDisplayName("H2 In Memory Suite")
@SelectPackages({"bigsky.pika"})
@ExcludeClassNamePatterns({".*ErrorsTest.*", ".*Chinook.*", ".*MigrationsTest"})
public class H2InMemorySuite {

    @BeforeSuite
    public static void before() {
        TestBase.setMode(TestBase.DatabaseMode.H2);
    }

    @AfterSuite
    public static void after() {
        TestBase.setMode(TestBase.DatabaseMode.SQLITE);
    }


}
