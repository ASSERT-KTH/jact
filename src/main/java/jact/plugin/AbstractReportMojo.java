package jact.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class AbstractReportMojo extends AbstractMojo {
    private static final String jactReportPath = "./target/jact-report/";
    private static String localRepoPath;
    private static String projectGroupId;
    private static String artifactId;
    private static String version;
    private static Map<String, Set<String>> packageClassMap = new HashMap<>();
    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter(property = "shadedJarName")
    private String shadedJarName;

    @Parameter(property = "skipTestDependencies", defaultValue = "true")
    private String skipTestDependencies;

    /**
     * Skip plugin execution completely.
     */
    @Parameter(property = "skipJACT", defaultValue = "false")
    private String skipJACT;

    @Parameter(property = "includeSummary", defaultValue = "false")
    private String includeSummary;

    @Override
    public final void execute()
            throws MojoExecutionException, MojoFailureException {
        if (skipReportGeneration()) {
            getLog().info("Skipping plugin execution...");
            return;
        }
        this.doExecute();
    }

    protected abstract void doExecute()
            throws MojoExecutionException, MojoFailureException;


    public boolean getDepFilterParam() {
        return Boolean.parseBoolean(this.skipTestDependencies);
    }

    public static String getJactReportPath() {
        return jactReportPath;
    }

    public String getLocalRepoPath() {
        return this.session.getLocalRepository().getBasedir();
    }

    public String getProjectGroupId() {
        return this.project.getGroupId();
    }

    public String getProjectArtifactId() {
        return this.project.getArtifactId();
    }

    public String getProjectVersion() {
        return this.project.getVersion();
    }

    public boolean skipReportGeneration() {
        return Boolean.parseBoolean(this.skipJACT);
    }

    public MavenProject getProject() {
        return this.project;
    }

    public String getProjId() {
        return getProjectGroupId() + ":" + getProjectArtifactId() + ":" + getProjectVersion();
    }

    public boolean getSummaryProperty() {
        return Boolean.parseBoolean(this.includeSummary);
    }

    public String getOutputJarName() {
        if (shadedJarName == null) {
            shadedJarName = this.project.getBuild().getFinalName() + "-shaded";
        }
        return shadedJarName;
    }

    public Map<String, Set<String>> getProjectPackagesAndClasses() {
        if (packageClassMap.isEmpty()) {
            collectClassNamesAndPackages();
        }
        return packageClassMap;
    }

    private void collectClassNamesAndPackages() {
        String classesDirectory = this.getProject().getBuild().getOutputDirectory();
        scanForClassesAndPackages(new File(classesDirectory), "");
    }

    private static void scanForClassesAndPackages(File directory, String parentPackage) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                String currentPackage = parentPackage.isEmpty() ? file.getName() : parentPackage + "." + file.getName();
                scanForClassesAndPackages(file, currentPackage);
            } else if (file.getName().endsWith(".class")) {
                // Extract package name from class file
                String packageName = parentPackage.replace(File.separator, ".");
                String className = file.getName().replace(".class", "");

                // Store class name in package map
                packageClassMap.computeIfAbsent(packageName, k -> new HashSet<>()).add(className);
            }
        }
    }

}
