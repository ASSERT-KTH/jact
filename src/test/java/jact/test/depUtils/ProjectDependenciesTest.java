package jact.test.depUtils;

import jact.depUtils.ProjectDependencies;

import jact.depUtils.ProjectDependency;
import jact.plugin.AbstractReportMojo;
import jact.utils.CommandExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static jact.depUtils.DependencyUsage.barLength;
import static jact.depUtils.DependencyUsage.percentage;
import static jact.depUtils.ProjectDependencies.getAllProjectDependencies;
import static jact.utils.DirectoryUtils.removeDirectory;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ProjectDependenciesTest {
    static String testDirectory = "./src/test/java/jact/test/testingDir/";
    public static final String testResourcesDir = "./src/test/resources/";
    static String OS = System.getProperty("os.name");

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
            "org.apiguardian:apiguardian-api:1.1.2",
            "org.opentest4j:opentest4j:1.3.0"
    );

    public static final List<String> googleGuavaChildrenDeps = Arrays.asList(
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.google.errorprone:error_prone_annotations:2.23.0",
            "com.google.guava:failureaccess:1.0.2",
            "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
            "com.google.j2objc:j2objc-annotations:2.8",
            "org.checkerframework:checker-qual:3.41.0"
    );

    public static List<ProjectDependency> dependencies;

    @AfterAll
    public static void cleanUpTestDirs(){
        removeDirectory(new File(testDirectory));
        Assertions.assertFalse(new File(testDirectory).exists());
    }

    @BeforeAll
    public static void initTestDependencies(){
        CommandExecutor cmdExecTest = new CommandExecutor(OS);
        assertTrue(new File(testResourcesDir + "lockfile.json").exists());
        dependencies = getAllProjectDependencies(cmdExecTest, testResourcesDir, false);
    }

    @Test
    public void allDepIdsPresentTest(){
        // Check the number of dependencies
        assertEquals(16, dependencies.size());

        // Check that all dependencies are present
        for(ProjectDependency pd : dependencies){
            assertTrue(dependencyIds.contains(pd.getId()));
        }
    }

    @Test
    public void parentAndChildrenDepsTest(){
        // Check that Google Guavas children dependencies are correct
        for(ProjectDependency childDep : dependencies.get(0).getChildDeps()){
            assertTrue(googleGuavaChildrenDeps.contains(childDep.getId()));
            // Check that the parent is Google Guava
            boolean foundChildDep = false;
            for(ProjectDependency deps : childDep.getParentDeps()){
                if(deps.getId().equals(dependencies.get(0).getId())){
                    foundChildDep = true;
                }
            }
            assertTrue(foundChildDep);
        }
    }

    @Test
    public void multipleTransitiveDepsTest(){
        // Check that "org.junit.platform:junit-platform-commons:1.10.2"
        // is the second child in "org.junit.jupiter:junit-jupiter-api:5.10.2"
        System.out.println(dependencies.get(13).getId());
        assertTrue(dependencies.get(11).getChildDeps().get(1).getId().equals("org.junit.platform:junit-platform-commons:1.10.2"));
        // Check that "org.junit.platform:junit-platform-commons:1.10.2"
        // has the correct child: "org.apiguardian:apiguardian-api:1.1.2"
        assertTrue(dependencies.get(13).getChildDeps().get(0).getId().equals("org.apiguardian:apiguardian-api:1.1.2"));
        // Check that "org.apiguardian:apiguardian-api:1.1.2" has an empty 'children' list:
        assertTrue(dependencies.get(13).getChildDeps().get(0).getChildDeps().isEmpty());
    }

}
