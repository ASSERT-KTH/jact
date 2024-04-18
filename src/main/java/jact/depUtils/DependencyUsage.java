package jact.depUtils;

import java.text.DecimalFormat;

/**
 * Tracks the usage of a dependency in all the recorded metrics.
 */
public class DependencyUsage {
    // Dependency usage, in their entry order
    private long missedInstructions = 0L;
    private long totalInstructions = 0L;
    private long missedBranches = 0L;
    private long totalBranches = 0L;
    private long missedCyclomaticComplexity = 0L;
    private long cyclomaticComplexity = 0L;

    private long missedLines = 0L;
    private long lines = 0L;

    private long missedMethods = 0L;
    private long methods = 0L;

    private long missedClasses = 0L;
    private long classes = 0L;

    public static String percentage(long part, long whole) {
        double percentage = (double) part / whole * 100;
        long roundedPercentage = (long) Math.floor(percentage);
        return String.format("%d%%", roundedPercentage);
    }

    private static String lessRoundedPercentage(long part, long whole) {
        double percentage = (double) part / whole * 100;
        DecimalFormat decimalFormat = new DecimalFormat("#.####"); // Format to four decimal places
        return decimalFormat.format(percentage) + "%";
    }

    public static int barLength(long part, long whole) {
        double percentage = (double) part / whole * 100;
        return (int) Math.floor(percentage);
    }

    public void addMissedInstructions(long missedInstr) {
        this.missedInstructions += missedInstr;
    }

    public long getMissedInstructions() {
        return this.missedInstructions;
    }

    public void addTotalInstructions(long instr) {
        this.totalInstructions += instr;
    }

    public long getTotalInstructions() {
        return this.totalInstructions;
    }

    public void addMissedBranches(long missedBranch) {
        this.missedBranches += missedBranch;
    }

    public long getMissedBranches() {
        return this.missedBranches;
    }

    public void addTotalBranches(long branches) {
        this.totalBranches += branches;
    }

    public long getTotalBranches() {
        return this.totalBranches;
    }

    public void addMissedCyclomaticComplexity(long missedCxty) {
        this.missedCyclomaticComplexity += missedCxty;
    }

    public long getMissedCyclomaticComplexity() {
        return this.missedCyclomaticComplexity;
    }

    public void addCyclomaticComplexity(long cxty) {
        this.cyclomaticComplexity += cxty;
    }

    public long getCyclomaticComplexity() {
        return this.cyclomaticComplexity;
    }

    public void addMissedLines(long mlines) {
        this.missedLines += mlines;
    }

    public long getMissedLines() {
        return this.missedLines;
    }

    public void addTotalLines(long clines) {
        this.lines += clines;
    }

    public long getTotalLines() {
        return this.lines;
    }

    public void addMissedMethods(long mMethods) {
        this.missedMethods += mMethods;
    }

    public long getMissedMethods() {
        return this.missedMethods;
    }

    public void addTotalMethods(long cMethods) {
        this.methods += cMethods;
    }

    public long getTotalMethods() {
        return this.methods;
    }

    public void addMissedClasses(long mClasses) {
        this.missedClasses += mClasses;
    }

    public long getMissedClasses() {
        return this.missedClasses;
    }

    public void addTotalClasses(long cClasses) {
        this.classes += cClasses;
    }

    public long getTotalClasses() {
        return this.classes;
    }

    public void addAll(DependencyUsage depUsage) {
        this.addMissedInstructions(depUsage.getMissedInstructions());
        this.addTotalInstructions(depUsage.getTotalInstructions());
        this.addMissedBranches(depUsage.getMissedBranches());
        this.addTotalBranches(depUsage.getTotalBranches());
        this.addMissedCyclomaticComplexity(depUsage.getMissedCyclomaticComplexity());
        this.addCyclomaticComplexity(depUsage.getCyclomaticComplexity());
        this.addMissedLines(depUsage.getMissedLines());
        this.addTotalLines(depUsage.getTotalLines());
        this.addMissedMethods(depUsage.getMissedMethods());
        this.addTotalMethods(depUsage.getTotalMethods());
        this.addMissedClasses(depUsage.getMissedClasses());
        this.addTotalClasses(depUsage.getTotalClasses());
    }

    public String usageToHTML(String dependencyDirName, DependencyUsage total, boolean isPackage, boolean transitiveEntry) {

        long coveredInstructions = this.getTotalInstructions() - this.getMissedInstructions();
        long coveredBranches = this.getTotalBranches() - this.getMissedBranches();

        int redInstrBar = barLength(this.getMissedInstructions(), total.getTotalInstructions());
        int greenInstrBar = barLength(coveredInstructions, total.getTotalInstructions());

        int redBranchBar = barLength(this.getMissedBranches(), total.getTotalBranches());
        int greenBranchBar = barLength(coveredBranches, total.getTotalBranches());
        String icon;
        if (isPackage) {
            icon = "el_package";
        } else {
            icon = "el_group";
        }
        String link = dependencyDirName + "/index.html";
        if (dependencyDirName.equals("transitive-dependencies") && transitiveEntry) {
            link = "transitive-dependencies.html";
        } else if (transitiveEntry) {
            link = "../" + dependencyDirName + "/index.html";
        }

        String htmlString = "<tr>\n" +
                "    <td id=\"a47\"><a href=\"" + link + "\" class=\"" + icon + "\">" + dependencyDirName + "</a></td>\n" +
                "    <td class=\"bar\" id=\"b5\"><img src=\"jacoco-resources/redbar.gif\" width=\"" + redInstrBar + "\" height=\"10\" title=\"" + String.format("%,d", this.getMissedInstructions()) + "\" alt=\"" + String.format("%,d", this.getMissedInstructions()) + "\">" +
                "<img src=\"jacoco-resources/greenbar.gif\" width=\"" + greenInstrBar + "\" height=\"10\" title=\"" + String.format("%,d", coveredInstructions) + "\" alt=\"" + String.format("%,d", coveredInstructions) + "\"></td>\n" +
                "    <td class=\"ctr2\" id=\"c5\">" + percentage(coveredInstructions, this.getTotalInstructions()) + "</td>\n" +
                "    <td class=\"bar\" id=\"d4\"><img src=\"jacoco-resources/redbar.gif\" width=\"" + redBranchBar + "\" height=\"10\" title=\"" + String.format("%,d", this.getMissedBranches()) + "\" alt=\"" + String.format("%,d", this.getMissedBranches()) + "\">" +
                "<img src=\"jacoco-resources/greenbar.gif\" width=\"" + greenBranchBar + "\" height=\"10\" title=\"" + String.format("%,d", coveredBranches) + "\" alt=\"" + String.format("%,d", coveredBranches) + "\"></td>\n" +
                "    <td class=\"ctr2\" id=\"e5\">" + percentage(coveredBranches, this.getTotalBranches()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"f2\">" + String.format("%,d", this.getMissedCyclomaticComplexity()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"g2\">" + String.format("%,d", this.getCyclomaticComplexity()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"h2\">" + String.format("%,d", this.getMissedLines()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"i2\">" + String.format("%,d", this.getTotalLines()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"j1\">" + String.format("%,d", this.getMissedMethods()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"k1\">" + String.format("%,d", this.getTotalMethods()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"l13\">" + String.format("%,d", this.getMissedClasses()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"m11\">" + String.format("%,d", this.getTotalClasses()) + "</td>\n" +
                "</tr>\n";
        return htmlString;
    }

    public String totalUsageToHTML() {
        long coveredInstructions = this.getTotalInstructions() - this.getMissedInstructions();
        long coveredBranches = this.getTotalBranches() - this.getMissedBranches();
        String htmlString = "<tr>\n" +
                "    <td>Total</td>\n" +
                "    <td class=\"bar\">" + String.format("%,d", this.getMissedInstructions()) + " of " + String.format("%,d", this.getTotalInstructions()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"c5\">" + percentage(coveredInstructions, this.getTotalInstructions()) + "</td>\n" +
                "    <td class=\"bar\">" + String.format("%,d", this.getMissedBranches()) + " of " + String.format("%,d", this.getTotalBranches()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"e5\">" + percentage(coveredBranches, this.getTotalBranches()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"f2\">" + String.format("%,d", this.getMissedCyclomaticComplexity()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"g2\">" + String.format("%,d", this.getCyclomaticComplexity()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"h2\">" + String.format("%,d", this.getMissedLines()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"i2\">" + String.format("%,d", this.getTotalLines()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"j1\">" + String.format("%,d", this.getMissedMethods()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"k1\">" + String.format("%,d", this.getTotalMethods()) + "</td>\n" +
                "    <td class=\"ctr1\" id=\"l13\">" + String.format("%,d", this.getMissedClasses()) + "</td>\n" +
                "    <td class=\"ctr2\" id=\"m11\">" + String.format("%,d", this.getTotalClasses()) + "</td>\n" +
                "</tr>\n";
        return htmlString;
    }

    public String totalUsageToXML() {
        long coveredInstructions = this.getTotalInstructions() - this.getMissedInstructions();
        long coveredBranches = this.getTotalBranches() - this.getMissedBranches();
        long coveredLines = this.getTotalLines() - this.getMissedLines();
        long coveredCyclomaticComplexity = this.getCyclomaticComplexity() - this.getMissedCyclomaticComplexity();
        long coveredMethods = this.getTotalMethods() - this.getMissedMethods();
        long coveredClasses = this.getTotalClasses() - this.getMissedClasses();

        String xmlString =
                "<counter covered=\"" + coveredInstructions + "\" missed=\"" + this.getMissedInstructions() + "\" type=\"INSTRUCTION\"/>" +
                        "<counter covered=\"" + coveredBranches + "\" missed=\"" + this.getMissedBranches() + "\" type=\"BRANCH\"/>" +
                        "<counter covered=\"" + coveredLines + "\" missed=\"" + this.getMissedLines() + "\" type=\"LINE\"/>" +
                        "<counter covered=\"" + coveredCyclomaticComplexity + "\" missed=\"" + this.getMissedCyclomaticComplexity() + "\" type=\"COMPLEXITY\"/>" +
                        "<counter covered=\"" + coveredMethods + "\" missed=\"" + this.getMissedMethods() + "\" type=\"METHOD\"/>" +
                        "<counter covered=\"" + coveredClasses + "\" missed=\"" + this.getMissedClasses() + "\" type=\"CLASS\"/>";

        return xmlString;
    }

    public String usageToMarkdown(String name) {
        long coveredInstructions = this.getTotalInstructions() - this.getMissedInstructions();
        String coveredInstructionsPercentage = lessRoundedPercentage(coveredInstructions, this.getTotalInstructions());
        String missedInstructionsPercentage = lessRoundedPercentage(this.getMissedInstructions(), this.getTotalInstructions());

        long coveredBranches = this.getTotalBranches() - this.getMissedBranches();
        String coveredBranchesPercentage = lessRoundedPercentage(coveredBranches, this.getTotalBranches());
        String missedBranchesPercentage = lessRoundedPercentage(this.getMissedBranches(), this.getTotalBranches());

        long coveredLines = this.getTotalLines() - this.getMissedLines();
        String coveredLinesPercentage = lessRoundedPercentage(coveredLines, this.getTotalLines());
        String missedLinesPercentage = lessRoundedPercentage(this.getMissedLines(), this.getTotalLines());

        long coveredCyclomaticComplexity = this.getCyclomaticComplexity() - this.getMissedCyclomaticComplexity();
        String coveredComplexityPercentage = lessRoundedPercentage(coveredCyclomaticComplexity, this.getCyclomaticComplexity());
        String missedComplexityPercentage = lessRoundedPercentage(this.getMissedCyclomaticComplexity(), this.getCyclomaticComplexity());

        long coveredMethods = this.getTotalMethods() - this.getMissedMethods();
        String coveredMethodsPercentage = lessRoundedPercentage(coveredMethods, this.getTotalMethods());
        String missedMethodsPercentage = lessRoundedPercentage(this.getMissedMethods(), this.getTotalMethods());

        long coveredClasses = this.getTotalClasses() - this.getMissedClasses();
        String coveredClassesPercentage = lessRoundedPercentage(coveredClasses, this.getTotalClasses());
        String missedClassesPercentage = lessRoundedPercentage(this.getMissedClasses(), this.getTotalClasses());

        String usageString =
                "### " + name + " (covered, missed, total):" + "  \n" +
                        "**INSTRUCTION** &nbsp;" + coveredInstructions + " : " + coveredInstructionsPercentage + " | " +
                        this.getMissedInstructions() + " : " + missedInstructionsPercentage + " | " + this.getTotalInstructions() + "  \n" +

                        "**BRANCH** &nbsp;" + coveredBranches + " : " + coveredBranchesPercentage + " | " +
                        this.getMissedBranches() + " : " + missedBranchesPercentage + " | " + this.getTotalBranches() + "  \n" +

                        "**LINE** &nbsp;" + coveredLines + " : " + coveredLinesPercentage + " | " +
                        this.getMissedLines() + " : " + missedLinesPercentage + " | " + this.getTotalLines() + "  \n" +

                        "**COMPLEXITY** &nbsp;" + coveredCyclomaticComplexity + " : " + coveredComplexityPercentage + " | " +
                        this.getMissedCyclomaticComplexity() + " : " + missedComplexityPercentage + " | " + this.getCyclomaticComplexity() + "  \n" +

                        "**METHOD** &nbsp;" + coveredMethods + " : " + coveredMethodsPercentage + " | " +
                        this.getMissedMethods() + " : " + missedMethodsPercentage + " | " + this.getTotalMethods() + "  \n" +

                        "**CLASS** &nbsp;" + coveredClasses + " : " + coveredClassesPercentage + " | " +
                        this.getMissedClasses() + " : " + missedClassesPercentage + " | " + this.getTotalClasses() + "  \n";

        return usageString;
    }

}
