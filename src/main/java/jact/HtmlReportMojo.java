package jact;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static jact.JacocoHTMLAugmenter.createDependencyReports;
import static jact.JacocoHTMLAugmenter.extractReportAndMoveDirs;


/**
 * Generates a complete code coverage report including all
 * dependencies along with their transitive dependencies.
 */
@Mojo(name = "html-report", defaultPhase = LifecyclePhase.INSTALL, threadSafe = false)
public class HtmlReportMojo extends AbstractMojo {

    private static String hostOS;
    private static String localRepoPath;
    private static String projectGroupId;
    private static String artifactId;
    private static String version;
    private static Map<String, Set<String>> packageClassMap = new HashMap<>();
    @Parameter(property = "scope")
    String scope;
    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;
    /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    public static String getHostOS() {
        return hostOS;
    }

    public static String getLocalRepoPath() {
        return localRepoPath;
    }

    public static String getProjectGroupId() {
        return projectGroupId;
    }

    public static String getProjectArtifactId() {
        return artifactId;
    }

    public static String getProjectVersion() {
        return version;
    }

    public static Map<String, Set<String>> getProjectPackagesAndClasses() {
        return packageClassMap;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        String outputJarName = project.getBuild().getFinalName();
        localRepoPath = session.getLocalRepository().getBasedir();
        hostOS = session.getSystemProperties().getProperty("os.name").toLowerCase();
        projectGroupId = project.getGroupId();
        artifactId = project.getArtifactId();
        version = project.getVersion();


        // Collect class names and package names
        collectClassNamesAndPackages();

        // Print out packages and their classes
        getLog().info("Packages in project:");
        for (Map.Entry<String, Set<String>> entry : packageClassMap.entrySet()) {
            getLog().info("- " + entry.getKey());
            for (String className : entry.getValue()) {
                getLog().info("  - " + className);
            }
        }


        getLog().info("STARTING: JACT - Java Complete Coverage Tracker");
        getLog().info("JARNAME: " + outputJarName + "-shaded");
        //String outputDirectory = project.getBuild().getOutputDirectory();

        List<ProjectDependency> projectDependencies = ProjectDependencies.getAllProjectDependencies();

        CommandExecutor cmdExec = new CommandExecutor();

        // Execute JaCoCoCLI to create the report WITH dependencies
        getLog().info("Copying the `jacococli.jar` to the project.");
        try {
            cmdExec.copyJacocoCliJar();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        getLog().info("Creating the complete coverage report.");
        cmdExec.executeJacocoCLI(outputJarName + "-shaded");

        getLog().info("Organizing the complete coverage report.");
        try {
            extractReportAndMoveDirs(projectDependencies);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createDependencyReports(projectDependencies);
        getLog().info("JACT Report Successfully Generated!");
    }

    private void collectClassNamesAndPackages() {
        String classesDirectory = project.getBuild().getOutputDirectory();
        scanForClassesAndPackages(new File(classesDirectory), "");
    }

    private void scanForClassesAndPackages(File directory, String parentPackage) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                String currentPackage = parentPackage.isEmpty() ? file.getName() : parentPackage + "." + file.getName();
                scanForClassesAndPackages(file, currentPackage);
            } else if (file.getName().endsWith(".class")) {
                // Extract package name from class file
                String packageName = parentPackage.replace(File.separator, ".");
                String className = file.getName().replace(".class", "");

                // Store class name in package map
                packageClassMap.computeIfAbsent(packageName, k -> new HashSet<>()).add(className);
            }
        }
    }
}