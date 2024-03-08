package jact;

import java.net.URISyntaxException;
import java.util.*;
import java.io.IOException;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.*;
import org.apache.maven.execution.MavenSession;

import static jact.JacocoHTMLAugmenter.*;


/**
 * Generates a complete code coverage report including all
 * dependencies along with their transitive dependencies.
 */
@Mojo(name = "coverage-report", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class CompleteCoverageMojo extends AbstractMojo {

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

    public static String hostOS;
    public static String localRepoPath;

    public static Set<String> projGroupIdSet = new HashSet<>();
    public static String projectGroupId;
    public static String artifactId;
    public static String version;

    public void execute() throws MojoExecutionException, MojoFailureException {
        projGroupIdSet.addAll(Arrays.asList(project.getGroupId().split("[.-]")));
        String outputJarName = project.getBuild().getFinalName();
        localRepoPath = session.getLocalRepository().getBasedir();
        hostOS = session.getSystemProperties().getProperty("os.name").toLowerCase();
        projectGroupId = project.getGroupId();
        artifactId = project.getArtifactId();
        version = project.getVersion();

        List<String> packages = project.getCompileSourceRoots();

        getLog().info("Packages in project:");
        for (String packageName : packages) {
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
        moveReportDirs(projectDependencies);
        createDependencyReports(projectDependencies);
        getLog().info("JACT Report Successfully Generated!");
    }

}