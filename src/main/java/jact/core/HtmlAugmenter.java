package jact.core;

import jact.depUtils.DependencyUsage;
import jact.depUtils.ProjectDependency;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jact.depUtils.PackageToDependencyResolver.packageToDependency;
import static jact.depUtils.ProjectDependencies.getTransitiveUsageMap;
import static jact.depUtils.ProjectDependency.depToDirName;
import static jact.plugin.AbstractReportMojo.getJactReportPath;
import static jact.utils.FileSystemUtils.*;

/**
 * Creates the HTML version of the JACT Report
 */
public class HtmlAugmenter {
    private static String jacocoResPath = getJactReportPath() + "jacoco-resources";
    private static ProjectDependency thisProject;
    private static DependencyUsage totalDependencyUsage;
    private static DependencyUsage completeUsage;
    private static List<String> calculatedChildIds;

    // Report summary usages:
    private static int nrDirectDeps = 0;
    private static int nrIndirectDeps = 0;

    // ONCE: Actual usage
    private static DependencyUsage summaryTotalDepUsage;
    private static DependencyUsage summaryDirectDepUsage;
    private static DependencyUsage summaryIndirectDepUsage;
    private static DependencyUsage summaryCompileScopeDepUsage;
    private static DependencyUsage summaryTotalUsage;

    // MULTIPLE: Indicated dependency heritage
    private static int nrMultipleIndirectDeps = 0;
    private static DependencyUsage summaryMultipleTotalDepUsage;
    private static DependencyUsage summaryMultipleIndirectDepUsage;
    private static DependencyUsage summaryMultipleTotalUsage;

    /**
     * Generates the entire JACT HTML report.
     *
     * @param dependenciesMap
     * @param projPackagesAndClassMap
     * @param localRepoPath
     * @param projId
     */
    public static void generateHtmlReport(Map<String, ProjectDependency> dependenciesMap,
                                          Map<String, Set<String>> projPackagesAndClassMap,
                                          String localRepoPath, String projId, boolean generateSummary) {
        thisProject = new ProjectDependency();
        totalDependencyUsage = new DependencyUsage();
        completeUsage = new DependencyUsage();
        calculatedChildIds = new ArrayList<>();

        // Report summary usages (for gathering results)
        if(generateSummary){
            summaryTotalUsage = new DependencyUsage();
            summaryTotalDepUsage = new DependencyUsage();
            summaryDirectDepUsage = new DependencyUsage();
            summaryIndirectDepUsage = new DependencyUsage();
            summaryCompileScopeDepUsage = new DependencyUsage();
            summaryMultipleTotalDepUsage = new DependencyUsage();
            summaryMultipleIndirectDepUsage = new DependencyUsage();
            summaryMultipleTotalUsage = new DependencyUsage();
        }

        // Rename the original index.html file
        String inputFilePath =
                renameFile(getJactReportPath() + "index.html", "originalIndex.html");
        // Format the index.html report:
        formatHtmlReport(inputFilePath);
        // Creates the report files and moves resource directories
        setupReport(dependenciesMap);

        try {
            extractReportAndMoveDirs(dependenciesMap, projPackagesAndClassMap, localRepoPath, projId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            createDependencyReports(dependenciesMap, generateSummary);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Creates a report summary (mainly for gathering results)
        if (generateSummary) {
            createReportSummary();
        }
    }


    private static void setupTransitiveReports(Map<String, ProjectDependency> dependenciesMap) {
        for (String depId : getTransitiveUsageMap().keySet()) {
            try {
                writeModifiedTemplateToFile("html-templates/indivDepViewTemplateStart.html",
                        dependenciesMap.get(depId).getReportPath() + "transitive-dependencies.html",
                        "<span style=\"display: inline-block;\">Transitive Dependencies from: <br>" +
                                depToDirName(dependenciesMap.get(depId)) + "</span>");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void setupDependencyReports(Map<String, ProjectDependency> dependenciesMap) {
        // Path to jacoco-resources (to be copied to subdirectories for correct icons and styling)
        copyDirectory(new File(jacocoResPath),
                new File(getJactReportPath() + "dependencies/jacoco-resources"));
        // Create the dependencies overview
        // Writes the HTML template for the Dependency Overview
        try {
            writeTemplateToFile("html-templates/depOverviewTemplateStart.html", getJactReportPath() + "dependencies/index.html");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (ProjectDependency dependency : dependenciesMap.values()) {
            String path = dependency.getReportPath();
            // Set up the directory and copy the jacoco-resources
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

    /**
     * Creates the report files and copies the required
     * resources to each dependency directory.
     *
     * @param dependenciesMap
     */
    private static void setupReport(Map<String, ProjectDependency> dependenciesMap) {

        // Create the whole project overview
        try {
            // Writes the overview HTML template
            writeTemplateToFile("html-templates/overviewTemplateStart.html", getJactReportPath() + "index.html");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        setupDependencyReports(dependenciesMap);
        setupTransitiveReports(dependenciesMap);
    }

    /**
     * Reads the html jacoco report to create corresponding ProjectDependency objects
     * for augmentation and usage tracking as well as sets up the required resources
     * and folder structure.
     *
     * @param dependenciesMap
     * @param projPackagesAndClassMap
     * @param localRepoPath
     * @param projId
     * @throws IOException
     */
    private static void extractReportAndMoveDirs(Map<String, ProjectDependency> dependenciesMap,
                                                 Map<String, Set<String>> projPackagesAndClassMap,
                                                 String localRepoPath, String projId) throws IOException {

        thisProject.setId(projId);
        thisProject.setReportPath(getJactReportPath());

        // Traverse the "report" directory:
        // Moves packages to their respective dependency directory and create their `index.html` file
        File reportDir = new File(getJactReportPath());
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] directories = reportDir.listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    String dirName = directory.getName();
                    if (!dirName.equals("dependencies") && !dirName.equals("jacoco-resources")) {
                        ProjectDependency matchedDep =
                                packageToDependency(dirName, dependenciesMap, projPackagesAndClassMap, localRepoPath);
                        // Could become problematic if packages share name with packages in dependencies
                        if (projPackagesAndClassMap.containsKey(dirName)) {
                            extractAndAddPackageTotal(getJactReportPath() + dirName +
                                    "/index.html", thisProject, dirName);
                        } else {
                            if (matchedDep.getId() != null) {
                                extractAndAddPackageTotal(getJactReportPath() + dirName +
                                        "/index.html", matchedDep, dirName);
                                moveDirectory(directory, matchedDep.getReportPath());
                            } else {
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
     *
     * @param inputFilePath
     */
    private static void formatHtmlReport(String inputFilePath) {
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
     *
     * @param dependenciesMap
     */
    private static void createDependencyReports(Map<String, ProjectDependency> dependenciesMap, boolean generateSummary) throws IOException {
        // Get all the project/dependency/package usage
        calculateAllUsages(dependenciesMap, generateSummary);

        // Write dependency usage
        writeDependenciesToFile(dependenciesMap);

        // Write project packages and overview usage
        writeOverviewToFile();
    }


    private static void writeTransitiveToFile(ProjectDependency pd) throws IOException {
        if (getTransitiveUsageMap().containsKey(pd.getId())) {
            String path = pd.getReportPath();
            writeHTMLStringToFile(path + "index.html",
                    getTransitiveUsageMap().get(pd.getId()).usageToHTML("transitive-dependencies",
                            pd.dependencyUsage, false, true));
            writeHTMLTotalToFile(path + "transitive-dependencies.html", getTransitiveUsageMap().get(pd.getId()).totalUsageToHTML());
            for (ProjectDependency child : pd.getChildDeps().values()) {
                writeHTMLStringToFile(path + "transitive-dependencies.html",
                        child.dependencyUsage.usageToHTML(depToDirName(child),
                                getTransitiveUsageMap().get(pd.getId()), false, true));
            }
            writeTemplateToFile("html-templates/endTemplate.html", path + "transitive-dependencies.html");
        }
    }

    /**
     * Writes all dependencies to the report. Dependencies
     * without parents are written to the overview and
     * child dependencies are written as entries in their
     * respective transitive reports.
     *
     * @param dependenciesMap
     * @throws IOException
     */
    private static void writeDependenciesToFile(Map<String, ProjectDependency> dependenciesMap) throws IOException {
        for (ProjectDependency pd : dependenciesMap.values()) {
            String path = pd.getReportPath();
            if (pd.rootDep) {
                writeHTMLStringToFile(getJactReportPath() + "dependencies/" + "index.html",
                        pd.dependencyUsage.usageToHTML(depToDirName(pd), totalDependencyUsage, false, false));
            }
            writeHTMLTotalToFile(path + "index.html", pd.dependencyUsage.totalUsageToHTML());
            writeTransitiveToFile(pd);
            pd.writePackagesToFile(path, pd.dependencyUsage);
            // Write the end of the template here
            writeModifiedTemplateToFile("html-templates/endTemplate.html",
                    path + "index.html", depToDirName(pd));
        }
        // Writes the HTML template for the Dependency Overview
        writeTemplateToFile("html-templates/endTemplate.html", getJactReportPath() + "dependencies/index.html");
    }

    /**
     * Writes the complete project overview
     * as well as the dependency overview.
     *
     * @throws IOException
     */
    private static void writeOverviewToFile() throws IOException {
        // Write the total dependency usage AND its entry in the overview
        writeHTMLStringToFile(getJactReportPath() + "index.html",
                totalDependencyUsage.usageToHTML("dependencies", completeUsage, false, false));
        writeHTMLTotalToFile(getJactReportPath() + "dependencies/index.html", totalDependencyUsage.totalUsageToHTML());

        // Write the project package overview entries:
        for (Map.Entry<String, DependencyUsage> entry : thisProject.packageUsageMap.entrySet()) {
            try {
                writeHTMLStringToFile(getJactReportPath() + "index.html",
                        entry.getValue().usageToHTML(entry.getKey(), completeUsage, true, false));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // Write the overview total: Project + Dependencies (incl. transitive)
        writeHTMLTotalToFile(getJactReportPath() + "index.html", completeUsage.totalUsageToHTML());

        // Writes the overview HTML template
        writeTemplateToFile("html-templates/endTemplate.html", getJactReportPath() + "index.html");
    }


    /**
     * Calculates the transitive dependency usage
     * for the input dependency recursively adding
     * the children dependencies.
     *
     * @param dependency
     * @param includeSelf
     * @return DependencyUsage
     */
    private static DependencyUsage calculateTransitiveDepUsage(ProjectDependency dependency, boolean includeSelf) {
        DependencyUsage currUsage = new DependencyUsage();
        if (!calculatedChildIds.contains(dependency.getId())) {
            for (ProjectDependency child : dependency.getChildDeps().values()) {
                currUsage.addAll(calculateTransitiveDepUsage(child, true));
            }
        }
        if (includeSelf) {
            currUsage.addAll(dependency.dependencyUsage);
        }
        return currUsage;
    }


    /**
     * Calculates the total usage for all layers of the report.
     * Layers include the complete overview, dependency overview
     * individual dependencies and their transitive dependencies.
     *
     * @param dependenciesMap
     */
    private static void calculateAllUsages(Map<String, ProjectDependency> dependenciesMap, boolean generateSummary) {
        for (ProjectDependency dependency : dependenciesMap.values()) {
            if(generateSummary){
                summaryTotalDepUsage.addAll((dependency.dependencyUsage));
                if(dependency.rootDep) {
                    nrDirectDeps++;
                    summaryDirectDepUsage.addAll(dependency.dependencyUsage);
                }else{
                    nrIndirectDeps++;
                    summaryIndirectDepUsage.addAll(dependency.dependencyUsage);
                }
                if (dependency.getScope().equals("compile")) {
                    summaryCompileScopeDepUsage.addAll(dependency.dependencyUsage);
                }
            }
            if (!dependency.getChildDeps().isEmpty()) {
                DependencyUsage indirectDepsUsage = calculateTransitiveDepUsage(dependency, false);
                if(generateSummary){
                    // Multiple Indirect Dependencies
                    nrMultipleIndirectDeps += dependency.getChildDeps().size();
                    summaryMultipleIndirectDepUsage.addAll(indirectDepsUsage);
                }
                dependency.dependencyUsage.addAll(indirectDepsUsage);
                getTransitiveUsageMap().get(dependency.getId()).addAll(indirectDepsUsage);
                calculatedChildIds.add(dependency.getId());
            }
            // Calculate the total
            // Only ROOT dependencies are added, since the transitive
            // cost was included in the previous if-statement.
            if (dependency.rootDep) {
                totalDependencyUsage.addAll(dependency.dependencyUsage);
            }
        }
        // Calculate the overall total (project + dependencies)
        completeUsage.addAll(totalDependencyUsage);
        completeUsage.addAll(thisProject.dependencyUsage);
        if(generateSummary){
            // Totals: Dependencies + Project
            summaryTotalUsage.addAll(summaryTotalDepUsage);
            summaryTotalUsage.addAll(thisProject.dependencyUsage);
            // Multiple Totals: Dependencies + Project
            summaryMultipleTotalDepUsage.addAll(totalDependencyUsage);
            summaryMultipleTotalUsage.addAll(completeUsage);
        }
    }

    /**
     * Loads a html template from resources.
     *
     * @param resourceName
     * @return
     * @throws IOException
     */
    private static String loadTemplate(String resourceName) throws IOException {
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
     *
     * @param filename
     * @param outputFilePath
     * @throws IOException
     */
    private static void writeTemplateToFile(String filename, String outputFilePath) throws IOException {
        String templateContent = loadTemplate(filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(templateContent);
        }
    }


    /**
     * Extracts and adds the usage of a package to its
     * corresponding dependency.
     *
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
     *
     * @param input
     * @return long
     */
    private static long extractUsageNumber(String input) {
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
     *
     * @param input
     * @return
     */
    private static long[] extractBranchNInstrUsage(String input) {
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
     *
     * @param inputFilePath
     * @param matchedDep
     * @param packageName
     * @throws IOException
     */
    private static void extractAndAddPackageTotal(String inputFilePath, ProjectDependency matchedDep, String packageName) throws IOException {
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


    private static void writeModifiedTemplateToFile(String filename, String outputFilePath, String dependencyName) throws IOException {
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
     *
     * @param outputFilePath
     * @param inputString
     * @throws IOException
     */
    private static void writeHTMLTotalToFile(String outputFilePath, String inputString) throws IOException {
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

    private static String loadTemplateWithReplacement(String resourceName, String dependencyName) throws IOException {
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

    private static void createReportSummary() {
        String outputFile = getJactReportPath() + "jactReportSummary.md";

        // Currently missing a complete total: Project + Deps
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(thisProject.dependencyUsage.usageToMarkdown("PROJECT USAGE") + "  \n");
            writer.write("----------------------------------------------------------------  \n");
            writer.write("### **NUMBER OF DEPENDENCIES:** " + "  \n");
            writer.write("- **#DIRECT:** " + nrDirectDeps + "  \n");
            writer.write("- **#INDIRECT:** " + nrIndirectDeps + "  \n");
            writer.write(summaryCompileScopeDepUsage.usageToMarkdown("COMPILE-SCOPE USAGE") + "  \n");
            writer.write(summaryDirectDepUsage.usageToMarkdown("DIRECT DEPENDENCY USAGE") + "  \n");
            writer.write(summaryIndirectDepUsage.usageToMarkdown("INDIRECT DEPENDENCY USAGE") + "  \n");
            writer.write(summaryTotalDepUsage.usageToMarkdown("TOTAL DEPENDENCY USAGE") + "  \n");
            writer.write(summaryTotalUsage.usageToMarkdown("TOTAL USAGE _[Project + Dependencies]_") + "  \n");
            writer.write("----------------------------------------------------------------  \n");
            // MULTIPLE
            writer.write("## MULTIPLE:  \n");
            writer.write("_If indirect dependencies are shared by direct dependencies they are added to the  \n" +
                    "cost of all of their parent dependencies in the visual HTML version but only included  \n" +
                    "once in the Uber-jar._  \n");
            writer.write("### **NUMBER OF DEPENDENCIES:** " + "  \n");
            writer.write("- **#DIRECT:** " + nrDirectDeps + "  \n");
            writer.write("- **#MULTIPLE INDIRECT:** " + nrMultipleIndirectDeps + "  \n");
            writer.write(summaryMultipleIndirectDepUsage.usageToMarkdown("MULTIPLE INDIRECT USAGE") + "  \n");
            writer.write(summaryMultipleTotalDepUsage.usageToMarkdown("MULTIPLE TOTAL DEPENDENCY USAGE") + "  \n");
            writer.write(summaryMultipleTotalUsage.usageToMarkdown("MULTIPLE TOTAL USAGE _[Project + Dependencies]_"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}