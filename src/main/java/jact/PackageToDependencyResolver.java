package jact;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PackageToDependencyResolver {
    /**
     * Take a list of all dependencies,
     * add the package to that dependency
     * Create a project 'dependency' for
     * all project packages
     */


    // Handle the scenario where two or more packages have the same name:


    public static List<ProjectDependency> packageToDepPaths(String packageName, List<ProjectDependency> dependencies) {
        // List of dependencies along with their coordinates

        List<ProjectDependency> currMatchedDeps = new ArrayList<>();
        Map<String, Set<String>> projectPackages = CompleteCoverageMojo.getProjectPackagesAndClasses();

        //List<ProjectDependency> dependencies = ProjectDependencies.getAllProjectDependencies();


        // Directory where your Maven dependencies are stored
        String mavenRepositoryDir = CompleteCoverageMojo.getLocalRepoPath();

        // Iterate over each dependency
        for (ProjectDependency dependency : dependencies) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();

            // Construct the path to the JAR file
            String jarFilePath = mavenRepositoryDir + "/" + groupId.replace('.', '/') +
                    "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
            File jarFile = new File(jarFilePath);

            // Check if the JAR file exists
            if (jarFile.exists()) {
                try (ZipFile zipFile = new ZipFile(jarFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if the entry is a class file within the desired package
                        if (entry.getName().startsWith(packageName.replace('.', '/')) && entry.getName().endsWith(".class")) {
                            System.out.println("Package: " + packageName + " matched to dependency: " + groupId + ":" + artifactId + ":" + version);
                            currMatchedDeps.add(dependency);
                            break;
                            //return dependency;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return currMatchedDeps;
    }
}

