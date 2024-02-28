package jonas.maven.master;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static jonas.maven.master.CompleteCoverageMojo.readInputStream;
import jonas.maven.master.ProjectDependency;
public class ProjectDependencies {

    List<List<ProjectDependency>> projectDependencies = new ArrayList<>();

    public ProjectDependencies(){

    }


    void parseDependencyTree(){

    }


    public static void generateDependencyTree(){
        try {
            // Command to be executed
            String command = "mvn dependency:tree -DoutputFile=./target/jact-report/dependency-tree.txt";

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
}
