package jact.plugin;

import jact.utils.CommandExecutor;
import jact.depUtils.ProjectDependencies;
import jact.depUtils.ProjectDependency;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static jact.core.XmlAugmenter.groupPackageByDep;


/**
 * Generates a complete code coverage report including all
 * dependencies along with their transitive dependencies.
 */
@Mojo(name = "xml-report", defaultPhase = LifecyclePhase.INSTALL, threadSafe = false)
public class XmlReportMojo extends AbstractReportMojo {

    @Override
    public void doExecute() throws MojoExecutionException {
        String outputJarName = getOutputJarName();

        // Print out packages and their classes
        getLog().info("Packages in project:");
        for (Map.Entry<String, Set<String>> entry : getProjectPackagesAndClasses().entrySet()) {
            getLog().info("- " + entry.getKey());
            for (String className : entry.getValue()) {
                getLog().info("  - " + className);
            }
        }


        getLog().info("STARTING: JACT - Java Complete Coverage Tracker");
        getLog().info("JARNAME: " + outputJarName + "-shaded");
        //String outputDirectory = project.getBuild().getOutputDirectory();



        CommandExecutor cmdExec = new CommandExecutor(getHostOS());
        List<ProjectDependency> projectDependencies = ProjectDependencies.getAllProjectDependencies(cmdExec, "./target/jact-report/", true);

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
        cmdExec.executeJacocoCLI(outputJarName + "-shaded", false);

        getLog().info("Organizing the complete coverage report.");

        groupPackageByDep(projectDependencies, getProjectPackagesAndClasses(), getLocalRepoPath(), getProjId());

        getLog().info("JACT: XML Report Successfully Generated!");
    }
}