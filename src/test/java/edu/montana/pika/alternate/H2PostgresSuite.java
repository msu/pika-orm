package edu.montana.pika.alternate;


import edu.montana.pika.TestBase;
import org.junit.platform.suite.api.*;

@Suite
@SuiteDisplayName("H2 Postgres Suite")
@SelectPackages({"edu.montana.pika"})
@ExcludeClassNamePatterns({".*ErrorsTest.*", ".*Chinook.*", ".*MigrationsTest"})
public class H2PostgresSuite {

    @BeforeSuite
    public static void before() {
        TestBase.setMode(TestBase.DatabaseMode.H2_POSTGRES);
    }

    @AfterSuite
    public static void after() {
        TestBase.setMode(TestBase.DatabaseMode.SQLITE);
    }


}
