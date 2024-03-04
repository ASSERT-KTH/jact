package jonas.maven.master;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class JacocoHTMLAugmenter {
    public static final String REPORTPATH = "./target/jact-report/";
    public static final String jacocoResPath = REPORTPATH + "jacoco-resources";


    public static void moveDepDirs(List<ProjectDependency> dependencies) {
        // Create a directory for the dependency coverage
        createDir(REPORTPATH + "dependencies");

        // Path to jacoco-resources (to be copied to subdirectories)
        copyDirectory(new File(jacocoResPath),
                new File(REPORTPATH + "dependencies/jacoco-resources"));

        for (ProjectDependency dependency : dependencies) {
            if (!dependency.getScope().equals("test")) {
                // Create all the dependency directories
                String fullPath = getFullDepPath(dependency);

                // Adding the path to easily get the report location.
                dependency.addReportPath(REPORTPATH + "dependencies/" + fullPath);
                createDir(REPORTPATH + "dependencies/" + fullPath);
                copyDirectory(new File(jacocoResPath),
                        new File(REPORTPATH + "dependencies/" + fullPath + "/jacoco-resources"));
            }
        }


        // Traverse the "report" directory
        File reportDir = new File(REPORTPATH);
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    String dirName = directory.getName();
                    boolean packageDir = CompleteCoverageMojo.projGroupIdSet.stream().allMatch(dirName::contains);
                    if(!dirName.equals("dependencies") && !dirName.equals("jacoco-resources") && !packageDir){
                        ProjectDependency matchedDep = PackageToDependencyResolver.packageToDepPaths(dirName);
                        if(matchedDep.getReportPaths().size() == 1){
                            //String path = matchedDep.getReportPaths().get(0);
                            moveDirectory(directory, matchedDep.getReportPaths().get(0));
                            String outputFilePath = matchedDep.getReportPaths().get(0) + "/index.html";
                            if(!new File(outputFilePath).exists()){
                                try {
                                    writeModifiedTemplateToFile("indivDepViewTemplateStart.html",
                                            outputFilePath, depToDirName(matchedDep));

                                    // Get the parent directory of the current path
                                    File parentDir = new File(matchedDep.getReportPaths().get(0)).getParentFile();
                                    // Ensure parentDir is not null and it's a directory
                                    if (parentDir != null && parentDir.isDirectory() && parentDir.getName().equals("transitive-dependencies")) {
                                        // Copy jacoco-resources if it's not already there.
                                        if (!new File(parentDir + "/index.html").exists()) {
                                            String parentDepName = parentDir.getParentFile().getName();
                                            try {
                                                writeModifiedTemplateToFile("indivDepViewTemplateStart.html",
                                                        parentDir + "/index.html", "Transitive Dependencies from: " + parentDepName);
                                                writeTemplateToFile("transitiveEntry.html", parentDir.getParentFile() + "/index.html");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        try {
                                            writeModifiedTemplateToFile("depEntry.html", parentDir + "/index.html", depToDirName(matchedDep));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            //writeTemplateToFile(templateFilePath2, outputFilePath);
                        }else{
                            // Handle dependencies with the same transitive dependencies.
                            for(String path : matchedDep.getReportPaths()){
                                copyDirectory(directory, new File(path + "/" + dirName));

                                // Get the parent directory of the current path
                                File parentDir = new File(path).getParentFile();
                                // Ensure parentDir is not null and it's a directory
                                if (parentDir != null && parentDir.isDirectory() && parentDir.getName().equals("transitive-dependencies")) {
                                    String parentDepName = parentDir.getParentFile().getName();
                                    if (!new File(parentDir + "/index.html").exists()) {
                                        try {
                                            writeModifiedTemplateToFile("indivDepViewTemplateStart.html",
                                                    parentDir + "/index.html", "Transitive Dependencies from: " + parentDepName);
                                            writeTemplateToFile("transitiveEntry.html", parentDir + "/index.html");
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    try {
                                        writeModifiedTemplateToFile("depEntry.html", parentDir + "/index.html", depToDirName(matchedDep));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                            }
                            System.out.println("REMOVING: " + directory.getPath());
                            removeDirectory(directory);
                        }
                    }
                }
            }
        }

    }

    public static String getFullDepPath(ProjectDependency projectDependency){
        StringBuilder fullPath = new StringBuilder();
        List<ProjectDependency> parentDeps = projectDependency.getParentDeps();
        String path;
        ProjectDependency currProjDep;

        // Creating the file path to the dependency
        for(int i = 0; i < parentDeps.size(); i++){
            currProjDep = parentDeps.get(i);
            path = depToDirName(currProjDep);
            fullPath.append(path);

            // Add a transitive dependencies directory here:
            fullPath.append("/transitive-dependencies/");

            // Copy jacoco-resources if it's not already there.
            if(!new File(fullPath + "jacoco-resources").exists()){
                copyDirectory(new File(jacocoResPath),
                        new File(REPORTPATH + "dependencies/" + fullPath + "/jacoco-resources"));
            }
        }
        // Adding dependency directory
        path = depToDirName(projectDependency);
        fullPath.append(path);

        return fullPath.toString();
    }

    private static String depToDirName(ProjectDependency dependency){
        return dependency.getGroupId().replace("-", ".") + "." +
                dependency.getArtifactId().replace("-", ".") + "-v" + dependency.getVersion();
    }

    public static void copyDirectory(File sourceDir, File destDir) {
        // Create the destination directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Get all files from the source directory
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                // Copy the file to the destination directory
                try {
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void removeDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    removeDirectory(file); // Recursive call to remove subdirectories
                } else {
                    if (!file.delete()) {
                        throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }

        if (!dir.delete()) {
            throw new RuntimeException("Failed to delete directory: " + dir.getAbsolutePath());
        }
    }


    private static void createDir(String directoryPath){
        // Will take a list of dependencies later

        File dir = new File(directoryPath);

        boolean success = dir.mkdirs();

        // Check if directory creation was successful
        if (success) {
            System.out.println("Report directory created successfully.");
        } else {
            System.out.println("Failed to create dependency report directory." + directoryPath);
        }
    }

    private static void moveDirectory(File sourceDir, String destDirName) {
        Path sourcePath = sourceDir.toPath();
        Path destPath = Paths.get(destDirName).resolve(sourcePath.getFileName());
        try {
            Files.move(sourcePath, destPath);
            //System.out.println("Moved directory: " + sourcePath.toString() + " to " + destPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void createDependencyReports(String projectName) {

        // Rename the original index.html file
        String originalFilePath = REPORTPATH + "index.html";
        File originalFile = new File(originalFilePath);
        File newFile = new File(originalFile.getParent(), "originalIndex.html");
        if (originalFile.exists()) {
            if (originalFile.renameTo(newFile)) {
                System.out.println("File renamed successfully.");
            } else {
                System.err.println("Failed to rename the file.");
            }
        } else {
            System.err.println("File doesn't exist.");
        }

        // Path to the original jacoco html report.
        String inputFilePath = REPORTPATH + "originalIndex.html";

        // Format the index.html report:
        try {
            // Read the HTML file
            File inputFile = new File(inputFilePath);
            Document doc = Jsoup.parse(inputFile, "UTF-8");

            String formattedHtml = doc.outerHtml();

            // Write the formatted HTML back to the original file, overwriting its previous content
            org.apache.commons.io.FileUtils.writeStringToFile(inputFile, formattedHtml, "UTF-8");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }


        // Create the individual dependencies report

        // HTML input and output file paths


        // Create individual reports for each dependency (including transitive).
        try {
            extractAndAppendHTMLDependencies(inputFilePath);
            System.out.println("Writing the overview for individual dependencies completed successfully.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Create the whole project overview
        String outputFilePath = REPORTPATH + "index.html";
        String templateFilePath1 = "overviewTemplateStart.html";
        String templateFilePath2 = "overviewTemplateEnd.html";
        String templateFilePathX = "overviewEntry.html";

        Set<String> projectNameSet = new HashSet<>();

        // Create sets of the words in the group/artifact-id
        projectNameSet.addAll(Arrays.asList(projectName.split("[.-]")));
        //System.out.println(projectNameSet.toString());
        try {
            writeTemplateToFile(templateFilePath1, outputFilePath);
            extractAndAppendHTML(inputFilePath, outputFilePath, projectNameSet); // Adds the project coverage
            writeTemplateToFile(templateFilePathX, outputFilePath);
            writeTemplateToFile(templateFilePath2, outputFilePath);
            System.out.println("Writing of the project overview completed successfully.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }


        // Create the dependencies overview
        // Create the dependencies overview
        // Traverse the "dependencies" directory
        File reportDir = new File(REPORTPATH + "dependencies");
        outputFilePath = REPORTPATH + "dependencies/index.html";
        templateFilePath1 = "depOverviewTemplateStart.html";
        templateFilePath2 = "depOverviewTemplateEnd.html";
        try {
            writeTemplateToFile(templateFilePath1, outputFilePath);
            if (reportDir.exists() && reportDir.isDirectory()) {
                File[] directories = reportDir.listFiles(File::isDirectory);
                if (directories != null) {
                    for (File directory : directories) {
                        String dirName = directory.getName();
                        if(!dirName.equals("jacoco-resources")){
                            writeModifiedTemplateToFile("depEntry.html", outputFilePath, dirName);
                        }
                    }
                }
            }
            writeTemplateToFile(templateFilePath2, outputFilePath);
            System.out.println("Writing the dependency overview completed successfully.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

    }


    public static String loadTemplate(String resourceName) throws IOException {
        try (InputStream inputStream = JacocoHTMLAugmenter.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                return stringBuilder.toString();
            }
        }
    }

    public static void writeTemplateToFile(String filename, String outputFilePath) throws IOException {
        String templateContent = loadTemplate(filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(templateContent);
        }
    }


    public static void extractAndAppendHTMLDependencies(String inputFilePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String line;

            // Flag to indicate if we are inside the <tbody> tag
            boolean insideTbody = false;

            // Iterate through the input HTML file
            while ((line = br.readLine()) != null) {
                // Check if we are inside the <tbody> tag
                if (line.contains("<tbody>")) {
                    insideTbody = true;
                } else if (line.contains("</tbody>")) {
                    insideTbody = false;
                }

                // TODO calculate the total usage here
                // Check if we are inside a <tr> element
                if (insideTbody && line.contains("<tr>")) {
                    // Read the next line
                    line = br.readLine();
                    if (line != null) {
                        // Extract package name from the <tr> element
                        String packageName = extractPackageName(line);
                        // Call function with package name
                        ProjectDependency matchedDep = PackageToDependencyResolver.packageToDepPaths(packageName);
                        // Write the entire <tr> element to each path
                        StringBuilder trContent = new StringBuilder(line).append("\n");
                        while ((line = br.readLine()) != null) {
                            trContent.append(line).append("\n");
                            if (line.contains("</tr>")) {
                                break; // Stop processing when encountering </tr>
                            }
                        }
                        if (!matchedDep.getReportPaths().isEmpty()) {
                            for (String path : matchedDep.getReportPaths()) {
                                // Get the parent directory of the current path
                                File parentDir = new File(path).getParentFile();

                                // Ensure parentDir is not null and it's a directory
                                if (parentDir != null && parentDir.isDirectory()) {
                                    // Construct the path to the parent directory's index.html file
                                    String indexPath = new File(parentDir, "index.html").getAbsolutePath();
                                    indexPath = path + "/index.html";
                                    // Write to the parent directory's index.html file
                                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(indexPath, true))) {
                                        bw.write(trContent.toString());
                                    }
                                }
                            }
                        }
                    }
                }

                // Check if we are outside the <tbody> tag
                // move this to the start of the loop later
                if (line.contains("</tbody>")) {
                    insideTbody = false;
                }
            }
        }
    }


    // Helper method to extract package name from <tr> element
    private static String extractPackageName(String line) {
        System.out.println("PACKAGE NAME");
        int startIndex = line.indexOf("el_package\">") + "el_package\">".length();
        int endIndex = line.indexOf("</a>", startIndex);
        System.out.println(line.substring(startIndex, endIndex));
        return line.substring(startIndex, endIndex);
    }



    public static void extractAndAppendHTML(String inputFilePath, String outputFilePath, Set<String> matchSet)
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
                    if (matchSet.stream().allMatch(line::contains)) {
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

    public static void writeModifiedTemplateToFile(String filename, String outputFilePath, String dependencyName) throws IOException {
        String templateContent = loadTemplateWithReplacement(filename, dependencyName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(templateContent);
        }
    }

    public static String loadTemplateWithReplacement(String resourceName, String dependencyName) throws IOException {
        try (InputStream inputStream = JacocoHTMLAugmenter.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            if(resourceName.equals("depOverviewTemplateStart.html")){
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.replace("dependency.name", dependencyName);
                        stringBuilder.append(line).append("\n");
                    }
                    return stringBuilder.toString();
                }
            }else{
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.replace("pathtodependencyindex", dependencyName + "/index.html");
                        line = line.replace("dependency.name", dependencyName);
                        stringBuilder.append(line).append("\n");
                    }
                    return stringBuilder.toString();
                }
            }
        }
    }

}
