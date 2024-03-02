package jonas.maven.master;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PackageToDependencyResolver {
    public static ProjectDependency packageToDepPaths(String packageName) {
        // List of dependencies along with their coordinates

        List<ProjectDependency> dependencies = ProjectDependencies.getAllProjectDependencies();

        // Package name you want to match
        // packageName = "jonas.sanity.check";

        // Directory where your Maven dependencies are stored
        String mavenRepositoryDir = "/home/jonas/.m2/repository"; // TODO GET THE REPO DYNAMICALLY

        // Iterate over each dependency
        for (ProjectDependency dependency : dependencies) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();

            // Construct the path to the JAR file
            String jarFilePath = mavenRepositoryDir + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
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

