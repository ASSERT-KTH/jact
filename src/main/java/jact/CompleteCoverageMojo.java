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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jact.JacocoHTMLAugmenter.createDependencyReports;
import static jact.JacocoHTMLAugmenter.extractReportAndMoveDirs;


/**
 * Generates a complete code coverage report including all
 * dependencies along with their transitive dependencies.
 */
@Mojo(name = "coverage-report", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class CompleteCoverageMojo extends AbstractMojo {

    private static String hostOS;
    private static String localRepoPath;
    private static String projectGroupId;
    private static String artifactId;
    private static String version;
    private static Set<String> projectPackages = new HashSet<>();
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

    public static Set<String> getProjectPackages() {
        return projectPackages;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        String outputJarName = project.getBuild().getFinalName();
        localRepoPath = session.getLocalRepository().getBasedir();
        hostOS = session.getSystemProperties().getProperty("os.name").toLowerCase();
        projectGroupId = project.getGroupId();
        artifactId = project.getArtifactId();
        version = project.getVersion();


        // Get all project packages to correctly add them in the report.
        for (Object sourceRoot : project.getCompileSourceRoots()) {
            scanForPackageNames(new File(sourceRoot.toString()), projectPackages, "");
        }

        // Print out packages
        getLog().info("Packages in project:");
        for (String packageName : projectPackages) {
            getLog().info("- " + packageName);
        }


        getLog().info("STARTING: JACT - Java Complete Coverage Tracker");

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

    private void scanForPackageNames(File directory, Set<String> packages, String parentPackage) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                String currentPackage = parentPackage.isEmpty() ? file.getName() : parentPackage + "." + file.getName();
                scanForPackageNames(file, packages, currentPackage);
            } else if (file.getName().endsWith(".java")) {
                // Extract package name from Java file
                String packageName = parentPackage.replace(File.separator, ".");
                packages.add(packageName);
                break; // Only need to add the package name once for each directory
            }
        }
    }
}