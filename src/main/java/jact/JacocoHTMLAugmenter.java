package jact;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jact.DirectoryUtils.*;

public class JacocoHTMLAugmenter {
    public static final String REPORTPATH = "./target/jact-report/";
    public static final String jacocoResPath = REPORTPATH + "jacoco-resources";
    private static final String projId = CompleteCoverageMojo.getProjectGroupId() + ":" +
            CompleteCoverageMojo.getProjectArtifactId() + ":" + CompleteCoverageMojo.getProjectVersion();
    private static ProjectDependency thisProject = new ProjectDependency();

    public static void extractReportAndMoveDirs(List<ProjectDependency> dependencies) throws IOException {
        thisProject.setId(projId);
        // Create a directory for the dependency coverage
        createDir(REPORTPATH + "dependencies");

        // Path to jacoco-resources (to be copied to subdirectories for correct icons and styling)
        copyDirectory(new File(jacocoResPath),
                new File(REPORTPATH + "dependencies/jacoco-resources"));

        // Creates the dependency report directories
        for (ProjectDependency dependency : dependencies) {
            if (!dependency.getScope().equals("test")) {
                String fullPath = getFullDepPath(dependency);

                // Adding the path to easily get the report location.
                dependency.addReportPath(REPORTPATH + "dependencies/" + fullPath);
                createDir(REPORTPATH + "dependencies/" + fullPath);
                copyDirectory(new File(jacocoResPath),
                        new File(REPORTPATH + "dependencies/" + fullPath + "/jacoco-resources"));
            }
        }

        // Traverse the "report" directory:
        // Moves packages to their respective dependency directory and create their `index.html` file
        File reportDir = new File(REPORTPATH);
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    String dirName = directory.getName();
                    //boolean packageDir = CompleteCoverageMojo.projGroupIdSet.stream().allMatch(dirName::contains);
                    if (!dirName.equals("dependencies") && !dirName.equals("jacoco-resources")) {
                        // Could become problematic if packages share name with packages in dependencies
                        // TODO HANDLE COLLIDING PROJECT PACKAGES HERE
                        List<ProjectDependency> matchedDeps = PackageToDependencyResolver.packageToDepPaths(dirName, dependencies);
                        if (CompleteCoverageMojo.getProjectPackagesAndClasses().containsKey(dirName) && matchedDeps.isEmpty()) {
                            extractAndAddPackageTotal(REPORTPATH + dirName +
                                    "/index.html", thisProject, dirName);
                            thisProject.addReportPath(REPORTPATH + dirName);
                        } else if(!CompleteCoverageMojo.getProjectPackagesAndClasses().containsKey(dirName) && !matchedDeps.isEmpty()) {
                            // Go into the directory and move the classfiles to a project package directory
                            ProjectDependency matchedDep = matchedDeps.get(0);
                            if (matchedDep.getId() != null) {
                                extractAndAddPackageTotal(REPORTPATH + dirName +
                                        "/index.html", matchedDep, dirName);
                            }
                            if (matchedDep.getReportPaths().size() == 1) {
                                moveDirectory(directory, matchedDep.getReportPaths().get(0));
                                String outputFilePath = matchedDep.getReportPaths().get(0) + "/index.html";
                                if (!new File(outputFilePath).exists()) {
                                    try {
                                        writeModifiedTemplateToFile("html-templates/indivDepViewTemplateStart.html",
                                                outputFilePath, depToDirName(matchedDep));

                                        // Get the parent directory of the current path
                                        File parentDir = new File(matchedDep.getReportPaths().get(0)).getParentFile();
                                        // Ensure parentDir is not null and it's a directory
                                        if (parentDir != null && parentDir.isDirectory() && parentDir.getName().equals("transitive-dependencies")) {
                                            // Copy jacoco-resources if it's not already there.
                                            if (!new File(parentDir + "/index.html").exists()) {
                                                String parentDepName = parentDir.getParentFile().getName();
                                                try {
                                                    writeModifiedTemplateToFile("html-templates/indivDepViewTemplateStart.html",
                                                            parentDir + "/index.html",
                                                            "<span style=\"display: inline-block;\">Transitive Dependencies from: <br>" +
                                                                    parentDepName + "</span>");
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                        }

                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            } else {
                                // Handle dependencies with the same transitive dependencies.
                                for (String path : matchedDep.getReportPaths()) {
                                    copyDirectory(directory, new File(path + "/" + dirName));

                                    // Get the parent directory of the current path
                                    File parentDir = new File(path).getParentFile();
                                    // Ensure parentDir is not null and it's a directory
                                    if (parentDir != null && parentDir.isDirectory() && parentDir.getName().equals("transitive-dependencies")) {
                                        String parentDepName = parentDir.getParentFile().getName();
                                        if (!new File(parentDir + "/index.html").exists()) {
                                            try {
                                                writeModifiedTemplateToFile("html-templates/indivDepViewTemplateStart.html",
                                                        parentDir + "/index.html", "Transitive Dependencies from: " + parentDepName);
                                                //writeTemplateToFile("transitiveEntry.html", parentDir.getParentFile() + "/index.html");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
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
    }

    public static String getFullDepPath(ProjectDependency projectDependency) {
        StringBuilder fullPath = new StringBuilder();
        List<ProjectDependency> parentDeps = projectDependency.getParentDeps();
        String path;
        ProjectDependency currProjDep;

        // Creating the file path to the dependency
        for (int i = 0; i < parentDeps.size(); i++) {
            currProjDep = parentDeps.get(i);
            path = depToDirName(currProjDep);
            fullPath.append(path);

            // Add a transitive dependencies directory here:
            fullPath.append("/transitive-dependencies/");

            // Copy jacoco-resources if it's not already there.
            if (!new File(fullPath + "jacoco-resources").exists()) {
                copyDirectory(new File(jacocoResPath),
                        new File(REPORTPATH + "dependencies/" + fullPath + "/jacoco-resources"));
            }
        }
        // Adding dependency directory
        path = depToDirName(projectDependency);
        fullPath.append(path);

        return fullPath.toString();
    }

    public static String depToDirName(ProjectDependency dependency) {
        return dependency.getGroupId().replace("-", ".") + "." +
                dependency.getArtifactId().replace("-", ".") + "-v" + dependency.getVersion();
    }


    public static void createDependencyReports(List<ProjectDependency> dependencies) {

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


        // Create the whole project overview
        String outputFilePath = REPORTPATH + "index.html";
        String templateFilePath1 = "html-templates/overviewTemplateStart.html";
        String templateFilePath2 = "html-templates/overviewTemplateEnd.html";

        try {
            // Writes the overview HTML template
            writeTemplateToFile(templateFilePath1, outputFilePath);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }


        // Create the dependencies overview
        outputFilePath = REPORTPATH + "dependencies/index.html";
        templateFilePath1 = "html-templates/depOverviewTemplateStart.html";
        templateFilePath2 = "html-templates/depOverviewTemplateEnd.html";
        try {
            // Writes the HTML template for the Dependency Overview
            writeTemplateToFile(templateFilePath1, outputFilePath);
            //writeTemplateToFile(templateFilePath2, outputFilePath);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        List<String> writtenPaths = new ArrayList<>();
        List<String> writtenEntryPaths = new ArrayList<>();

        DependencyUsage totalDepUsage = new DependencyUsage();
        for (ProjectDependency pd : dependencies) {
            DependencyUsage currTotal = new DependencyUsage();
            currTotal = calculateTotalForAllLayers(pd, writtenPaths, writtenEntryPaths, currTotal);
            if (!pd.writtenEntryToFile) {
                pd.writtenEntryToFile = true;
                for (String path : pd.getReportPaths()) {
                    File currDir = new File(path);
                    File parentDir = currDir.getParentFile();
                    try {
                        writeHTMLStringToFile(parentDir + "/index.html", pd.dependencyUsage.usageToHTML(currDir.getName(), currTotal, false));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            pd.writePackagesToFile(currTotal);

            totalDepUsage.addAll(pd.dependencyUsage);
        }

        try {
            // Writes the HTML template for the Dependency Overview
            writeTemplateToFile(templateFilePath2, outputFilePath);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            DependencyUsage overallTotal = new DependencyUsage();
            overallTotal.addAll(totalDepUsage);
            for (Map.Entry<String, DependencyUsage> entry : thisProject.packageUsageMap.entrySet()) {
                overallTotal.addAll(entry.getValue());
            }

            // Write the total dependency usage AND its entry in the overview
            writeHTMLStringToFile(REPORTPATH + "/index.html", totalDepUsage.usageToHTML("dependencies", overallTotal, false));
            writeHTMLTotalToFile(REPORTPATH + "dependencies/index.html", totalDepUsage.totalUsageToHTML());

            // Write the project package overview entries:
            for (Map.Entry<String, DependencyUsage> entry : thisProject.packageUsageMap.entrySet()) {
                try {
                    writeHTMLStringToFile(REPORTPATH + "/index.html", entry.getValue().usageToHTML(entry.getKey(),overallTotal, true));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // Write the overview total: Project + Dependencies (incl. transitive)
            writeHTMLTotalToFile(REPORTPATH + "index.html", overallTotal.totalUsageToHTML());


            // Create the whole project overview
            outputFilePath = REPORTPATH + "index.html";
            templateFilePath2 = "html-templates/overviewTemplateEnd.html";

            try {
                // Writes the overview HTML template
                writeTemplateToFile(templateFilePath2, outputFilePath);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


    public static DependencyUsage calculateTotalForAllLayers(ProjectDependency currDependency, List<String> writtenPaths, List<String> writtenEntryPaths, DependencyUsage currTotal) {
        List<String> writtenTotalPaths = new ArrayList<>();
        // Keep track of dependencies that have already been checked out.
        //DependencyUsage currTotal = new DependencyUsage();
        if (!currDependency.getChildDeps().isEmpty()) {
            DependencyUsage childTotal = new DependencyUsage();
            for (ProjectDependency child : currDependency.getChildDeps()) {
                DependencyUsage childUsage = new DependencyUsage();
                childTotal.addAll(calculateTotalForAllLayers(child, writtenPaths, writtenEntryPaths, childUsage));
                currDependency.dependencyUsage.addAll(childTotal);
            }

            for (String path : currDependency.getReportPaths()) {
                if (!writtenPaths.contains(path)) {
                    writtenPaths.add(path);
                    File currDir = new File(path);
                    File parentDir = currDir.getParentFile();
                    try {
                        // Writing the dependency total as an entry
                        DependencyUsage totalForBars = currTotal;
                        totalForBars.addAll(currDependency.dependencyUsage);
                        writeHTMLStringToFile(currDir + "/index.html", childTotal.usageToHTML("transitive-dependencies", totalForBars, false));
                        writeHTMLTotalToFile(currDir + "/transitive-dependencies/index.html", childTotal.totalUsageToHTML());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
        // Calculate the total.
        currTotal.addAll(currDependency.dependencyUsage);


        // Write here
        if (!currDependency.writtenTotalToFile) {
            currDependency.writtenTotalToFile = true;
            for (String path : currDependency.getReportPaths()) {
                File currDir = new File(path);
                try {
                    // Writing the dependency total as an entry
                    writtenEntryPaths.add(path);
                    if (new File(currDir + "/index.html").exists()) {
                        writeHTMLTotalToFile(currDir + "/index.html", currDependency.dependencyUsage.totalUsageToHTML());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        return currTotal;
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


    private static void extractUsage(String line, int entryIndex, ProjectDependency matchedDep, DependencyUsage packageUsage) {
        switch (entryIndex) {
            case 1:
                // Missed and Covered instructions
                long[] instrUsage = extractBranchNInstrUsage(line);
                matchedDep.dependencyUsage.addMissedInstructions(instrUsage[0]);
                matchedDep.dependencyUsage.addTotalInstructions(instrUsage[1]);
                packageUsage.addMissedInstructions(instrUsage[0]);
                packageUsage.addTotalInstructions(instrUsage[1]);
                break;
            case 2:
                // Percentage (Don't care about this now)
                break;
            case 3:
                // Missed and Covered Branches
                long[] branchUsage = extractBranchNInstrUsage(line);
                matchedDep.dependencyUsage.addMissedBranches(branchUsage[0]);
                matchedDep.dependencyUsage.addTotalBranches(branchUsage[1]);
                packageUsage.addMissedBranches(branchUsage[0]);
                packageUsage.addTotalBranches(branchUsage[1]);
                break;
            case 4:
                // Percentage (Don't care about this now)
                break;
            case 5:
                // Missed cyclomatic complexity
                matchedDep.dependencyUsage.addMissedCyclomaticComplexity(extractUsageNumber(line));
                packageUsage.addMissedCyclomaticComplexity(extractUsageNumber(line));
                break;
            case 6:
                // Covered cyclomatic complexity
                matchedDep.dependencyUsage.addCyclomaticComplexity(extractUsageNumber(line));
                packageUsage.addCyclomaticComplexity(extractUsageNumber(line));
                break;
            case 7:
                // Missed Lines
                matchedDep.dependencyUsage.addMissedLines(extractUsageNumber(line));
                packageUsage.addMissedLines(extractUsageNumber(line));
                break;
            case 8:
                // Covered Lines
                matchedDep.dependencyUsage.addCoveredLines(extractUsageNumber(line));
                packageUsage.addCoveredLines(extractUsageNumber(line));
                break;
            case 9:
                // Missed Methods
                matchedDep.dependencyUsage.addMissedMethods(extractUsageNumber(line));
                packageUsage.addMissedMethods(extractUsageNumber(line));
                break;
            case 10:
                // Covered Methods
                matchedDep.dependencyUsage.addCoveredMethods(extractUsageNumber(line));
                packageUsage.addCoveredMethods(extractUsageNumber(line));
                break;
            case 11:
                // Missed Classes
                matchedDep.dependencyUsage.addMissedClasses(extractUsageNumber(line));
                packageUsage.addMissedClasses(extractUsageNumber(line));
                break;
            case 12:
                // Covered Classes
                matchedDep.dependencyUsage.addCoveredClasses(extractUsageNumber(line));
                packageUsage.addCoveredClasses(extractUsageNumber(line));
                break;
            default:
                System.out.println("Could not extract usage of line: " + line);
        }
    }


    public static long extractUsageNumber(String input) {
        // Define a regex pattern to match the number within <td> tags
        Pattern pattern = Pattern.compile("<td[^>]*>(\\d+(?:,\\d+)*)</td>");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String numberStr = matcher.group(1); // Extract the matched number string
            // Remove commas and parse the string as Long
            try {
                return Long.parseLong(numberStr.replace(",", ""));
            } catch (NumberFormatException e) {
                System.out.println("Error parsing number: " + e.getMessage());
                return 0L;
            }
        } else {
            System.out.println("No match found for extracting number from <td> tag.");
            return 0L;
        }
    }

    public static long[] extractBranchNInstrUsage(String input) {
        // Define regex pattern to match the numbers inside <td> tags
        Pattern pattern = Pattern.compile("<td[^>]*>(\\d+(?:,\\d+)*)\\s+of\\s+(\\d+(?:,\\d+)*)</td>");
        Matcher matcher = pattern.matcher(input);

        long[] numbers = new long[2];
        int index = 0;

        if (matcher.find()) {
            for (int i = 1; i <= 2; i++) {
                String numberStr = matcher.group(i); // Extract the matched number string
                // Remove commas and parse the string as Long
                try {
                    numbers[index] = Long.parseLong(numberStr.replace(",", ""));
                } catch (NumberFormatException e) {
                    System.out.println("Error parsing number: " + e.getMessage());
                    numbers[index] = 0L;
                }
                index++;
            }
        }
        if (index == 1) {
            numbers[1] = 0L;
        }

        return numbers;
    }


    public static void extractAndAddPackageTotal(String inputFilePath, ProjectDependency matchedDep, String packageName) throws IOException {
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

        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {

            String line;

            // Flag to indicate if we are inside the <tbody> tag
            boolean insideTbody = false;

            // Iterate through the input HTML file
            while ((line = br.readLine()) != null) {
                // Check if we are inside the <tbody> tag
                if (line.contains("<tfoot>")) {
                    insideTbody = true;
                    line = br.readLine();
                }
                // Check if we are inside a <tr> element
                if (insideTbody && line.contains("<tr>")) {
                    line = br.readLine();
                    if (line != null) {
                        int entryIndex = 1;
                        DependencyUsage packageUsage = new DependencyUsage();
                        while ((line = br.readLine()) != null) {
                            //trContent.append(line).append("\n");
                            if (line.contains("</tr>")) {
                                break; // Stop processing when encountering </tr>
                            }
                            if (entryIndex > 0 && matchedDep.getId() != null) {
                                extractUsage(line, entryIndex, matchedDep, packageUsage);
                            }
                            entryIndex++;
                        }
                        matchedDep.packageUsageMap.put(packageName, packageUsage);
                        break;
                    }
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

    public static void writeHTMLStringToFile(String outputFilePath, String inputString) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(inputString);
        }
    }

    public static void writeHTMLTotalToFile(String outputFilePath, String inputString) throws IOException {
        // Create a temporary file to store the modified content
        File tempFile = new File(outputFilePath + ".temp");
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(outputFilePath));
            writer = new BufferedWriter(new FileWriter(tempFile));

            String line;
            boolean foundMarker = false;

            // Read lines from the original file and write them to the temporary file
            while ((line = reader.readLine()) != null) {
                // If the marker is found, insert the input string
                if (line.contains("REPLACEWITHTOTAL") && !foundMarker) {
                    writer.write(inputString);
                    writer.newLine();
                    foundMarker = true; // Set the flag to true after inserting the input string
                } else {
                    // Write the line to the temporary file only if it doesn't contain the marker
                    writer.write(line);
                    writer.newLine();
                }
            }

            // If the marker was not found, and inputString was not inserted, insert it at the end of the file
            if (!foundMarker) {
                writer.write(inputString);
                writer.newLine();
            }
        } finally {
            // Close the reader and writer
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }

        // Delete the original file
        File originalFile = new File(outputFilePath);
        originalFile.delete();

        // Rename the temporary file to the original file
        tempFile.renameTo(originalFile);
    }

    public static String loadTemplateWithReplacement(String resourceName, String dependencyName) throws IOException {
        try (InputStream inputStream = JacocoHTMLAugmenter.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            if (resourceName.equals("depOverviewTemplateStart.html")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.replace("dependency.name", dependencyName);
                        stringBuilder.append(line).append("\n");
                    }
                    return stringBuilder.toString();
                }
            } else {
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
