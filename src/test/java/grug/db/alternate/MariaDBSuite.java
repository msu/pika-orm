package grug.db.alternate;


import grug.db.TestBase;
import org.junit.platform.suite.api.*;

@Suite
@SuiteDisplayName("MariaDB Suite")
@SelectPackages({"grug.db"})
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
