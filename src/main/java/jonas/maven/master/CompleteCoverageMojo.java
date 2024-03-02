package jonas.maven.master;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.lang.ProcessBuilder;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.*;
import org.apache.maven.execution.MavenSession;

import org.apache.maven.shared.dependency.graph.*;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyGraphBuilder;
import io.github.chains_project.maven_lockfile.GenerateLockFileMojo;

import static jonas.maven.master.JacocoHTMLAugmenter.createDependencyReports;
import static jonas.maven.master.JacocoHTMLAugmenter.moveDepDirs;


/**
 * Counts the number of maven dependencies of a project.
 *
 * It can be filtered by scope.
 *
 */
@Mojo(name = "coverage-report", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class CompleteCoverageMojo extends AbstractMojo {

    /**
     * Scope to filter the dependencies.
     */
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

    public void execute() throws MojoExecutionException, MojoFailureException {
        projGroupIdSet.addAll(Arrays.asList(project.getGroupId().split("[.-]")));
        String outputJarName = project.getBuild().getFinalName();
        localRepoPath = session.getLocalRepository().getBasedir();
        hostOS = session.getSystemProperties().getProperty("os.name").toLowerCase();
        List<Dependency> dependencies = project.getDependencies();

        getLog().info("DEPENDENCY INFO:");
        for (Dependency dependency : dependencies) {
            getLog().info(dependency.toString() + "-{SCOPE: " + dependency.getScope() + "}");
        }

        String outputDirectory = project.getBuild().getOutputDirectory();

        //System.out.println("OUTPUT DIRECTORY: " + outputDirectory + "\n");

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
        moveDepDirs(projectDependencies);
        createDependencyReports(projectDependencies, project.getGroupId());

    }

}