package jact.core;

import jact.depUtils.DependencyUsage;
import jact.depUtils.PackageToDependencyResolver;
import jact.depUtils.ProjectDependency;
import jact.depUtils.TransitiveDependencies;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jact.depUtils.ProjectDependencies.rootDepIds;
import static jact.depUtils.ProjectDependencies.transitiveUsageMap;
import static jact.depUtils.ProjectDependency.depToDirName;
import static jact.plugin.AbstractReportMojo.getReportPath;
import static jact.utils.FileSystemUtils.*;

/**
 * Creates the HTML version of the JACT Report
 */
public class HtmlAugmenter {
    public static String jacocoResPath = getReportPath() + "jacoco-resources";
    private static ProjectDependency thisProject;
    private static DependencyUsage totalDependencyUsage;
    private static DependencyUsage completeUsage;

    private static List<String> calculatedChildIds;

    public static void generateHtmlReport(Map<String, ProjectDependency> dependenciesMap,
                                          Map<String, Set<String>> projPackagesAndClassMap,
                                          String localRepoPath, String projId){
        thisProject = new ProjectDependency();
        totalDependencyUsage = new DependencyUsage();
        completeUsage = new DependencyUsage();
        calculatedChildIds = new ArrayList<>();

        // Rename the original index.html file
        String inputFilePath =
                renameFile(getReportPath() + "index.html", "originalIndex.html");
        // Format the index.html report:
        formatHtmlReport(inputFilePath);

        setupReportPaths(dependenciesMap);

        try {
            extractReportAndMoveDirs(dependenciesMap, projPackagesAndClassMap, localRepoPath, projId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            createDependencyReports(dependenciesMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void setupTransitivePaths(){
        for(TransitiveDependencies transitiveDeps : transitiveUsageMap.values()){
            for(String path : transitiveDeps.getReportPaths()){
                copyDirectory(new File(jacocoResPath),
                        new File(path + "jacoco-resources"));
                String parentDir = new File(path).getParent();
                if (new File(parentDir).exists()) {
                    try {
                        writeModifiedTemplateToFile("html-templates/indivDepViewTemplateStart.html",
                                path + "index.html",
                                "<span style=\"display: inline-block;\">Transitive Dependencies from: <br>" +
                                        transitiveDeps.getParentDirName() + "</span>");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static void setupDependencyPaths(Map<String, ProjectDependency> dependenciesMap){
        // Create a directory for the dependency coverage
        createDir(getReportPath() + "dependencies");

        // Path to jacoco-resources (to be copied to subdirectories for correct icons and styling)
        copyDirectory(new File(jacocoResPath),
                new File(getReportPath() + "dependencies/jacoco-resources"));
        // Create the dependencies overview
        // Writes the HTML template for the Dependency Overview
        try {
            writeTemplateToFile("html-templates/depOverviewTemplateStart.html", getReportPath() + "dependencies/index.html");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (ProjectDependency dependency : dependenciesMap.values()){
            for(String path : dependency.getReportPaths()){
                // Set up the directory and copy the jacoco-resources
                createDir(path);
                copyDirectory(new File(jacocoResPath),
                        new File(path + "jacoco-resources"));
                // Set up the index.html file
                try {
                    writeModifiedTemplateToFile("html-templates/indivDepViewTemplateStart.html",
                            path + "index.html", depToDirName(dependency));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private static void setupReportPaths(Map<String, ProjectDependency> dependenciesMap){

        // Create the whole project overview
        try {
            // Writes the overview HTML template
            writeTemplateToFile("html-templates/overviewTemplateStart.html", getReportPath() + "index.html");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        setupDependencyPaths(dependenciesMap);
        setupTransitivePaths();
    }

    /**
     * Reads the html jacoco report to create corresponding ProjectDependency objects
     * for augmentation and usage tracking as well as sets up the required resources
     * and folder structure.
     * @param dependenciesMap
     * @param projPackagesAndClassMap
     * @param localRepoPath
     * @param projId
     * @throws IOException
     */
    public static void extractReportAndMoveDirs(Map<String, ProjectDependency> dependenciesMap,
                                                Map<String, Set<String>> projPackagesAndClassMap,
                                                String localRepoPath, String projId) throws IOException {

        thisProject.setId(projId);
        thisProject.addReportPath(getReportPath());

        // Traverse the "report" directory:
        // Moves packages to their respective dependency directory and create their `index.html` file
        File reportDir = new File(getReportPath());
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    String dirName = directory.getName();
                    if (!dirName.equals("dependencies") && !dirName.equals("jacoco-resources")) {
                        ProjectDependency matchedDep =
                                PackageToDependencyResolver.packageToDepPaths(dirName, dependenciesMap, projPackagesAndClassMap, localRepoPath);
                        // Could become problematic if packages share name with packages in dependencies
                        if (projPackagesAndClassMap.containsKey(dirName)) {
                            extractAndAddPackageTotal(getReportPath() + dirName +
                                    "/index.html", thisProject, dirName);
                        } else {
                            if (matchedDep.getId() != null) {
                                extractAndAddPackageTotal(getReportPath() + dirName +
                                        "/index.html", matchedDep, dirName);
                            }
                            if (matchedDep.getReportPaths().size() == 1) {
                                moveDirectory(directory, matchedDep.getReportPaths().get(0));
                            } else {
                                // Handle dependencies with the same transitive dependencies.
                                for (String path : matchedDep.getReportPaths()) {
                                    copyDirectory(directory, new File(path + directory.getName()));
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


    /**
     * Formats the input HTML report with
     * newlines and correct indentation.
     * @param inputFilePath
     */
    public static void formatHtmlReport(String inputFilePath){
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
    }


    /**
     * Creates the complete jact-report by reading the usage and writing
     * to the corresponding files.
     * @param dependenciesMap
     */
    public static void createDependencyReports(Map<String, ProjectDependency> dependenciesMap) throws IOException{
        // Get all the project/dependency/package usage
        calculateAllUsages(dependenciesMap);

        // Write dependency usage
        writeTransitiveToFile(dependenciesMap);
        writeDependenciesToFile(dependenciesMap);
        writeTransitiveEndToFile();

        // Write project packages and overview usage
        writeOverviewToFile();
    }


    private static void writeTransitiveToFile(Map<String, ProjectDependency> dependenciesMap) throws IOException {
        for(TransitiveDependencies transitiveDeps : transitiveUsageMap.values()){
            for(String path : transitiveDeps.getReportPaths()){
                File currDir = new File(path);
                File parentDir = currDir.getParentFile();
                writeHTMLStringToFile(parentDir + "/index.html",
                        transitiveDeps.transitiveUsage.usageToHTML("transitive-dependencies",
                                dependenciesMap.get(parentDir.getName()).dependencyUsage, false));
                writeHTMLTotalToFile(path + "index.html", transitiveDeps.transitiveUsage.totalUsageToHTML());
            }
        }
    }

    private static void writeTransitiveEndToFile() throws IOException {
        for(TransitiveDependencies transitiveDeps : transitiveUsageMap.values()){
            for(String path : transitiveDeps.getReportPaths()){
                writeTemplateToFile("html-templates/overviewTemplateEnd.html", path + "index.html");
            }
        }
    }


    private static void writeDependenciesToFile(Map<String, ProjectDependency> dependenciesMap) throws IOException {
        for (ProjectDependency pd : dependenciesMap.values()) {
            DependencyUsage entryReferenceUsage;
            for (String path : pd.getReportPaths()) {
                File currDir = new File(path);
                File parentDir = currDir.getParentFile();
                if(parentDir.getName().equals("dependencies")){
                    entryReferenceUsage = totalDependencyUsage;
                }else{
                    String parentDepDir = parentDir.getParentFile().getName();
                    entryReferenceUsage = transitiveUsageMap.get(parentDepDir).transitiveUsage;
                }
                writeHTMLStringToFile(parentDir + "/index.html",
                        pd.dependencyUsage.usageToHTML(currDir.getName(), entryReferenceUsage, false));
                writeHTMLTotalToFile(path + "index.html", pd.dependencyUsage.totalUsageToHTML());
                pd.writePackagesToFile(path, pd.dependencyUsage);
                // Write the end of the template here
                writeModifiedTemplateToFile("html-templates/indivDepViewTemplateEnd.html",
                        path + "index.html", depToDirName(pd));
            }
        }
        // Writes the HTML template for the Dependency Overview
        writeTemplateToFile("html-templates/depOverviewTemplateEnd.html", getReportPath() + "dependencies/index.html");
    }


    private static void writeOverviewToFile() throws IOException {
        // Write the total dependency usage AND its entry in the overview
        writeHTMLStringToFile(getReportPath() + "index.html", totalDependencyUsage.usageToHTML("dependencies", completeUsage, false));
        writeHTMLTotalToFile(getReportPath() + "dependencies/index.html", totalDependencyUsage.totalUsageToHTML());

        // Write the project package overview entries:
        for (Map.Entry<String, DependencyUsage> entry : thisProject.packageUsageMap.entrySet()) {
            try {
                writeHTMLStringToFile(getReportPath() + "index.html", entry.getValue().usageToHTML(entry.getKey(),completeUsage, true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // Write the overview total: Project + Dependencies (incl. transitive)
        writeHTMLTotalToFile(getReportPath() + "index.html", completeUsage.totalUsageToHTML());

        // Writes the overview HTML template
        writeTemplateToFile("html-templates/overviewTemplateEnd.html", getReportPath() + "index.html");
    }


    private static DependencyUsage calculateTransitiveDepUsage(ProjectDependency dependency, boolean includeSelf){
        DependencyUsage currUsage = new DependencyUsage();
        if(!calculatedChildIds.contains(dependency.getId())){
            for(ProjectDependency child : dependency.getChildDeps().values()){
                currUsage.addAll(calculateTransitiveDepUsage(child, true));
            }
        }
        if(includeSelf){
            currUsage.addAll(dependency.dependencyUsage);
        }
        return currUsage;
    }


    /**
     * Calculates the total usage for all layers of the report.
     * Layers include the complete overview, dependency overview
     * individual dependencies and their transitive dependencies.
     * @param dependenciesMap
     */
    private static void calculateAllUsages(Map<String, ProjectDependency> dependenciesMap){
        for(ProjectDependency dependency : dependenciesMap.values()){
            if(!dependency.getChildDeps().isEmpty()){
                DependencyUsage transitiveDepsUsage = calculateTransitiveDepUsage(dependency, false);
                dependency.dependencyUsage.addAll(transitiveDepsUsage);
                transitiveUsageMap.get(depToDirName(dependency)).transitiveUsage.addAll(transitiveDepsUsage);
                calculatedChildIds.add(dependency.getId());
            }
            // Calculate the total
            // Only ROOT dependencies are added, since the transitive
            // cost was included in the previous if-statement.
            if(rootDepIds.contains(dependency.getId())){
                totalDependencyUsage.addAll(dependency.dependencyUsage);
            }
        }
        // Calculate the overall total (project + dependencies)
        completeUsage.addAll(totalDependencyUsage);
        completeUsage.addAll(thisProject.dependencyUsage);
    }


    /**
     * Loads a html template from resources.
     * @param resourceName
     * @return
     * @throws IOException
     */
    public static String loadTemplate(String resourceName) throws IOException {
        try (InputStream inputStream = HtmlAugmenter.class.getClassLoader().getResourceAsStream(resourceName)) {
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

    /**
     * Writes a html template from resources.
     * @param filename
     * @param outputFilePath
     * @throws IOException
     */
    public static void writeTemplateToFile(String filename, String outputFilePath) throws IOException {
        String templateContent = loadTemplate(filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(templateContent);
        }
    }


    /**
     * Extracts and adds the usage of a package to its
     * corresponding dependency.
     * @param line
     * @param entryIndex
     * @param matchedDep
     * @param packageUsage
     */
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
                matchedDep.dependencyUsage.addTotalLines(extractUsageNumber(line));
                packageUsage.addTotalLines(extractUsageNumber(line));
                break;
            case 9:
                // Missed Methods
                matchedDep.dependencyUsage.addMissedMethods(extractUsageNumber(line));
                packageUsage.addMissedMethods(extractUsageNumber(line));
                break;
            case 10:
                // Covered Methods
                matchedDep.dependencyUsage.addTotalMethods(extractUsageNumber(line));
                packageUsage.addTotalMethods(extractUsageNumber(line));
                break;
            case 11:
                // Missed Classes
                matchedDep.dependencyUsage.addMissedClasses(extractUsageNumber(line));
                packageUsage.addMissedClasses(extractUsageNumber(line));
                break;
            case 12:
                // Covered Classes
                matchedDep.dependencyUsage.addTotalClasses(extractUsageNumber(line));
                packageUsage.addTotalClasses(extractUsageNumber(line));
                break;
            default:
                System.out.println("Could not extract usage of line: " + line);
        }
    }


    /**
     * Extracts the usage of all but the instructions/branches from
     * the jacoco report.
     * @param input
     * @return long
     */
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

    /**
     * Extracts the usage of instructions/branches from
     * the jacoco report.
     * @param input
     * @return
     */
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

    /**
     * Reads the jacoco report and extracts its usage.
     * @param inputFilePath
     * @param matchedDep
     * @param packageName
     * @throws IOException
     */
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

    /**
     * Writes the total usage to the html report.
     * @param outputFilePath
     * @param inputString
     * @throws IOException
     */
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
        try (InputStream inputStream = HtmlAugmenter.class.getClassLoader().getResourceAsStream(resourceName)) {
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