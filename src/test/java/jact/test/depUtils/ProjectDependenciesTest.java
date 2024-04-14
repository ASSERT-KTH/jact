package jact.test.depUtils;

import jact.depUtils.ProjectDependency;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jact.depUtils.ProjectDependencies.getAllProjectDependencies;
import static jact.depUtils.ProjectDependencies.transitiveUsageMap;
import static jact.utils.FileSystemUtils.removeDirectory;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ProjectDependenciesTest {
    static String testDirectory = "./src/test/java/jact/test/testingDir/";
    public static final String testResourcesDir = "./src/test/resources/";

    public static String REPORTPATH = "./target/jact-report/";


    //All dependency IDs from the pre-defined lockfile for testing
    public static final List<String> dependencyIds = Arrays.asList(
            "com.google.guava:guava:33.0.0-jre",
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.google.errorprone:error_prone_annotations:2.23.0",
            "com.google.guava:failureaccess:1.0.2",
            "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
            "com.google.j2objc:j2objc-annotations:2.8",
            "org.checkerframework:checker-qual:3.41.0",
            "joda-time:joda-time:2.12.7",
            "junit:junit:4.13.2",
            "org.hamcrest:hamcrest-core:1.3",
            "org.apache.commons:commons-math3:3.6.1",
            "org.junit.jupiter:junit-jupiter-api:5.10.2",
            "org.apiguardian:apiguardian-api:1.1.2",
            "org.junit.platform:junit-platform-commons:1.10.2",
            "org.opentest4j:opentest4j:1.3.0"
    );

    // All children dependencies of Google Guava
    public static final List<String> googleGuavaChildrenDeps = Arrays.asList(
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.google.errorprone:error_prone_annotations:2.23.0",
            "com.google.guava:failureaccess:1.0.2",
            "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
            "com.google.j2objc:j2objc-annotations:2.8",
            "org.checkerframework:checker-qual:3.41.0"
    );

    public static Map<String, ProjectDependency> dependencies;

    @AfterAll
    public static void cleanUpTestDirs(){
        removeDirectory(new File(testDirectory));
        Assertions.assertFalse(new File(testDirectory).exists());
    }

    @BeforeAll
    /**
     * Generates the test-project Dependencies from the
     * pre-defined lockfile. The dependencies are represented
     * by ProjectDependency objects which contain all the required
     * fields for identification and recording usage.
     */
    public static void initTestDependencies(){
        assertTrue(new File(testResourcesDir + "lockfile.json").exists());
        dependencies = getAllProjectDependencies(testResourcesDir, false, false);
    }

    @Test
    /**
     * Requirements: See `initTestDependencies()`.
     * Contract:
     *      Pre-condition: A valid lockfile.json from
     *                     test resources has generated all defined
     *                     dependencies to a list of ProjectDependency
     *                     objects.
     *     Post-condition: There is a total of 16 dependencies.
     *                     All dependencies are present with their
     *                     unique ID in the pre-defined list of IDs.
     */
    public void allDepIdsPresentTest(){
        // Check the number of dependencies
        assertEquals(15, dependencies.size());

        // Check that all dependencies are present
        for (Map.Entry<String, ProjectDependency> pdEntry : dependencies.entrySet()){
            assertTrue(dependencyIds.contains(pdEntry.getValue().getId()));
        }
    }

    @Test
    /**
     * Requirements: See `initTestDependencies()`.
     * Contract:
     *      Pre-condition: A valid lockfile.json from
     *                     test resources has generated all defined
     *                     dependencies to a list of ProjectDependency
     *                     objects.
     *     Post-condition: All child dependencies of Google Guava are
     *                     correctly generated and the children mention
     *                     Google Guava in their `parents` list.
     */
    public void parentAndChildrenDepsTest(){
        // Check that Google Guavas children dependencies are correct
        for(ProjectDependency childDep : dependencies.get("com.google.guava.guava-v33.0.0-jre").getChildDeps().values()){
            assertTrue(googleGuavaChildrenDeps.contains(childDep.getId()));
            // Check that the parent-list contains Google Guava
            boolean foundChildDep = false;
            for(ProjectDependency deps : childDep.getParentDeps().values()){
                if(deps.getId().equals(dependencies.get("com.google.guava.guava-v33.0.0-jre").getId())){
                    foundChildDep = true;
                }
            }
            assertTrue(foundChildDep);
        }
    }

    @Test
    /**
     * Requirements: See `initTestDependencies()`.
     * Contract:
     *      Pre-condition: A valid lockfile.json from
     *                     test resources has generated all defined
     *                     dependencies to a list of ProjectDependency
     *                     objects.
     *     Post-condition: Transitive dependencies of
     *                     "org.junit.platform:junit-platform-commons:1.10.2"
     *                     are correctly generated and correctly defined in
     *                     the chain of child-dependencies.
     */
    public void multipleTransitiveDepsTest(){
        // Check that "org.junit.platform:junit-platform-commons:1.10.2"
        // is the second child in "org.junit.jupiter:junit-jupiter-api:5.10.2"
        assertTrue(dependencies.get("org.junit.jupiter.junit.jupiter.api-v5.10.2").getChildDeps().get("org.junit.platform:junit-platform-commons:1.10.2").getId().equals("org.junit.platform:junit-platform-commons:1.10.2"));
        assertEquals(3, dependencies.get("org.junit.jupiter.junit.jupiter.api-v5.10.2").getChildDeps().size());
        // Check that "org.junit.platform:junit-platform-commons:1.10.2"
        // has the correct child: "org.apiguardian:apiguardian-api:1.1.2"
        assertTrue(dependencies.get("org.junit.platform.junit.platform.commons-v1.10.2").getChildDeps().get("org.apiguardian:apiguardian-api:1.1.2").getId().equals("org.apiguardian:apiguardian-api:1.1.2"));
        assertEquals(1, dependencies.get("org.junit.platform.junit.platform.commons-v1.10.2").getChildDeps().size());

        // Check that "org.apiguardian:apiguardian-api:1.1.2" has an empty 'children' list:
        assertTrue(dependencies.get("org.apiguardian.apiguardian.api-v1.1.2").getChildDeps().isEmpty());
    }

    @Test
    /**
     * Requirements: See `initTestDependencies()`.
     * Contract:
     *      Pre-condition: A valid lockfile.json from
     *                     test resources has generated all defined
     *                     dependencies to a list of ProjectDependency
     *                     objects.
     *     Post-condition: Created ProjectDependency objects have the
     *                     correct corresponding values within their fields.
     */
    public void properFieldValuesTest(){
        // Check that "org.apache.commons:commons-math3:3.6.1" has correct field values
        assertTrue(dependencies.get("org.apache.commons.commons.math3-v3.6.1").getId().equals("org.apache.commons:commons-math3:3.6.1"));
        assertTrue(dependencies.get("org.apache.commons.commons.math3-v3.6.1").getGroupId().equals("org.apache.commons"));
        assertTrue(dependencies.get("org.apache.commons.commons.math3-v3.6.1").getArtifactId().equals("commons-math3"));
        assertTrue(dependencies.get("org.apache.commons.commons.math3-v3.6.1").getVersion().equals("3.6.1"));
        assertTrue(dependencies.get("org.apache.commons.commons.math3-v3.6.1").getScope().equals("compile"));

        // Check that "com.google.guava:guava:33.0.0-jre" has correct field values
        assertTrue(dependencies.get("com.google.guava.guava-v33.0.0-jre").getId().equals("com.google.guava:guava:33.0.0-jre"));
        assertTrue(dependencies.get("com.google.guava.guava-v33.0.0-jre").getGroupId().equals("com.google.guava"));
        assertTrue(dependencies.get("com.google.guava.guava-v33.0.0-jre").getArtifactId().equals("guava"));
        assertTrue(dependencies.get("com.google.guava.guava-v33.0.0-jre").getVersion().equals("33.0.0-jre"));
        assertTrue(dependencies.get("com.google.guava.guava-v33.0.0-jre").getScope().equals("compile"));
    }


    @Test
    public void reportPathsTest(){
        // Brute force check that all dependencies have their
        // manually checked report paths
        ProjectDependency currDep = dependencies.get("com.google.guava.guava-v33.0.0-jre");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/", currDep.getReportPaths().get(0));


        currDep = dependencies.get("com.google.code.findbugs.jsr305-v3.0.2");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/" +
                        "com.google.code.findbugs.jsr305-v3.0.2/",
                currDep.getReportPaths().get(0));


        currDep = dependencies.get("com.google.errorprone.error_prone_annotations-v2.23.0");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/" +
                        "com.google.errorprone.error_prone_annotations-v2.23.0/",
                currDep.getReportPaths().get(0));


        currDep = dependencies.get("com.google.guava.failureaccess-v1.0.2");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/" +
                        "com.google.guava.failureaccess-v1.0.2/",
                currDep.getReportPaths().get(0));

        currDep = dependencies.get("com.google.guava.listenablefuture-v9999.0-empty-to-avoid-conflict-with-guava");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/" +
                        "com.google.guava.listenablefuture-v9999.0-empty-to-avoid-conflict-with-guava/",
                currDep.getReportPaths().get(0));


        currDep = dependencies.get("com.google.j2objc.j2objc.annotations-v2.8");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/" +
                        "com.google.j2objc.j2objc.annotations-v2.8/",
                currDep.getReportPaths().get(0));

        currDep = dependencies.get("org.checkerframework.checker.qual-v3.41.0");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/" +
                        "org.checkerframework.checker.qual-v3.41.0/",
                currDep.getReportPaths().get(0));


        currDep = dependencies.get("joda.time.joda.time-v2.12.7");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/joda.time.joda.time-v2.12.7/", currDep.getReportPaths().get(0));

        currDep = dependencies.get("junit.junit-v4.13.2");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/junit.junit-v4.13.2/", currDep.getReportPaths().get(0));


        currDep = dependencies.get("org.hamcrest.hamcrest.core-v1.3");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/junit.junit-v4.13.2/" +
                        "transitive-dependencies/" +
                        "org.hamcrest.hamcrest.core-v1.3/",
                currDep.getReportPaths().get(0));

        currDep = dependencies.get("org.apache.commons.commons.math3-v3.6.1");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/org.apache.commons.commons.math3-v3.6.1/", currDep.getReportPaths().get(0));

        currDep = dependencies.get("org.junit.jupiter.junit.jupiter.api-v5.10.2");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/", currDep.getReportPaths().get(0));

        currDep = dependencies.get("org.apiguardian.apiguardian.api-v1.1.2");
        assertEquals(2, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/" +
                        "transitive-dependencies/" +
                        "org.apiguardian.apiguardian.api-v1.1.2/",
                currDep.getReportPaths().get(0));
        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/" +
                        "transitive-dependencies/" +
                        "org.junit.platform.junit.platform.commons-v1.10.2/" +
                        "transitive-dependencies/" +
                        "org.apiguardian.apiguardian.api-v1.1.2/",
                currDep.getReportPaths().get(1));

        currDep = dependencies.get("org.junit.platform.junit.platform.commons-v1.10.2");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/" +
                        "transitive-dependencies/" +
                        "org.junit.platform.junit.platform.commons-v1.10.2/",
                currDep.getReportPaths().get(0));

        currDep = dependencies.get("org.opentest4j.opentest4j-v1.3.0");
        assertEquals(1, currDep.getReportPaths().size());
        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/" +
                        "transitive-dependencies/" +
                        "org.opentest4j.opentest4j-v1.3.0/",
                currDep.getReportPaths().get(0));
    }

    @Test
    public void transitiveReportPathsTest(){
        assertEquals(4, transitiveUsageMap.size());

        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/" +
                        "transitive-dependencies/",
                transitiveUsageMap.get("org.junit.jupiter.junit.jupiter.api-v5.10.2").getReportPaths().get(0));

        assertEquals(REPORTPATH + "dependencies/org.junit.jupiter.junit.jupiter.api-v5.10.2/" +
                        "transitive-dependencies/" +
                        "org.junit.platform.junit.platform.commons-v1.10.2/" +
                        "transitive-dependencies/",
                transitiveUsageMap.get("org.junit.platform.junit.platform.commons-v1.10.2").getReportPaths().get(0));

        assertEquals(REPORTPATH + "dependencies/junit.junit-v4.13.2/" +
                        "transitive-dependencies/",
                transitiveUsageMap.get("junit.junit-v4.13.2").getReportPaths().get(0));

        assertEquals(REPORTPATH + "dependencies/com.google.guava.guava-v33.0.0-jre/" +
                        "transitive-dependencies/",
                transitiveUsageMap.get("com.google.guava.guava-v33.0.0-jre").getReportPaths().get(0));
    }

}
