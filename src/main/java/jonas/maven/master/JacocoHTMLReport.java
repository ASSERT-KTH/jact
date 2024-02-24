package jonas.maven.master;

import org.apache.maven.model.Dependency;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JacocoHTMLReport {

    public static void moveDepDirs(List<Dependency> dependencies) {
        // Create a directory for the dependency coverage
        createDir("dependencies");

        // Generate sets of words from dependencies
        List<Set<String>> setOfAllDeps = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            if(!dependency.getScope().equals("test")){
                // Create all the dependency directories
                String depGroupId = dependency.getGroupId();
                String depArtifactId = dependency.getArtifactId();
                String depVersion = dependency.getVersion();
                createDir("dependencies/" + depGroupId.replace("-", ".") +
                        "." + depArtifactId.replace("-", ".") + "-v" + depVersion);

                Set<String> depWordsSet = new HashSet<>();

                // Create sets of the words in the group/artifact-id
                depWordsSet.addAll(Arrays.asList(depGroupId.split("[.-]")));
                depWordsSet.addAll(Arrays.asList(depArtifactId.split("[.-]")));
                setOfAllDeps.add(depWordsSet);
            }
        }
//        Set<String> depWordsSet2 = new HashSet<>();
//        String dep = "org.apache.commons.math3";
//        depWordsSet2.addAll(Arrays.asList(dep.split("[.-]")));
//        setOfAllDeps.add(depWordsSet2);





        // Traverse the "report" directory
        File reportDir = new File("report");
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    // Check if directory name contains any string from sets in setOfAllDeps
                    String dirName = directory.getName();
                    System.out.println("DIRECTORY: " + dirName);
                    for (Set<String> depWordsSet : setOfAllDeps) {
                        boolean containsAll = depWordsSet.stream().allMatch(dirName::contains);
                        System.out.println("BOOL: " + containsAll);
                        if (containsAll) {
                            // Check again which directory it should be place in
                            // Another contains all with the pre-created directories.
                            File depDir = new File("dependencies");
                            String matchingDir = matchPackageToDir(depWordsSet);
                            moveDirectory(directory, "./report/dependencies/" + matchingDir);
                            break; // Move to next directory after moving this one
                        }
                    }
                }
            }
        }
    }

    private static String matchPackageToDir(Set<String> matchedSet){
        // Traverse the "dependencies" directory
        File reportDir = new File("report/dependencies");
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    // Check if directory name contains any string from sets in setOfAllDeps
                    String dirName = directory.getName();
                    System.out.println("DIRECTORY: " + dirName);
                    boolean containsAll = matchedSet.stream().allMatch(dirName::contains);
                    System.out.println("BOOL: " + containsAll);
                    if (containsAll) {
                        return dirName;
                    }
                }
            }
        }
        return "Could not find a matching directory";
    }

    private static void createDir(String dirName){
        // Will take a list of dependencies later
        String directoryPath = "./report/" + dirName;

        File dir = new File(directoryPath);

        // Use the mkdirs() method to create the directory along with any necessary parent directories
        boolean success = dir.mkdirs();

        // Check if directory creation was successful
        if (success) {
            System.out.println("Directory created successfully.");
        } else {
            System.out.println("Failed to create directory.");
        }

        //for dependency in dependencies --> create dirs for all of them
    }

    private static void moveDirectory(File sourceDir, String destDirName) {
        Path sourcePath = sourceDir.toPath();
        Path destPath = Paths.get(destDirName).resolve(sourcePath.getFileName());
        try {
            Files.move(sourcePath, destPath);
            System.out.println("Moved directory: " + sourcePath.toString() + " to " + destPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        String depEntry = "org.apache.commons.commons.math3-v3.6.1";

        // HTML input and output file paths
        String inputFilePath = "input.html"; // Provide the path to your input HTML file
        String outputFilePath = "math3cov.html"; // Provide the path to the output HTML file
        String templateFilePath1 = "depTemplate1.html"; // Provide the path to the first template HTML file
        String templateFilePath2 = "depTemplate2.html"; // Provide the path to the second template HTML file

        // Call method to extract and write HTML
        try {
            copyTemplate(templateFilePath1, outputFilePath);
            extractAndAppendHTML(inputFilePath, outputFilePath, "org.apache.commons.math3");
            appendTemplate(templateFilePath2, outputFilePath);
            System.out.println("Extraction and writing completed successfully.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        outputFilePath = "projOverview.html"; // Provide the path to the output HTML file
        templateFilePath1 = "depOverviewTemplate1.html"; // Provide the path to the first template HTML file
        templateFilePath2 = "depOverviewTemplate2.html"; // Provide the path to the second template HTML file
        String templateFilePathX = "depOverviewEntry.html";

        try {
            copyTemplate(templateFilePath1, outputFilePath);
            extractAndAppendHTML(inputFilePath, outputFilePath, "jonas.sanity.check");
            appendTemplate(templateFilePathX, outputFilePath);
            appendTemplate(templateFilePath2, outputFilePath);
            System.out.println("Extraction and writing completed successfully. 2");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        outputFilePath = "dependencies.html"; // Provide the path to the output HTML file
        templateFilePath1 = "depTemplate1.html"; // Provide the path to the first template HTML file
        templateFilePath2 = "depTemplate2.html"; // Provide the path to the second template HTML file
        try {
            copyTemplate(templateFilePath1, outputFilePath);
            copyHtmlWithReplacement(outputFilePath, depEntry);
            appendTemplate(templateFilePath2, outputFilePath);
            System.out.println("Extraction and writing completed successfully. 3");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void copyTemplate(String templateFilePath, String outputFilePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(templateFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            // Copy contents of template file to output file
            while ((line = br.readLine()) != null) {
                bw.write(line.trim() + "\n");
            }
        }
    }

    public static void appendTemplate(String templateFilePath, String outputFilePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(templateFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {

            String line;
            // Append contents of template file to output file
            while ((line = br.readLine()) != null) {
                bw.write(line.trim() + "\n");
            }
        }
    }

    public static void extractAndAppendHTML(String inputFilePath, String outputFilePath, String matchName)
            throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {

            String line;

            // Flag to indicate if we are inside the <tbody> tag
            boolean insideTbody = false;
            // Flag to indicate if the current <tr> element contains the specified string
            boolean containsString = false;
            // StringBuilder to store content of current <tr> element
            StringBuilder trContent = new StringBuilder();

            // Iterate through the input HTML file
            while ((line = br.readLine()) != null) {
                // Check if we are inside the <tbody> tag
                if (line.contains("<tbody>")) {
                    insideTbody = true;
                }

                // Check if we are inside a <tr> element
                if (insideTbody && line.contains("<tr>")) {
                    trContent.setLength(0); // Clear StringBuilder for new <tr> element
                    trContent.append(line.trim()).append("\n");
                    containsString = false; // Reset containsString flag for new <tr> element
                }

                // Append line to current <tr> element content
                if (insideTbody && trContent.length() > 0) {
                    trContent.append(line.trim()).append("\n");
                    if (line.contains(matchName)) {
                        containsString = true; // Set containsString flag if the line contains the specified string
                    }
                }

                // Write content of <tr> element to output if it contains the specified string
                if (insideTbody && line.contains("</tr>")) {
                    if (containsString) {
                        bw.write(trContent.toString());
                    }
                }

                // Check if we are outside the <tbody> tag
                if (line.contains("</tbody>")) {
                    insideTbody = false;
                }
            }
        }
    }

    public static void copyHtmlWithReplacement(String outputFilePath, String dependencyName)
            throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader("depEntry.html"));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, true))) {

            String line;

            // Read each line of the template HTML file
            while ((line = br.readLine()) != null) {
                // Replace placeholders with provided strings
                line = line.replace("pathtodependencyindex", dependencyName + "/index.html");
                line = line.replace("dependency.name", dependencyName);

                // Write the modified line to the output file
                bw.write(line);
                bw.newLine();
            }
        }
    }

}
