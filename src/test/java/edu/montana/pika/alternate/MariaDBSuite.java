package edu.montana.pika.alternate;


import edu.montana.pika.TestBase;
import org.junit.platform.suite.api.*;

@Suite
@SuiteDisplayName("MariaDB Suite")
@SelectPackages({"edu.montana.pika"})
@ExcludeClassNamePatterns({".*ErrorsTest.*", ".*Chinook.*", ".*MigrationsTest"})
public class MariaDBSuite {

    @BeforeSuite
    public static void before() {
        TestBase.setMode(TestBase.DatabaseMode.MARIADB);
    }

    @AfterSuite
    public static void after() {
        TestBase.setMode(TestBase.DatabaseMode.SQLITE);
    }


}
