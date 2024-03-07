package jonas.maven.master;

import org.apache.maven.model.Dependency;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JacocoHTMLAugmenter {
    public static final String REPORTPATH = "./target/jact-report/";
    public static final String jacocoResPath = REPORTPATH + "jacoco-resources";

    private static ProjectDependency thisProject = new ProjectDependency();
    static String projId = CompleteCoverageMojo.projectGroupId + ":" + CompleteCoverageMojo.projectGroupId +
            ":" + CompleteCoverageMojo.version;


    public static void moveDepDirs(List<ProjectDependency> dependencies) {
        thisProject.setId(projId);
        // Create a directory for the dependency coverage
        createDir(REPORTPATH + "dependencies");

        // Copy the JACT logo to the jacoco-resources


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
                                                        parentDir + "/index.html",
                                                        "<span style=\"display: inline-block;\">Transitive Dependencies from: <br>" +
                                                                parentDepName+"</span>");

                                                // Write the transitive-dependencies entry
                                                //writeTemplateToFile("transitiveEntry.html", parentDir.getParentFile() + "/index.html");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

//                                        try {
//                                            //writeModifiedTemplateToFile("depEntry.html", parentDir + "/index.html", depToDirName(matchedDep));
//                                            writeHTMLStringToFile(parentDir + "/index.html", matchedDep.usageToHTML());
//                                        } catch (IOException e) {
//                                            throw new RuntimeException(e);
//                                        }
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
                                            //writeTemplateToFile("transitiveEntry.html", parentDir.getParentFile() + "/index.html");
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
//                                    try {
//                                        //writeModifiedTemplateToFile("depEntry.html", parentDir + "/index.html", depToDirName(matchedDep));
//                                        writeHTMLStringToFile(parentDir + "/index.html", matchedDep.dependencyUsage.usageToHTML(depToDirName(matchedDep), false));
//                                    } catch (IOException e) {
//                                        throw new RuntimeException(e);
//                                    }

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

    public static String depToDirName(ProjectDependency dependency){
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


    public static void createDependencyReports(String projectName, List<ProjectDependency> dependencies) {

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
            // Write total here:
            //extractAndAppendOverallTotal(inputFilePath, outputFilePath);
            //extractAndAppendHTML(inputFilePath, outputFilePath, projectNameSet); // Adds the project coverage
            //writeTemplateToFile(templateFilePathX, outputFilePath);
            //writeTemplateToFile(templateFilePath2, outputFilePath);
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
            // TODO WRITE THE TOTAL ENTRIES FROM HERE
//            for(ProjectDependency pd : dependencies) {
//                for (String path : pd.getReportPaths()) {
//                    // Get the parent directory of the current path
//                    File parentDir = new File(path).getParentFile();
//
//                    // Ensure parentDir is not null and it's a directory
//                    if (parentDir != null && parentDir.isDirectory() &&
//                            (parentDir.getName().equals("transitive-dependencies") ||
//                                    parentDir.getName().equals("dependencies"))) {
//                        try {
//                            writeHTMLStringToFile(parentDir + "/index.html", pd.dependencyUsage.usageToHTML(depToDirName(pd)));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }
//            }


            //writeTemplateToFile(templateFilePath2, outputFilePath);
            System.out.println("Writing the dependency overview completed successfully.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        List<String> writtenPaths = new ArrayList<>();
        List<String> writtenEntryPaths = new ArrayList<>();

        DependencyUsage totalDepUsage = new DependencyUsage();
        for(ProjectDependency pd : dependencies){

            DependencyUsage currTotal = new DependencyUsage();
            currTotal = calculateTotalForAllLayers(pd, writtenPaths, writtenEntryPaths, currTotal);
            if(!pd.writtenEntryToFile){
                pd.writtenEntryToFile = true;
                for (String path : pd.getReportPaths()) {
                    //System.out.println("DEP: " + currDependency.getId() + " PATH: " + path);
                    File currDir = new File(path);
                    File parentDir = currDir.getParentFile();
                    File grandParentDir = parentDir.getParentFile();
                    try {
                        writeHTMLStringToFile(parentDir + "/index.html", pd.dependencyUsage.usageToHTML(currDir.getName(), currTotal,false));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            pd.writePackagesToFile(currTotal);


            totalDepUsage.addAll(pd.dependencyUsage);
        }
        try {
            DependencyUsage overallTotal = new DependencyUsage();
            overallTotal.addAll(totalDepUsage);
            overallTotal.addAll(thisProject.packageUsageMap.get(CompleteCoverageMojo.projectGroupId));

            writeHTMLStringToFile(REPORTPATH + "/index.html", totalDepUsage.usageToHTML("dependencies", overallTotal, false));
            writeHTMLTotalToFile(REPORTPATH + "dependencies/index.html", totalDepUsage.totalUsageToHTML());

            //String test = thisProject.packageUsageMap.get(CompleteCoverageMojo.projectGroupId).usageToHTML(CompleteCoverageMojo.projectGroupId, true);
            //System.out.println(test);
            writeHTMLStringToFile(REPORTPATH + "/index.html",
                    thisProject.packageUsageMap.get(CompleteCoverageMojo.projectGroupId).usageToHTML(CompleteCoverageMojo.projectGroupId, overallTotal,true));
            writeHTMLTotalToFile(REPORTPATH + "index.html", overallTotal.totalUsageToHTML());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static ProjectDependency dirNameToDep(String directoryName, List<ProjectDependency> dependencies){
        for(ProjectDependency pd : dependencies){
            if(depToDirName(pd).equals(directoryName)){
                return pd;
            }
        }
        throw new RuntimeException("Could not find a matching dependency.");
    }

    public static DependencyUsage calculateTotalForAllLayers(ProjectDependency currDependency, List<String> writtenPaths, List<String> writtenEntryPaths, DependencyUsage currTotal ){
        List<String> writtenTotalPaths = new ArrayList<>();
        // Keep track of dependencies that have already been checked out.
        //DependencyUsage currTotal = new DependencyUsage();
            if(!currDependency.getChildDeps().isEmpty()){
                DependencyUsage childTotal = new DependencyUsage();
                for(ProjectDependency child : currDependency.getChildDeps()){
                    DependencyUsage childUsage = new DependencyUsage();
                    childTotal.addAll(calculateTotalForAllLayers(child, writtenPaths, writtenEntryPaths, childUsage));
                    currDependency.dependencyUsage.addAll(childTotal);
                }

                for (String path : currDependency.getReportPaths()) {
                    if(!writtenPaths.contains(path)){
                        writtenPaths.add(path);
                        //System.out.println("DEP: " + currDependency.getId() + " PATH: " + path);
                        File currDir = new File(path);
                        File parentDir = currDir.getParentFile();
                        File grandParentDir = parentDir.getParentFile();
                        try {
                            // Writing the dependency total as an entry
                            DependencyUsage totalForBars = currTotal;
                            totalForBars.addAll(currDependency.dependencyUsage);
                            System.out.println("Writing total to: " + currDir + "/transitive-dependencies/index.html");
                            writeHTMLStringToFile(currDir + "/index.html", childTotal.usageToHTML("transitive-dependencies", totalForBars, false));
                            writeHTMLTotalToFile(currDir + "/transitive-dependencies/index.html", childTotal.totalUsageToHTML());
                            // Writing the total within the transitive dependency
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }
            // Calculate the total.
            currTotal.addAll(currDependency.dependencyUsage);


            // Write here
            if(!currDependency.writtenTotalToFile){
                currDependency.writtenTotalToFile = true;
                for (String path : currDependency.getReportPaths()) {
                    //System.out.println("DEP: " + currDependency.getId() + " PATH: " + path);
                    File currDir = new File(path);
                    File parentDir = currDir.getParentFile();
                    File grandParentDir = parentDir.getParentFile();
//                if(!writtenPaths.contains(path)){
//                    writtenPaths.add(path);
//                    // Needs to be moved and checked
//                    try {
//                        // Write the total for its own index.html
//                        writeHTMLStringToFile(path + "/index.html", currTotal.totalUsageToHTML());
//                        writeHTMLStringToFile(path + "/index.html", "\n</tfoot>\n<tbody>\n");
//
//
//                        //writeHTMLStringToFile(parentDir + "/index.html", currDependency.dependencyUsage.totalUsageToHTML());
//                        //writeHTMLStringToFile(parentDir + "/index.html", "\n</tfoot>\n<tbody>\n");
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }

//                if(!writtenEntryPaths.contains(parentDir.getPath()) &&
//                                            (currDir.getName().equals("transitive-dependencies") ||
//                                                    currDir.getName().equals("dependencies"))){
//                    writtenEntryPaths.add(parentDir.getPath());
                    try {
                        // Writing the dependency total as an entry

                        //writeHTMLStringToFile(currDir + "/index.html", currDependency.dependencyUsage.totalUsageToHTML());
                        writtenEntryPaths.add(path);
                        //System.out.println("WRITING TO: " + parentDir + " " + "WITH " + currDir.getName());
                        //writeHTMLStringToFile(parentDir + "/index.html", currDependency.dependencyUsage.usageToHTML(currDir.getName(), ,false));
                        if(new File(currDir + "/index.html").exists()){
                            writeHTMLTotalToFile(currDir + "/index.html", currDependency.dependencyUsage.totalUsageToHTML());
                        }


                        // Writing the total within the transitive dependency
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    //}

                    // Get the parent directory of the current path
                    //writtenPaths.add(grandParentDir.getPath());

                    // Ensure parentDir is not null and it's a directory
//                if (parentDir != null && parentDir.isDirectory() && !currDependency.writtenToFile) {
//                    try {
//                        //  &&
//                        //                            (parentDir.getName().equals("transitive-dependencies") ||
//                        //                                    parentDir.getName().equals("dependencies"))
//                        // Writing entry
//                        writeHTMLStringToFile(parentDir + "/index.html", currDependency.dependencyUsage.usageToHTML(depToDirName(currDependency)));
//                        currDependency.writtenToFile = true;
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }

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
                        if(matchedDep.getId() != null){
                            if(new File(matchedDep.getReportPaths().getFirst() + "/" +
                                    packageName + "/index.html").exists()){
                                extractAndAddPackageTotal(matchedDep.getReportPaths().getFirst() + "/" +
                                        packageName + "/index.html", matchedDep, packageName);
                            }
                        }else if(packageName.equals(CompleteCoverageMojo.projectGroupId)){
                            extractAndAddPackageTotal(REPORTPATH + CompleteCoverageMojo.projectGroupId +
                                            "/index.html", thisProject, packageName);
                        }
                        if (!matchedDep.getReportPaths().isEmpty()) {
                            for (String path : matchedDep.getReportPaths()) {
                                // Get the parent directory of the current path
                                File parentDir = new File(path).getParentFile();

                                // Ensure parentDir is not null and it's a directory
                                if (parentDir != null && parentDir.isDirectory()) {
                                    // Construct the path to the parent directory's index.html file
                                    // TODO refactor this
                                    String indexPath = new File(parentDir, "index.html").getAbsolutePath();
                                    indexPath = path + "/index.html";
                                    // Write to the parent directory's index.html file
//                                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(indexPath, true))) {
//                                        bw.write(trContent.toString());
//                                    }
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
        //System.out.println("PACKAGE NAME");
        int startIndex = line.indexOf("el_package\">") + "el_package\">".length();
        int endIndex = line.indexOf("</a>", startIndex);
        //System.out.println(line.substring(startIndex, endIndex));
        return line.substring(startIndex, endIndex);
    }


    private static void extractUsage(String line, int entryIndex, ProjectDependency matchedDep, DependencyUsage packageUsage){
        switch(entryIndex) {
            case 1:
                // Missed and Covered instructions
                long[] instrUsage = extractBranchNInstrUsage(line);
                matchedDep.dependencyUsage.addMissedInstructions(instrUsage[0]);
                matchedDep.dependencyUsage.addTotalInstructions(instrUsage[1]);
                packageUsage.addMissedInstructions(instrUsage[0]);
                packageUsage.addTotalInstructions(instrUsage[1]);
//                if(matchedDep.getId().equals("com.google.code.findbugs:jsr305:3.0.2")){
//                    System.out.println("CHECK ME OUT: \n"  + instrUsage[0] + " of " +
//                            instrUsage[1]);
//                    System.out.println("THIS IS THE LINE: " + line);
//                }
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
                return 0L; // Or handle the error as appropriate
            }
        } else {
            System.out.println("No match found for extracting number from <td> tag.");
            return 0L; // Or handle the absence of match as appropriate
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
                    numbers[index] = 0L; // Or handle the error as appropriate
                }
                index++;
            }
        }

        // If only one number found, set the second number to 0
        if (index == 1) {
            numbers[1] = 0L;
        }

        return numbers;
    }


    public static void extractAndAddPackageTotal(String inputFilePath, ProjectDependency matchedDep, String packageName) throws IOException {
        //System.out.println("READING: " + inputFilePath);
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
                            if (entryIndex > 0 && (matchedDep.getId() != null ||
                                    packageName.equals(CompleteCoverageMojo.projectGroupId))) {
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
                // TODO REMOVE THIS
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
