package edu.montana.pika.alternate;


import edu.montana.pika.TestBase;
import org.junit.platform.suite.api.*;

@Suite
@SuiteDisplayName("H2 SQLServer Suite")
@SelectPackages({"bigsky.pika"})
@ExcludeClassNamePatterns({".*ErrorsTest.*", ".*Chinook.*", ".*MigrationsTest"})
public class H2SqlServerSuite {

    @BeforeSuite
    public static void before() {
        TestBase.setMode(TestBase.DatabaseMode.H2_SQLSERVER);
    }

    @AfterSuite
    public static void after() {
        TestBase.setMode(TestBase.DatabaseMode.SQLITE);
    }


}
