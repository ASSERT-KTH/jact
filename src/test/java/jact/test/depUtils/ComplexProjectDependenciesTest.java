package jact.test.depUtils;

import jact.depUtils.ProjectDependency;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jact.depUtils.ProjectDependencies.getAllProjectDependencies;
import static jact.depUtils.ProjectDependencies.getIndirectUsageMap;
import static jact.depUtils.ProjectDependency.depToDirName;
import static jact.utils.FileSystemUtils.removeDirectory;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;


public class ComplexProjectDependenciesTest {
    static String testDirectory = "./src/test/java/jact/test/testingDir/";
    public static final String testResourcesDir = "./src/test/resources/complexLockfile/";

    public static String REPORTPATH = "./target/jact-report/";

    //All dependency IDs from the pre-defined lockfile for testing
    public static List<String> dependencyIds = Arrays.asList(
            "com.github.caciocavallosilano:cacio-tta:1.11",
            "com.github.caciocavallosilano:cacio-shared:1.11",
            "com.google.guava:guava:33.1.0-jre",
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.google.errorprone:error_prone_annotations:2.26.1",
            "com.google.guava:failureaccess:1.0.2",
            "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
            "com.google.j2objc:j2objc-annotations:3.0.0",
            "com.google.truth:truth:1.4.2",
            "com.google.auto.value:auto-value-annotations:1.10.4",
            "junit:junit:4.13.2",
            "org.hamcrest:hamcrest-core:1.3",
            "org.checkerframework:checker-qual:3.42.0",
            "org.ow2.asm:asm:9.6",
            "com.tngtech.archunit:archunit-junit5:1.2.1",
            "com.tngtech.archunit:archunit-junit5-api:1.2.1",
            "com.tngtech.archunit:archunit:1.2.1",
            "com.tngtech.archunit:archunit-junit5-engine:1.2.1",
            "com.tngtech.archunit:archunit-junit5-engine-api:1.2.1",
            "commons-beanutils:commons-beanutils:1.9.4",
            "commons-collections:commons-collections:3.2.2",
            "commons-logging:commons-logging:1.2",
            "commons-io:commons-io:2.15.1",
            "de.thetaphi:forbiddenapis:3.7",
            "info.picocli:picocli:4.7.5",
            "net.sf.saxon:Saxon-HE:12.4",
            "org.xmlresolver:xmlresolver:5.2.2",
            "org.apache.httpcomponents.client5:httpclient5:5.1.3",
            "org.apache.httpcomponents.core5:httpcore5:5.1.3",
            "net.bytebuddy:byte-buddy:1.14.12",
            "org.objenesis:objenesis:3.3",
            "org.antlr:antlr4-runtime:4.13.1",
            "org.apache.ant:ant:1.10.14",
            "org.apache.ant:ant-launcher:1.10.14",
            "org.apache.maven.doxia:doxia-core:1.12.0",
            "org.apache.commons:commons-lang3:3.8.1",
            "org.apache.commons:commons-text:1.3",
            "org.apache.httpcomponents:httpclient:4.5.13",
            "org.apache.httpcomponents:httpcore:4.4.14",
            "org.apache.maven.doxia:doxia-logging-api:1.12.0",
            "org.codehaus.plexus:plexus-container-default:2.1.0",
            "org.codehaus.plexus:plexus-component-annotations:2.1.0",
            "org.codehaus.plexus:plexus-utils:3.3.0",
            "org.apache.maven.doxia:doxia-module-xdoc:1.12.0",
            "org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r",
            "com.googlecode.javaewah:JavaEWAH:1.2.3",
            "commons-codec:commons-codec:1.16.0",
            "org.itsallcode:junit5-system-extensions:1.2.0",
            "org.jacoco:org.jacoco.agent:0.8.11",
            "org.junit-pioneer:junit-pioneer:2.2.0",
            "org.junit.jupiter:junit-jupiter-api:5.10.2",
            "org.junit.jupiter:junit-jupiter-engine:5.10.2",
            "org.junit.jupiter:junit-jupiter-params:5.9.2",
            "org.junit.platform:junit-platform-commons:1.10.2",
            "org.junit.platform:junit-platform-engine:1.10.2",
            "org.junit.platform:junit-platform-launcher:1.9.2",
            "org.opentest4j:opentest4j:1.3.0",
            "org.apiguardian:apiguardian-api:1.1.2",
            "org.mockito:mockito-inline:5.2.0",
            "org.mockito:mockito-core:5.2.0",
            "net.bytebuddy:byte-buddy-agent:1.14.1",
            "org.objenesis:objenesis:3.3",
            "org.reflections:reflections:0.10.2",
            "com.google.code.findbugs:jsr305:3.0.2",
            "org.javassist:javassist:3.28.0-GA",
            "org.slf4j:slf4j-api:1.7.32",
            "org.slf4j:slf4j-simple:2.0.12",
            "nl.jqno.equalsverifier:equalsverifier:3.16",
            "org.apache.httpcomponents.core5:httpcore5-h2:5.1.3",
            "org.apache.maven.doxia:doxia-sink-api:1.12.0",
            "org.codehaus.plexus:plexus-classworlds:2.6.0",
            "org.apache.xbean:xbean-reflect:3.7"
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

    public static List<String> removeDuplicates(List<String> list) {
        List<String> uniqueList = new ArrayList<>();
        for (String element : list) {
            if (!uniqueList.contains(element)) {
                uniqueList.add(element);
            }
        }
        return uniqueList;
    }

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
        dependencyIds = removeDuplicates(dependencyIds);
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
     *     Post-condition: All dependencies have a corresponding
     *                     ProjectDependency object in the dependencies map.
     *                     The corresponding object has the correct report path.
     */
    public void allDepIdsPresentTest() {
        // Check the number of dependencies
        assertEquals(68, dependencies.size());

        // Check that all dependencies are present
        for (Map.Entry<String, ProjectDependency> pdEntry : dependencies.entrySet()) {
            assertTrue(dependencyIds.contains(pdEntry.getValue().getId()));
            assertTrue(pdEntry.getValue().getReportPath().equals(REPORTPATH + "dependencies/" + depToDirName(pdEntry.getValue()) + "/"));
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
        assertEquals(30, getIndirectUsageMap().size());

        assertTrue(getIndirectUsageMap().containsKey("org.apache.maven.doxia:doxia-logging-api:1.12.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.apache.maven.doxia:doxia-sink-api:1.12.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.jupiter:junit-jupiter-engine:5.10.2"));
        assertTrue(getIndirectUsageMap().containsKey("com.tngtech.archunit:archunit-junit5-api:1.2.1"));
        assertTrue(getIndirectUsageMap().containsKey("org.apache.httpcomponents.client5:httpclient5:5.1.3"));
        assertTrue(getIndirectUsageMap().containsKey("org.apache.httpcomponents:httpclient:4.5.13"));
        assertTrue(getIndirectUsageMap().containsKey("com.github.caciocavallosilano:cacio-tta:1.11"));
        assertTrue(getIndirectUsageMap().containsKey("org.xmlresolver:xmlresolver:5.2.2"));
        assertTrue(getIndirectUsageMap().containsKey("com.tngtech.archunit:archunit-junit5:1.2.1"));
        assertTrue(getIndirectUsageMap().containsKey("org.apache.httpcomponents.core5:httpcore5-h2:5.1.3"));
        assertTrue(getIndirectUsageMap().containsKey("com.google.truth:truth:1.4.2"));
        assertTrue(getIndirectUsageMap().containsKey("commons-beanutils:commons-beanutils:1.9.4"));
        assertTrue(getIndirectUsageMap().containsKey("junit:junit:4.13.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.apache.maven.doxia:doxia-module-xdoc:1.12.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.platform:junit-platform-engine:1.10.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.jupiter:junit-jupiter-params:5.9.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r"));
        assertTrue(getIndirectUsageMap().containsKey("org.mockito:mockito-inline:5.2.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit-pioneer:junit-pioneer:2.2.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.mockito:mockito-core:5.2.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.platform:junit-platform-commons:1.10.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.codehaus.plexus:plexus-container-default:2.1.0"));
        assertTrue(getIndirectUsageMap().containsKey("nl.jqno.equalsverifier:equalsverifier:3.16"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.jupiter:junit-jupiter-api:5.10.2"));
        assertTrue(getIndirectUsageMap().containsKey("com.google.guava:guava:33.1.0-jre"));
        assertTrue(getIndirectUsageMap().containsKey("com.tngtech.archunit:archunit-junit5-engine:1.2.1"));
        assertTrue(getIndirectUsageMap().containsKey("net.sf.saxon:Saxon-HE:12.4"));
        assertTrue(getIndirectUsageMap().containsKey("org.junit.platform:junit-platform-launcher:1.9.2"));
        assertTrue(getIndirectUsageMap().containsKey("org.apache.maven.doxia:doxia-core:1.12.0"));
        assertTrue(getIndirectUsageMap().containsKey("org.reflections:reflections:0.10.2"));
    }

}
