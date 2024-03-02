package jonas.maven.master;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DependencyResolverExample {
    public static void main(String[] args) {
        // List of dependencies along with their coordinates
        String[][] dependencies = {
                {"jonas.sanity.check", "sanity-check", "1.0"},
                //{"groupId2", "artifactId2", "version2"},
                // Add more dependencies as needed
        };

        // Package name you want to match
        String packageName = "jonas.sanity.check";

        // Directory where your Maven dependencies are stored
        String mavenRepositoryDir = "/home/jonas/.m2/repository";

        // Iterate over each dependency
        for (String[] dependency : dependencies) {
            String groupId = dependency[0];
            String artifactId = dependency[1];
            String version = dependency[2];

            // Construct the path to the JAR file
            String jarFilePath = mavenRepositoryDir + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
            File jarFile = new File(jarFilePath);

            // Check if the JAR file exists
            if (jarFile.exists()) {
                System.out.println("HELLO 2");
                try (ZipFile zipFile = new ZipFile(jarFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if the entry is a class file within the desired package
                        if (entry.getName().startsWith(packageName.replace('.', '/')) && entry.getName().endsWith(".class")) {
                            System.out.println("Package " + packageName + " found in dependency: " + groupId + ":" + artifactId + ":" + version);
                            break;
                        }
                        System.out.println("DID NOT FIND PACKAGE");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

