package jact;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static jact.JacocoHTMLAugmenter.createDependencyReports;
import static jact.JacocoHTMLAugmenter.extractReportAndMoveDirs;


public abstract class AbstractReportMojo extends AbstractMojo {


    private static final String LINE_SEPARATOR = "------------------------------------------------------------------------";


    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Skip plugin execution completely.
     */
    @Parameter(property = "skipJACT", defaultValue = "false")
    private boolean skipJACT;

    @Override
    public final void execute()
            throws MojoExecutionException, MojoFailureException
    {
        if (isSkipJDBL()) {
            getLog().info("Skipping plugin execution...");
            return;
        }
        this.doExecute();
    }

    protected abstract void doExecute()
            throws MojoExecutionException, MojoFailureException;

    public boolean isSkipJDBL()
    {
        return this.skipJACT;
    }

    public MavenProject getProject()
    {
        return project;
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

}
