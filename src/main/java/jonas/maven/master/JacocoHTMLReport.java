package jonas.maven.master;

import org.apache.maven.model.Dependency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
}
