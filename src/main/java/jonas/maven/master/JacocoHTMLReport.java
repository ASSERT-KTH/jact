package jonas.maven.master;

import org.apache.maven.model.Dependency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JacocoHTMLReport {

    //public static void moveDepDirs(List<Dependency> dependencies) {
    public static void main(String[] args) {
        System.out.println("HELLO I STARTED");
        // Generate sets of words from dependencies
        List<Set<String>> setOfAllDeps = new ArrayList<>();
//        for (Dependency dependency : dependencies) {
//            Set<String> depWordsSet = new HashSet<>();
//
//            String depGroupId = dependency.getGroupId();
//            depWordsSet.addAll(Arrays.asList(depGroupId.split("[.-]")));
//
//            String depArtifactId = dependency.getArtifactId();
//            depWordsSet.addAll(Arrays.asList(depArtifactId.split("[.-]")));
//
//            setOfAllDeps.add(depWordsSet);
//        }
        Set<String> depWordsSet2 = new HashSet<>();
        String dep = "org.apache.commons.math3";
        depWordsSet2.addAll(Arrays.asList(dep.split("[.-]")));
        setOfAllDeps.add(depWordsSet2);

        createDepDirs();

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
                        boolean containsAny = depWordsSet.stream().anyMatch(dirName::contains);
                        System.out.println("BOOL: " + containsAny);
                        if (containsAny) {
                            moveDirectory(directory, "./report/Dep1");
                            break; // Move to next directory after moving this one
                        }
                    }
                }
            }
        }
    }

    private static void createDepDirs(){
        // Will take a list of dependencies later
        String directoryPath = "./report/dependencies";

        File dependencies = new File(directoryPath);

        File dep1 = new File("./report/Dep1");

        dep1.mkdirs();

        // Use the mkdirs() method to create the directory along with any necessary parent directories
        boolean success = dependencies.mkdirs();

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
