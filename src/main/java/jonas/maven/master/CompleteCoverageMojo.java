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
@Mojo(name = "dependency-counter", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class CompleteCoverageMojo extends AbstractMojo {

    /**
     * Scope to filter the dependencies.
     */
    @Parameter(property = "scope")
    String scope;

    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = false)
    MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Dependency> dependencies = project.getDependencies();


        getLog().info("DEPENDENCY INFO:");
        for (Dependency dependency : dependencies) {
            getLog().info(dependency.toString() + "-{SCOPE: " + dependency.getScope() + "}");
        }

        String outputDirectory = project.getBuild().getOutputDirectory();

        //System.out.println("OUTPUT DIRECTORY: " + outputDirectory + "\n");

        //ProjectDependencies.generateDependencyTree();

        // Execute JaCoCoCLI to create the report WITH dependencies
        getLog().info("Copying the `jacococli.jar` to the project.");
        try {
            copyJacocoCliJar();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        getLog().info("Creating the complete coverage report.");
        executeJacocoCLI("sanity-check-1.0-shaded"); // TODO need to get the final jar name

        getLog().info("Organizing the complete coverage report.");
        moveDepDirs(dependencies);
        createDependencyReports(dependencies, project.getGroupId());

        //mvnVersion();
        //File mavenHome = new File("/mnt/c/Programs/apache-maven-3.9.1");

    }

    public void copyJacocoCliJar() throws IOException, URISyntaxException {
        // Get the path to the plugin JAR file
        Path pluginJarPath = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

        // Create a JarInputStream to read from the plugin JAR
        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(pluginJarPath.toFile()))) {
            // Loop through all entries in the plugin JAR
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                // Look for jacococli.jar entry
                if (jarEntry.getName().equals("jacococli.jar")) {
                    // Prepare the target directory
                    Path targetDirectory = Paths.get("target", "jacococli");
                    Files.createDirectories(targetDirectory);

                    // Define the target file path
                    Path targetPath = targetDirectory.resolve("jacococli.jar");

                    // Copy the entry to the target directory
                    try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = jarInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                    break; // Exit loop once jacococli.jar is found and copied
                }
            }
        }
    }

    void executeJacocoCLI(String jarName) throws MojoExecutionException {
        try {
            // Retrieve the URL to the jacococli.jar file
            // Command to execute Jacoco CLI
            String command = String.format("java -jar ./target/jacococli/jacococli.jar report ./target/jacoco.exec --classfiles " +
                    "./target/" + jarName +".jar --html ./target/report");

            // For Linux
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // If Jacoco CLI execution fails
            if (exitCode != 0) {
                throw new MojoExecutionException("Failed to execute Jacoco CLI");
            }

            // Print the output
            InputStream inputStream = process.getInputStream();
            String output = readInputStream(inputStream);
            getLog().info(output);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error executing Jacoco CLI", e);
        }
    }

    void mvnVersion(){
        try {
            // Command to be executed
            String command = "mvn --version";

            // Create a process builder with a shell
            //ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command); // For Windows
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command); // For Linux

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Get the input stream of the process
            InputStream inputStream = process.getInputStream();

            // Read the output
            String output = readInputStream(inputStream);

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Print the output
            System.out.println("Exit Code: " + exitCode);
            System.out.println("Output:\n" + output);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static String readInputStream(InputStream inputStream) throws IOException {
        // Read the input stream and convert it to a string
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
    }

}