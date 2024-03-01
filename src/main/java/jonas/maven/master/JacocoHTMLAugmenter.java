package jonas.maven.master;

import org.apache.maven.model.Dependency;
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

    public static void moveDepDirs(List<ProjectDependency> dependencies) {
        // Create a directory for the dependency coverage
        createDir(REPORTPATH + "dependencies");

        // Path to jacoco-resources (to be copied to subdirectories)
        String jacocoResPath = REPORTPATH + "jacoco-resources";
        copyDirectory(new File(jacocoResPath),
                new File(REPORTPATH + "dependencies/jacoco-resources"));

        // Generate sets of words from dependencies
        List<Set<String>> setOfAllDeps = new ArrayList<>();
        Map<Set<String>, List<String>> depPathsMap = new HashMap<>();
        for (ProjectDependency dependency : dependencies) {
            if (!dependency.getScope().equals("test")) {
                // Create all the dependency directories
                String depGroupId = dependency.getGroupId();
                String depArtifactId = dependency.getArtifactId();
                String depVersion = dependency.getVersion();
                String fullPath = getFullDepPath(dependency);


                createDir(REPORTPATH + "dependencies/" + fullPath);


                Set<String> depWordsSet = new HashSet<>();

                // Create sets of the words in the group/artifact-id
                depWordsSet.addAll(Arrays.asList(depGroupId.split("[.-]")));
                depWordsSet.addAll(Arrays.asList(depArtifactId.split("[.-]")));
                setOfAllDeps.add(depWordsSet);

                List<String> depPaths = depPathsMap.getOrDefault(depWordsSet, new ArrayList<>());
                depPaths.add(fullPath);
                depPathsMap.put(depWordsSet, depPaths);
            }
        }
//        Set<String> depWordsSet2 = new HashSet<>();
//        String dep = "org.apache.commons.math3";
//        depWordsSet2.addAll(Arrays.asList(dep.split("[.-]")));
//        setOfAllDeps.add(depWordsSet2);


        // Traverse the "report" directory
        File reportDir = new File(REPORTPATH);
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    // Check if directory name contains any string from sets in setOfAllDeps
                    String dirName = directory.getName();
                    //System.out.println("DIRECTORY: " + dirName);
                    for (Set<String> depWordsSet : setOfAllDeps) {
                         boolean containsAll = depWordsSet.stream().allMatch(dirName::contains);
                        //System.out.println("BOOL: " + containsAll);
                        if (containsAll) {
                            // Check again which directory it should be place in
                            // Another contains all with the pre-created directories.
                            List<String> matchedDirs = depPathsMap.get(depWordsSet);
                            //String matchingDir = matchPackageToDir(depWordsSet);
                            for(String matchedDir : matchedDirs){
                                copyDirectory(directory, new File(REPORTPATH + "dependencies/" + matchedDir)); // Copy instead
                                copyDirectory(new File(jacocoResPath),
                                        new File(REPORTPATH + "dependencies/" + matchedDir + "/jacoco-resources"));
                            }
                            removeDirectory(directory);
                            break; // Move to next directory after moving this one
                        }
                    }

                }

                // If we still have directories left with group ids that are from the dependencies:
                directories = reportDir.listFiles(File::isDirectory);
                if (directories != null) {
                    for (File directory : directories) {
                        // Check if directory name contains any string from sets in setOfAllDeps
                        String dirName = directory.getName();
                        //System.out.println("DIRECTORY: " + dirName);
                        for (ProjectDependency pd : dependencies) {
                            if (dirName.contains(pd.getGroupId().replace("-", "."))) {
                                Set<String> wordSet = new HashSet<>();

                                // Create sets of the words in the group/artifact-id
                                wordSet.addAll(Arrays.asList(pd.getGroupId().split("[.-]")));
                                wordSet.addAll(Arrays.asList(pd.getArtifactId().split("[.-]")));
                                List<String> matchedDirs = depPathsMap.get(wordSet);
                                for (String matchedDir : matchedDirs) {
                                    System.out.println("NON-STRICT PACKAGE MATCH, copying: " + dirName + " to: " + "\n" +
                                            REPORTPATH + "dependencies/" + matchedDir + "/" + dirName);
                                    copyDirectory(directory, new File(REPORTPATH + "dependencies/" + matchedDir + "/" + dirName)); // Copy instead
                                }
                                removeDirectory(directory);
                                break; // Move to next directory after moving this one
                            }
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
            path = currProjDep.getGroupId().replace("-", ".") + "." +
                    currProjDep.getArtifactId().replace("-", ".") + "-v" + currProjDep.getVersion();
            fullPath.append(path);

            // Add a transitive dependencies directory here:
            fullPath.append("/transitive-dependencies/");
        }
        // Adding dependency directory
        path = projectDependency.getGroupId().replace("-", ".") + "." +
                projectDependency.getArtifactId().replace("-", ".") + "-v" + projectDependency.getVersion();
        fullPath.append(path);

        return fullPath.toString();
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


    private static String matchPackageToDir(Set<String> matchedSet){
        // Traverse the "report" directory
        File reportDir = new File(REPORTPATH + "dependencies");
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    // Check if directory name contains any string from sets in setOfAllDeps
                    String dirName = directory.getName();
                    //System.out.println("DIRECTORY: " + dirName);
                    boolean containsAll = matchedSet.stream().allMatch(dirName::contains);
                    //System.out.println("BOOL: " + containsAll);
                    if (containsAll) {
                        return dirName;
                    }
                }
            }
        }
        return "Could not find a matching directory";
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


    public static void createDependencyReports(List<ProjectDependency> dependencies, String projectName) {

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



        // REMOVE THIS LATER
        // Generate sets of words from dependencies
        List<Set<String>> setOfAllDeps = new ArrayList<>();
        for (ProjectDependency dependency : dependencies) {
            if(!dependency.getScope().equals("test")){
                // Create all the dependency directories
                String depGroupId = dependency.getGroupId();
                String depArtifactId = dependency.getArtifactId();
                String depVersion = dependency.getVersion();

                Set<String> depWordsSet = new HashSet<>();

                // Create sets of the words in the group/artifact-id
                depWordsSet.addAll(Arrays.asList(depGroupId.split("[.-]")));
                depWordsSet.addAll(Arrays.asList(depArtifactId.split("[.-]")));
                setOfAllDeps.add(depWordsSet);
            }
        }


        // Create the individual dependencies report

        // HTML input and output file paths

        String outputFilePath = "";
        String templateFilePath1 = "indivDepViewTemplateStart.html";
        String templateFilePath2 = "indivDepViewTemplateEnd.html";


        // Create a report inside 'target/report/depname/index.html'
        // for all dependencies.

        // Traverse the "dependencies" directory
        File reportDir = new File(REPORTPATH + "dependencies");
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    // Check if directory name contains any string from sets in setOfAllDeps
                    String dirName = directory.getName();
                    //System.out.println("DIRECTORY: " + dirName);
                    for (Set<String> depWordsSet : setOfAllDeps) {
                        boolean containsAll = depWordsSet.stream().allMatch(dirName::contains);
                        //System.out.println("BOOL: " + containsAll);
                        if(containsAll){
                            String depDirName = matchPackageToDir(depWordsSet);
                            outputFilePath = REPORTPATH + "dependencies/" + depDirName + "/index.html";
                            try {
                                writeModifiedTemplateToFile("indivDepViewTemplateStart.html",
                                        outputFilePath, depDirName);

                                extractAndAppendHTML(inputFilePath, outputFilePath, depWordsSet);

                                writeTemplateToFile(templateFilePath2, outputFilePath);
                                //appendTemplate(templateFilePath2, outputFilePath);
                                System.out.println("Writing the dependency package overview for " + depDirName + " completed successfully.");
                            } catch (IOException e) {
                                System.err.println("Error: " + e.getMessage());
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
        }


        // Create the whole project overview
        outputFilePath = REPORTPATH + "index.html";
        templateFilePath1 = "overviewTemplateStart.html";
        templateFilePath2 = "overviewTemplateEnd.html";
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

        outputFilePath = REPORTPATH + "dependencies/index.html";
        templateFilePath1 = "depOverviewTemplateStart.html";
        templateFilePath2 = "depOverviewTemplateEnd.html";
        try {
            writeTemplateToFile(templateFilePath1, outputFilePath);
            //copyTemplate(templateFilePath1, outputFilePath);
            File depDir = new File("./target/report/dependencies");
            if (reportDir.exists() && reportDir.isDirectory()) {
                File[] directories = reportDir.listFiles(File::isDirectory);
                if (directories != null) {
                    for (File directory : directories) {
                        // Check if directory name contains any string from sets in setOfAllDeps
                        String dirName = directory.getName();
                        for (Set<String> depWordsSet : setOfAllDeps) {
                            boolean containsAll = depWordsSet.stream().allMatch(dirName::contains);
                            //System.out.println("BOOL: " + containsAll);
                            if(containsAll){
                                writeModifiedTemplateToFile("depEntry.html", outputFilePath, dirName);
                                break;
                            }
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
