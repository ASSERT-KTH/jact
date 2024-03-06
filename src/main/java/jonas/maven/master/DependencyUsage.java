package jonas.maven.master;

public class DependencyUsage {
    // Dependency usage, in their entry order
    private long missedInstructions = 0L;
    private long coveredInstructions = 0L;
    private long missedBranches = 0L;
    private long coveredBranches = 0L;
    private long missedCyclomaticComplexity = 0L;
    private long cyclomaticComplexity = 0L;

    private long missedLines = 0L;
    private long lines = 0L;

    private long missedMethods = 0L;
    private long methods = 0L;

    private long missedClasses = 0L;
    private long classes = 0L;


    public void addMissedInstructions(long missedInstr){
        this.missedInstructions += missedInstr;
    }

    public long getMissedInstructions(){
        return this.missedInstructions;
    }


    public void addCoveredInstructions(long coveredInstr){
        this.coveredInstructions += coveredInstr;
    }

    public long getCoveredInstructions(){
        return this.coveredInstructions;
    }

    public void addMissedBranches(long missedBranch){
        this.missedBranches += missedBranch;
    }

    public long getMissedBranches(){
        return this.missedBranches;
    }

    public void addCoveredBranches(long coveredBranch){
        this.coveredBranches += coveredBranch;
    }

    public long getCoveredBranches(){
        return this.coveredBranches;
    }

    public void addMissedCyclomaticComplexity(long missedCxty){
        this.missedCyclomaticComplexity += missedCxty;
    }

    public long getMissedCyclomaticComplexity(){
        return this.missedCyclomaticComplexity;
    }


    public void addCyclomaticComplexity(long cxty){
        this.cyclomaticComplexity += cxty;
    }

    public long getCyclomaticComplexity(){
        return this.cyclomaticComplexity;
    }

    public void addMissedLines(long mlines){
        this.missedLines += mlines;
    }

    public long getMissedLines(){
        return this.missedLines;
    }

    public void addCoveredLines(long clines){
        this.lines += clines;
    }

    public long getCoveredLines(){
        return this.lines;
    }


    public void addMissedMethods(long mMethods){
        this.missedMethods += mMethods;
    }

    public long getMissedMethods(){
        return this.missedMethods;
    }

    public void addCoveredMethods(long cMethods){
        this.methods += cMethods;
    }

    public long getCoveredMethods(){
        return this.methods;
    }


    public void addMissedClasses(long mClasses){
        this.missedClasses += mClasses;
    }

    public long getMissedClasses(){
        return this.missedClasses;
    }

    public void addCoveredClasses(long cClasses){
        this.classes += cClasses;
    }

    public long getCoveredClasses(){
        return this.classes;
    }

    /*
     TODO:
        - methods for instruction/branch percentage
        - Creating the html entry (Calculate bar widths)
     */

    private static String percentage(long part, long whole) {
        double percentage = (double) part / whole * 100;
        long roundedPercentage = (long) Math.floor(percentage);
        return String.format("%d%%", roundedPercentage);
    }

    public void addAll(DependencyUsage depUsage){
        this.addMissedInstructions(depUsage.getMissedInstructions());
        this.addCoveredInstructions(depUsage.getCoveredInstructions());
        this.addMissedBranches(depUsage.getMissedBranches());
        this.addCoveredBranches(depUsage.getCoveredBranches());
        this.addMissedCyclomaticComplexity(depUsage.getMissedCyclomaticComplexity());
        this.addCyclomaticComplexity(depUsage.getCyclomaticComplexity());
        this.addMissedLines(depUsage.getMissedLines());
        this.addCoveredLines(depUsage.getCoveredLines());
        this.addMissedMethods(depUsage.getMissedMethods());
        this.addCoveredMethods(depUsage.getCoveredMethods());
        this.addMissedClasses(depUsage.getMissedClasses());
        this.addCoveredClasses(depUsage.getCoveredClasses());
    }

    public String usageToHTML(String dependencyDirName, Boolean isPackage){

        // TODO fix the width of the bars
        long totalInstructions = this.getCoveredInstructions() + this.getMissedInstructions();
        long totalBranches = this.getCoveredBranches() + this.getMissedBranches();
        String icon;
        if(isPackage){
            icon = "el_package";
        }else{
            icon = "el_group";
        }
        String htmlString = "<tr>\n" +
                "    <td id=\"a47\"><a href=\""+ dependencyDirName +"/index.html\" class=\""+ icon +"\">"+ dependencyDirName +"</a></td>\n" +
                "    <td class=\"bar\" id=\"b5\"><img src=\"jacoco-resources/redbar.gif\" width=\"33\" height=\"10\" title=\""+ String.format("%,d", this.getMissedInstructions()) + "\" alt=\""+ String.format("%,d", this.getMissedInstructions()) + "\">" +
                "<img src=\"jacoco-resources/greenbar.gif\" width=\"1\" height=\"10\" title=\""+String.format("%,d", this.getCoveredInstructions())+"\" alt=\""+String.format("%,d", this.getCoveredInstructions())+"\"></td>\n" +
                "    <td class=\"ctr2\" id=\"c5\">"+percentage(this.getCoveredInstructions(), totalInstructions) +"</td>\n" +
                "    <td class=\"bar\" id=\"d4\"><img src=\"jacoco-resources/redbar.gif\" width=\"33\" height=\"10\" title=\""+ String.format("%,d", this.getMissedBranches()) + "\" alt=\""+ String.format("%,d", this.getMissedBranches()) + "\">" +
                "<img src=\"jacoco-resources/greenbar.gif\" width=\"1\" height=\"10\" title=\""+String.format("%,d", this.getCoveredBranches())+"\" alt=\""+String.format("%,d", this.getCoveredBranches())+"\"></td>\n" +
                "    <td class=\"ctr2\" id=\"e5\">"+percentage(this.getCoveredBranches(), totalBranches) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"f2\">"+ String.format("%,d", this.getMissedCyclomaticComplexity()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"g2\">"+ String.format("%,d", this.getCyclomaticComplexity()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"h2\">"+ String.format("%,d", this.getMissedLines()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"i2\">"+ String.format("%,d", this.getCoveredLines()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"j1\">"+ String.format("%,d", this.getMissedMethods()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"k1\">"+ String.format("%,d", this.getCoveredMethods()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"l13\">"+ String.format("%,d", this.getMissedClasses()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"m11\">"+ String.format("%,d", this.getCoveredClasses()) +"</td>\n" +
                "</tr>\n";
        return htmlString;
    }

    public String totalUsageToHTML(){

        // TODO fix the width of the bars
        long totalInstructions = this.getCoveredInstructions() + this.getMissedInstructions();
        long totalBranches = this.getCoveredBranches() + this.getMissedBranches();
        String htmlString = "<tr>\n" +
                "    <td>Total</td>\n" +
                "    <td class=\"bar\">"+String.format("%,d", this.getCoveredInstructions())+" of "+String.format("%,d", totalInstructions)+"</td>\n" +
                "    <td class=\"ctr2\" id=\"c5\">"+ percentage(this.getCoveredInstructions(), totalInstructions) +"</td>\n" +
                "    <td class=\"bar\">"+String.format("%,d", this.getCoveredBranches())+" of "+String.format("%,d", totalBranches)+"</td>\n" +
                "    <td class=\"ctr2\" id=\"e5\">"+ percentage(this.getCoveredBranches(), totalBranches) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"f2\">"+ String.format("%,d", this.getMissedCyclomaticComplexity()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"g2\">"+ String.format("%,d", this.getCyclomaticComplexity()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"h2\">"+ String.format("%,d", this.getMissedLines()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"i2\">"+ String.format("%,d", this.getCoveredLines()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"j1\">"+ String.format("%,d", this.getMissedMethods()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"k1\">"+ String.format("%,d", this.getCoveredMethods()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"l13\">"+ String.format("%,d", this.getMissedClasses()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"m11\">"+ String.format("%,d", this.getCoveredClasses()) +"</td>\n" +
                "</tr>\n";
        return htmlString;
    }

}
