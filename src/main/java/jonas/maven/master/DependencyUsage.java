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

}
