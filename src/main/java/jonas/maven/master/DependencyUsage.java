package jonas.maven.master;

public class DependencyUsage {
    // Dependency usage, in their entry order
    private Long missedInstructions = 0L;
    private Long coveredInstructions = 0L;
    private Long missedBranches = 0L;
    private Long coveredBranches = 0L;
    private Long missedCyclomaticComplexity = 0L;
    private Long cyclomaticComplexity = 0L;

    private Long missedLines = 0L;
    private Long lines = 0L;

    private Long missedMethods = 0L;
    private Long methods = 0L;

    private Long missedClasses = 0L;
    private Long classes = 0L;


    public void addMissedInstructions(Long missedInstr){
        this.missedInstructions += missedInstr;
    }

    public Long getMissedInstructions(){
        return this.missedInstructions;
    }


    public void addCoveredInstructions(Long coveredInstr){
        this.coveredInstructions += coveredInstr;
    }

    public Long getCoveredInstructions(){
        return this.coveredInstructions;
    }

    public void addMissedBranches(Long missedBranch){
        this.missedBranches += missedBranch;
    }

    public Long getMissedBranches(){
        return this.missedBranches;
    }

    public void addCoveredBranches(Long coveredBranch){
        this.coveredBranches += coveredBranch;
    }

    public Long getCoveredBranches(){
        return this.coveredBranches;
    }

    public void addMissedCyclomaticComplexity(Long missedCxty){
        this.missedCyclomaticComplexity += missedCxty;
    }

    public Long getMissedCyclomaticComplexity(){
        return this.missedCyclomaticComplexity;
    }


    public void addCyclomaticComplexity(Long cxty){
        this.cyclomaticComplexity += cxty;
    }

    public Long getCyclomaticComplexity(){
        return this.cyclomaticComplexity;
    }

    public void addMissedLines(Long mlines){
        this.missedLines += mlines;
    }

    public Long getMissedLines(){
        return this.missedLines;
    }

    public void addCoveredLines(Long clines){
        this.lines += clines;
    }

    public Long getCoveredLines(){
        return this.lines;
    }


    public void addMissedMethods(Long mMethods){
        this.missedMethods += mMethods;
    }

    public Long getMissedMethods(){
        return this.missedMethods;
    }

    public void addCoveredMethods(Long cMethods){
        this.methods += cMethods;
    }

    public Long getCoveredMethods(){
        return this.methods;
    }


    public void addMissedClasses(Long mClasses){
        this.missedClasses += mClasses;
    }

    public Long getMissedClasses(){
        return this.missedClasses;
    }

    public void addCoveredClasses(Long cClasses){
        this.classes += cClasses;
    }

    public Long getCoveredClasses(){
        return this.classes;
    }

    /*
     TODO:
        - methods for instruction/branch percentage
        - Creating the html entry (Calculate bar widths)
     */

}
