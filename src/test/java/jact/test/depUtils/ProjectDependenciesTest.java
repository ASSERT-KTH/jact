package jact.test.depUtils;

import jact.depUtils.ProjectDependencies;

import jact.depUtils.ProjectDependency;
import jact.plugin.AbstractReportMojo;
import jact.utils.CommandExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
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
    String OS = System.getProperty("os.name");

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

    @AfterAll
    public static void cleanUpTestDirs(){
        removeDirectory(new File(testDirectory));
        Assertions.assertFalse(new File(testDirectory).exists());
    }

    @Test
    public void generateAllDependenciesTest(){
        CommandExecutor cmdExecTest = new CommandExecutor(OS);
        assertTrue(new File(testResourcesDir + "lockfile.json").exists());
        List<ProjectDependency> dependencies = getAllProjectDependencies(cmdExecTest, testResourcesDir, false);



    }

}
