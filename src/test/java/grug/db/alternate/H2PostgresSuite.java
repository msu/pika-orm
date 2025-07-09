package grug.db.alternate;


import grug.db.TestBase;
import org.junit.platform.suite.api.*;

@Suite
@SuiteDisplayName("H2 Postgres Suite")
@SelectPackages({"grug.db"})
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
