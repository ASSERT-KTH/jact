package jact.test.depUtils;

import jact.depUtils.ProjectDependency;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jact.depUtils.ProjectDependencies.getAllProjectDependencies;
import static jact.depUtils.ProjectDependencies.getIndirectUsageMap;
import static jact.depUtils.ProjectDependency.depToDirName;
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
    public static void cleanUpTestDirs() {
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
    public static void initTestDependencies() {
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
    public void allDepIdsPresentTest() {
        // Check the number of dependencies
        assertEquals(15, dependencies.size());

        // Check that all dependencies are present
        for (Map.Entry<String, ProjectDependency> pdEntry : dependencies.entrySet()) {
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
    public void parentAndChildrenDepsTest() {
        // Check that Google Guavas children dependencies are correct
        for (ProjectDependency childDep : dependencies.get("com.google.guava:guava:33.0.0-jre").getChildDeps().values()) {
            assertTrue(googleGuavaChildrenDeps.contains(childDep.getId()));
            // Check that the parent-list contains Google Guava
            boolean foundChildDep = false;
            for (ProjectDependency deps : childDep.getParentDeps().values()) {
                if (deps.getId().equals(dependencies.get("com.google.guava:guava:33.0.0-jre").getId())) {
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
    public void multipleTransitiveDepsTest() {
        // Check that "org.junit.platform:junit-platform-commons:1.10.2"
        // is the second child in "org.junit.jupiter:junit-jupiter-api:5.10.2"
        assertTrue(dependencies.get("org.junit.jupiter:junit-jupiter-api:5.10.2").getChildDeps().get("org.junit.platform:junit-platform-commons:1.10.2").getId().equals("org.junit.platform:junit-platform-commons:1.10.2"));
        assertEquals(3, dependencies.get("org.junit.jupiter:junit-jupiter-api:5.10.2").getChildDeps().size());
        // Check that "org.junit.platform:junit-platform-commons:1.10.2"
        // has the correct child: "org.apiguardian:apiguardian-api:1.1.2"
        assertTrue(dependencies.get("org.junit.platform:junit-platform-commons:1.10.2").getChildDeps().get("org.apiguardian:apiguardian-api:1.1.2").getId().equals("org.apiguardian:apiguardian-api:1.1.2"));
        assertEquals(1, dependencies.get("org.junit.platform:junit-platform-commons:1.10.2").getChildDeps().size());

        // Check that "org.apiguardian:apiguardian-api:1.1.2" has an empty 'children' list:
        assertTrue(dependencies.get("org.apiguardian:apiguardian-api:1.1.2").getChildDeps().isEmpty());
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
    public void properFieldValuesTest() {
        // Check that "org.apache.commons:commons-math3:3.6.1" has correct field values
        assertTrue(dependencies.get("org.apache.commons:commons-math3:3.6.1").getId().equals("org.apache.commons:commons-math3:3.6.1"));
        assertTrue(dependencies.get("org.apache.commons:commons-math3:3.6.1").getGroupId().equals("org.apache.commons"));
        assertTrue(dependencies.get("org.apache.commons:commons-math3:3.6.1").getArtifactId().equals("commons-math3"));
        assertTrue(dependencies.get("org.apache.commons:commons-math3:3.6.1").getVersion().equals("3.6.1"));
        assertTrue(dependencies.get("org.apache.commons:commons-math3:3.6.1").getScope().equals("compile"));

        // Check that "com.google.guava:guava:33.0.0-jre" has correct field values
        assertTrue(dependencies.get("com.google.guava:guava:33.0.0-jre").getId().equals("com.google.guava:guava:33.0.0-jre"));
        assertTrue(dependencies.get("com.google.guava:guava:33.0.0-jre").getGroupId().equals("com.google.guava"));
        assertTrue(dependencies.get("com.google.guava:guava:33.0.0-jre").getArtifactId().equals("guava"));
        assertTrue(dependencies.get("com.google.guava:guava:33.0.0-jre").getVersion().equals("33.0.0-jre"));
        assertTrue(dependencies.get("com.google.guava:guava:33.0.0-jre").getScope().equals("compile"));
    }


    @Test
    /**
     * Requirements: See `initTestDependencies()`.
     * Contract:
     *      Pre-condition: A valid lockfile.json from
     *                     test resources has generated all defined
     *                     dependencies to a list of ProjectDependency
     *                     objects.
     *     Post-condition: All ProjectDependencies have the correct
     *                     report path set.
     */
    public void reportPathsTest() {
        // Brute force check that all dependencies have their
        // manually checked report paths
        for (String depId : dependencyIds) {
            ProjectDependency currDep = dependencies.get(depId);
            assertEquals(REPORTPATH + "dependencies/" + depToDirName(currDep) + "/", currDep.getReportPath());
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
     *     Post-condition: All ProjectDependencies with children
     *                     have a corresponding entry in the
     *                     indirectUsageMap.
     */
    public void indirectReportPathsTest() {
        assertEquals(4, getIndirectUsageMap().size());
        assertTrue(getIndirectUsageMap().containsKey("com.google.guava:guava:33.0.0-jre"));
        assertTrue(getIndirectUsageMap().containsKey("junit:junit:4.13.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.jupiter:junit-jupiter-api:5.10.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.platform:junit-platform-commons:1.10.2"));
    }

}
