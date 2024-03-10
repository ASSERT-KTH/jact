package jact;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
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
    private static ProjectDependency lastMatchedDep = new ProjectDependency();



    public static ProjectDependency packageToDepPaths(String packageName, List<ProjectDependency> dependencies) {
        // List of dependencies along with their coordinates

        //List<ProjectDependency> dependencies = ProjectDependencies.getAllProjectDependencies();

        // Package name you want to match
        // packageName = "jonas.sanity.check";

        // Directory where your Maven dependencies are stored
        String mavenRepositoryDir = CompleteCoverageMojo.getLocalRepoPath();

        if(lastMatchedDep.getId() != null){
            String groupId = lastMatchedDep.getGroupId();
            String artifactId = lastMatchedDep.getArtifactId();
            String version = lastMatchedDep.getVersion();
            // Search within the last matched dependency for the package
            String jarFilePath = mavenRepositoryDir + "/" + groupId.replace('.', '/') +
                    "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
            File jarFile = new File(jarFilePath);
            if (jarFile.exists()) {
                try (ZipFile zipFile = new ZipFile(jarFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if the entry is a class file within the desired package
                        if (entry.getName().startsWith(packageName.replace('.', '/')) && entry.getName().endsWith(".class")) {
                            System.out.println("Package: " + packageName + " matched to dependency: " + groupId + ":" + artifactId + ":" + version);
                            lastMatchedDep.packageUsageMap.put(packageName, new DependencyUsage());
                            return lastMatchedDep;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

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
                            lastMatchedDep = dependency;
                            return dependency;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Couldn't find a matching package, return an empty one.
        return new ProjectDependency();
    }
}

