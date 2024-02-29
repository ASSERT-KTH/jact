package jonas.maven.master;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jonas.maven.master.CompleteCoverageMojo.readInputStream;
import jonas.maven.master.ProjectDependency;
public class ProjectDependencies {

    List<ProjectDependency> projectDependencies = new ArrayList<>();

    public ProjectDependencies(){

    }


    void parseDependencyTreeFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                parseDependencyLine(line, null); // Initially parent is null for the root dependencies
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseDependencyLine(String line, ProjectDependency parent) {
        // Regular expressions to match group id, artifact id, version, scope, and optional
        Pattern pattern = Pattern.compile("([\\w.:-]+):(\\w+):(\\w+):(\\w+)(?::(\\w+))?");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);
            String scope = matcher.group(4);
            String optional = matcher.group(5);

            // Create ProjectDependency object
            ProjectDependency dependency = new ProjectDependency();
            dependency.setGroupId(groupId);
            dependency.setArtifactId(artifactId);
            dependency.setVersion(version);
            dependency.setScope(scope);
            dependency.setOptional(optional != null && optional.equals("test")); // assuming optional is "test"
            dependency.setParent(parent); // Set the parent

            // Add dependency to the list
            projectDependencies.add(dependency);

            // Recursively parse child dependencies
            parseChildDependencies(line, dependency);
        }
    }

    private void parseChildDependencies(String line, ProjectDependency parent) {
        String[] lines = line.split("\\n"); // Split by newline to get child lines
        for (int i = 1; i < lines.length; i++) { // Start from index 1 to skip the parent line
            parseDependencyLine(lines[i], parent);
        }
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
