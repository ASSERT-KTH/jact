package jonas.maven.master;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.lang.ProcessBuilder;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * Counts the number of maven dependencies of a project.
 *
 * It can be filtered by scope.
 *
 */
@Mojo(name = "dependency-counter", defaultPhase = LifecyclePhase.PACKAGE)
public class DependencyCounterMojo extends AbstractMojo {

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

        String outputDir = project.getBuild().getOutputDirectory();
        System.out.println("OUTPUT DIRECTORY: " + outputDir);

        //mvnVersion();
        File mavenHome = new File("/mnt/c/Programs/apache-maven-3.9.1");
        // Run JaCoCo usage analysis
        JacocoCoverage jacocoCoverage = new JacocoCoverage(project, mavenHome);
        UsageAnalysis jacocoUsageAnalysis = jacocoCoverage.executeTestBasedAnalysis();

        // Print out JaCoCo coverage output
        System.out.println("JaCoCo:");
        if (!jacocoUsageAnalysis.classes().isEmpty() && jacocoUsageAnalysis != null) {
            System.out.print(jacocoUsageAnalysis.toString());
        }else if(jacocoUsageAnalysis.classes().isEmpty()){
            System.out.println("jacoco analysis empty classes");
        }else if(jacocoUsageAnalysis == null){
            System.out.println("jacoco analysis is null");
        }else{
            System.out.println("Something else is wrong with jacoco");
        }
        //myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JACOCO, jacocoUsageAnalysis);
        //printCoverageAnalysisResults(jacocoUsageAnalysis);

        for (Dependency dependency : dependencies) {
            System.out.println("DEPENDENCY: " + dependency.toString());
            System.out.println("SCOPE: " + dependency.getScope());
        }


        long numDependencies = dependencies.stream()
                .filter(d -> (scope == null || scope.isEmpty()) || scope.equals(d.getScope()))
                .count();

        getLog().info("Number of dependencies: " + numDependencies);
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