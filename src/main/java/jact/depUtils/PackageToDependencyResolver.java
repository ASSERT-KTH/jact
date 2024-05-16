package jact.depUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves a package name to a dependency in the local .m2 folder.
 */
public class PackageToDependencyResolver {

    private static ProjectDependency prevMatchedDep = new ProjectDependency();

    public static ProjectDependency packageToDependency(String packageName, Map<String,
                                                        ProjectDependency> dependenciesMap,
                                                        String localRepoPath) {

        ProjectDependency matchedDep = new ProjectDependency();
        boolean foundPackage = false;

        // Check the last matched dependency
        if(prevMatchedDep.getId() != null){
            foundPackage = containsPackage(packageName, getDependencyJars(prevMatchedDep, localRepoPath));
        }

        if(foundPackage){
            matchedDep = prevMatchedDep;
        }else{
            // Check all dependencies for the package
            for (ProjectDependency dependency : dependenciesMap.values()) {
                foundPackage = containsPackage(packageName, getDependencyJars(dependency, localRepoPath));
                if(foundPackage){
                    prevMatchedDep = dependency;
                    matchedDep = dependency;
                    break;
                }
            }
        }

        if (matchedDep.getId() == null) {
            // Usually a problem with a runtime dependency required by a test-dependency.
            // Which jacoco occasionally includes. Remove it.
            System.out.println("CANNOT MATCH PACKAGE TO ANY DEPENDENCY: " + packageName);
        }
        return matchedDep;
    }

    /**
     * Checks if the given jars contain the
     * packaged that is being searched for.
     * @param packageName
     * @param jarFiles
     * @return
     */
    private static boolean containsPackage(String packageName, File[] jarFiles){
        if (jarFiles != null && jarFiles.length > 0) {
            for(File jarFile : jarFiles){
                try (ZipFile zipFile = new ZipFile(jarFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if the entry is a class file within the desired package
                        if (entry.getName().startsWith(packageName.replace('.', '/')) && entry.getName().endsWith(".class")) {
                            //System.out.println("Package: " + packageName + " matched to dependency: " +
                            // groupId + ":" + artifactId + ":" + version);
                            return true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * Gets all the jar-names contained in
     * the dependency directory, required
     * to resolve non-standard jar-names.
     * @param dependency
     * @param localRepoPath
     * @return
     */
    private static File[] getDependencyJars(ProjectDependency dependency, String localRepoPath){
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();

        // Construct the path to the JAR file
        String directoryPath = localRepoPath + "/" + groupId.replace('.', '/') +
                "/" + artifactId + "/" + version + "/";
        File directory = new File(directoryPath);
        // Return all jar files from that dependency
        return directory.listFiles((dir, name) -> name.endsWith(".jar"));
    }

}
