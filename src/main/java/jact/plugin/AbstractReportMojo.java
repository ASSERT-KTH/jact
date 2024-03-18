package jact.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.*;


public abstract class AbstractReportMojo extends AbstractMojo {


    private static final String LINE_SEPARATOR = "------------------------------------------------------------------------";


    private static String hostOS;
    private static String localRepoPath;
    private static String projectGroupId;
    private static String artifactId;
    private static String version;
    private static Map<String, Set<String>> packageClassMap = new HashMap<>();
    @Parameter(property = "scope")
    String scope;
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



    public String getHostOS() {
        return this.session.getSystemProperties().getProperty("os.name").toLowerCase();
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

    public String getOutputJarName() {
        return this.project.getBuild().getFinalName();
    }

    public Map<String, Set<String>> getProjectPackagesAndClasses() {
        if(packageClassMap.isEmpty()){
            collectClassNamesAndPackages();
        }
        return packageClassMap;
    }

    public String getProjId(){
        return getProjectGroupId() + ":" + getProjectArtifactId() + ":" + getProjectVersion();
    }

    /**
     * Skip plugin execution completely.
     */
    @Parameter(property = "skipJACT", defaultValue = "false")
    private boolean skipJACT;

    @Override
    public final void execute()
            throws MojoExecutionException, MojoFailureException
    {
        if (skipReportGeneration()) {
            getLog().info("Skipping plugin execution...");
            return;
        }
        this.doExecute();
    }

    protected abstract void doExecute()
            throws MojoExecutionException, MojoFailureException;

    public boolean skipReportGeneration()
    {
        return this.skipJACT;
    }

    public MavenProject getProject()
    {
        return this.project;
    }

    public void printCustomStringToConsole(final String s)
    {
        this.getLog().info(LINE_SEPARATOR);
        this.getLog().info(s);
        this.getLog().info(LINE_SEPARATOR);
    }

    public static String getLineSeparator()
    {
        return LINE_SEPARATOR;
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
