package jonas.maven.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.project.MavenProject;

import static jonas.maven.master.DependencyCounterMojo.readInputStream;


public class TestRunner
{
    private static final Logger LOGGER = LogManager.getLogger(TestRunner.class.getName());

    public static void runTests(MavenProject mavenProject, boolean isTestRunForCoverage) throws IOException
    {
//        try {
//            // Command to be executed
//            String command = "mvn -Dmaven.repo.local=/mnt/c/kthcs/MEX/jonas-maven/src/main/it/sanity-check-project/target/classes test";
//
//            // Create a process builder with a shell
//            //ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command); // For Windows
//            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command); // For Linux
//
//            // Redirect error stream to output stream
//            processBuilder.redirectErrorStream(true);
//
//            // Start the process
//            Process process = processBuilder.start();
//
//            // Get the input stream of the process
//            InputStream inputStream = process.getInputStream();
//
//            // Read the output
//            String output = readInputStream(inputStream);
//
//            // Wait for the process to complete
//            int exitCode = process.waitFor();
//
//            // Print the output
//            System.out.println("Exit Code: " + exitCode);
//            System.out.println("Output:\n" + output);
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }





        Runtime rt = Runtime.getRuntime();
        Process p;

        if (isTestRunForCoverage) {
            p = rt.exec("mvn org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5:test");
        } else {
            p = rt.exec("mvn test -Dmaven.main.skip=true -Drat.skip=true -Danimal.sniffer.skip=true -Dmaven.javadoc.skip=true -Dlicense.skip=true -Dsource.skip=true");
        }

        printProcessToStandardOutput(p);

        try {
            p.waitFor();
        } catch (
                InterruptedException e) {
            LOGGER.error("Re-testing process terminated unexpectedly.");
            Thread.currentThread().interrupt();
        }

        TestResultReader testResultReader = new TestResultReader(".");
        TestResult testResult = testResultReader.getResults();

        MyFileWriter myFileWriter = new MyFileWriter(mavenProject.getBasedir().getAbsolutePath());
        myFileWriter.writeTestResultsToFile(testResult);

        if (testResult.errorTests() != 0 || testResult.failedTests() != 0) {
            LOGGER.error("JDBL: THERE ARE TESTS FAILURES");
        }
    }


    public static void runTests2(MavenProject mavenProject, String template) throws IOException
    {
        Runtime rt = Runtime.getRuntime();
        Process p;

        p = rt.exec("mvn surefire:test " +
                "-DargLine=\"-Djcov.template=" + template + "\"");

        printProcessToStandardOutput(p);

        try {
            p.waitFor();
        } catch (
                InterruptedException e) {
            LOGGER.error("Re-testing process terminated unexpectedly.");
            Thread.currentThread().interrupt();
        }

        TestResultReader testResultReader = new TestResultReader(".");
        TestResult testResult = testResultReader.getResults();

        MyFileWriter myFileWriter = new MyFileWriter(mavenProject.getBasedir().getAbsolutePath());
        myFileWriter.writeTestResultsToFile(testResult);

        if (testResult.errorTests() != 0 || testResult.failedTests() != 0) {
            LOGGER.error("JDBL: THERE ARE TESTS FAILURES");
        }

    }

    private static void printProcessToStandardOutput(final Process p) throws IOException
    {
        String line;
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
        input.close();
    }
}
