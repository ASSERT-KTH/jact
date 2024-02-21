package jonas.maven.master;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.lang.ProcessBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
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
@Mojo(name = "dependency-counter", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
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

        String outputDirectory = project.getBuild().getOutputDirectory();
        String projectBaseDir = project.getBasedir().getAbsolutePath();
        MyFileWriter myFileWriter = new MyFileWriter(projectBaseDir);
        myFileWriter.resetJDBLReportsDirectory();

        replaceClassesWithClassesInJarWithDependencies(projectBaseDir);
        System.out.println("OUTPUT DIRECTORY: " + outputDirectory);




        //mvnVersion();
        //File mavenHome = new File("/mnt/c/Programs/apache-maven-3.9.1");
        // Run JaCoCo usage analysis
        //JacocoCoverage jacocoCoverage = new JacocoCoverage(project, mavenHome);
        //UsageAnalysis jacocoUsageAnalysis = jacocoCoverage.executeTestBasedAnalysis();

        // Print out JaCoCo coverage output
//        System.out.println("JaCoCo:");
//        if (!jacocoUsageAnalysis.classes().isEmpty() && jacocoUsageAnalysis != null) {
//            System.out.print(jacocoUsageAnalysis.toString());
//        }else if(jacocoUsageAnalysis.classes().isEmpty()){
//            System.out.println("jacoco analysis empty classes");
//        }else if(jacocoUsageAnalysis == null){
//            System.out.println("jacoco analysis is null");
//        }else{
//            System.out.println("Something else is wrong with jacoco");
//        }
        //myFileWriter.writeCoverageAnalysisToFile(CoverageToolEnum.JACOCO, jacocoUsageAnalysis);
        //printCoverageAnalysisResults(jacocoUsageAnalysis);

        String filename = "ProjectDependencies.json";

//        try (FileWriter fileWriter = new FileWriter(filename)) {
//            for (Dependency dependency : dependencies) {
//                fileWriter.write(dependency.toString() + "\n");
//            }
//            System.out.println("Successfully wrote to the file.");
//        } catch (IOException e) {
//            System.out.println("An error occurred while writing to the file.");
//            e.printStackTrace();
//        }


//        try (FileWriter fileWriter = new FileWriter(filename)) {
//            JSONArray jsonArray = new JSONArray();
//
//            for (Dependency dependency : dependencies) {
//                JSONObject dependencyObj = new JSONObject();
//                dependencyObj.put("groupId", dependency.getGroupId());
//                dependencyObj.put("artifactId", dependency.getArtifactId());
//                dependencyObj.put("version", dependency.getVersion());
//                jsonArray.add(dependencyObj);
//            }
//
//            fileWriter.write(jsonArray.toJSONString());
//            System.out.println("Successfully wrote to the JSON file.");
//        } catch (IOException e) {
//            System.out.println("An error occurred while writing to the JSON file.");
//            e.printStackTrace();
//        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter fileWriter = new FileWriter(filename)) {
            for (Dependency dependency : dependencies) {
                JsonObject dependencyObj  = new JsonObject();
                dependencyObj.addProperty("groupId", dependency.getGroupId());
                dependencyObj.addProperty("artifactId", dependency.getArtifactId());
                dependencyObj.addProperty("version", dependency.getVersion());
                dependencyObj.addProperty("scope", dependency.getScope());
                //dependencyObj.addProperty("type", dependency.getType());
                String json = gson.toJson(dependencyObj);
                fileWriter.write(json + "\n");
            }
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }

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

    private void replaceClassesWithClassesInJarWithDependencies(final String projectBaseDir)
    {
        Collection<File> jarFiles = FileUtils.listFiles(new File(projectBaseDir + "/target"), new String[]{"jar"}, false);

        for (File jarFile : jarFiles) {
            if (jarFile.getName().endsWith("-jar-with-dependencies.jar")) {
                final String jarWithDepsName = jarFile.getName().substring(0, jarFile.getName().length() - 4);
                final String jarWithDepsPath = jarFile.getAbsolutePath();
                JarWithDeps.setInstance(jarWithDepsName, jarWithDepsPath);
            }
        }
    }

}