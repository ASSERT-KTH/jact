package jonas.maven.master;

import java.util.ArrayList;
import java.util.List;

public class ProjectDependency {
    private String id;
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private List<ProjectDependency> children = new ArrayList<>();
    private List<ProjectDependency> parents = new ArrayList<>();
    private List<String> raportPaths = new ArrayList<>();
    public DependencyUsage dependencyUsage = new DependencyUsage();


    public void setId(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    public void setGroupId(String groupId){
        this.groupId = groupId;
    }

    public String getGroupId(){
        return this.groupId;
    }

    public void setArtifactId(String artifactId){
        this.artifactId = artifactId;
    }

    public String getArtifactId(){
        return this.artifactId;
    }

    public void setVersion(String version){
        this.version = version;
    }

    public String getVersion(){
        return this.version;
    }

    public void setScope(String scope){
        this.scope = scope;
    }

    public String getScope(){
        return this.scope;
    }

    public void addChildDep(ProjectDependency child){
        children.add(child);
    }

    public List<ProjectDependency> getChildDeps(){
        return this.children;
    }

    public void addParentDep(ProjectDependency parent){
        this.parents.add(parent);
    }

    public List<ProjectDependency> getParentDeps(){
        return this.parents;
    }

    public void addReportPath(String reportPath){
        this.raportPaths.add(reportPath);
    }

    public List<String> getReportPaths(){
        return this.raportPaths;
    }


    @Override
    public String toString() {
        return "{" +
                "id='" + this.getId() + '\'' +
                "groupId='" + this.getGroupId() + '\'' +
                ", artifactId='" + this.getArtifactId() + '\'' +
                ", version='" + this.getVersion() + '\'' +
                ", scope='" + this.getScope() + '\'' +
                ", children=[" + childrenToString() + ']'+
                ", parents=["  + parentsToString() + ']' +
                '}';
    }

    private String childrenToString() {
        StringBuilder sb = new StringBuilder();
        if (!this.getChildDeps().isEmpty()) {

            for (int i = 0; i < this.getChildDeps().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(this.children.get(i).id);
            }
            return sb.toString();
        }
        return "";
    }

    private String parentsToString(){
        if(this.getParentDeps().isEmpty()){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i< this.getParentDeps().size(); i++){
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.getParentDeps().get(i).getId());
        }
        return sb.toString();
    }

    public static String formatNumber(long number) {
        if (number < 0) {
            return "-" + formatNumber(-number);
        }
        if (number < 1000) {
            return Long.toString(number);
        }

        String[] suffixes = new String[]{"", "thousand", "million", "billion", "trillion", "quadrillion", "quintillion"};
        int index = 0;

        while (number >= 1000) {
            number /= 1000;
            index++;
        }

        return String.format("%,d %s", number, suffixes[index]);
    }

    private static String percentage(long part, long whole) {
        double percentage = (double) part / whole * 100;
        long roundedPercentage = (long) Math.floor(percentage);
        return String.format("%d%%", roundedPercentage);
    }

    public String usageToHTML(){
        String dependencyDirName = JacocoHTMLAugmenter.depToDirName(this);

        // TODO fix the width of the bars
        long totalInstructions = this.dependencyUsage.getCoveredInstructions() + this.dependencyUsage.getMissedInstructions();
        long totalBranches = this.dependencyUsage.getCoveredBranches() + this.dependencyUsage.getMissedBranches();
        String htmlString = "<tr>\n" +
                "    <td id=\"a47\"><a href=\""+ dependencyDirName +"/index.html\" class=\"el_group\">"+ dependencyDirName +"</a></td>\n" +
                "    <td class=\"bar\" id=\"b5\"><img src=\"jacoco-resources/redbar.gif\" width=\"33\" height=\"10\" title=\""+ String.format("%,d", this.dependencyUsage.getMissedInstructions()) + "\" alt=\""+ String.format("%,d", this.dependencyUsage.getMissedInstructions()) + "\">" +
                "<img src=\"jacoco-resources/greenbar.gif\" width=\"1\" height=\"10\" title=\""+String.format("%,d", this.dependencyUsage.getCoveredInstructions())+"\" alt=\""+String.format("%,d", this.dependencyUsage.getCoveredInstructions())+"\"></td>\n" +
                "    <td class=\"ctr2\" id=\"c5\">"+percentage(this.dependencyUsage.getCoveredInstructions(), totalInstructions) +"</td>\n" +
                "    <td class=\"bar\" id=\"d4\"><img src=\"jacoco-resources/redbar.gif\" width=\"33\" height=\"10\" title=\""+ String.format("%,d", this.dependencyUsage.getMissedBranches()) + "\" alt=\""+ String.format("%,d", this.dependencyUsage.getMissedBranches()) + "\">" +
                "<img src=\"jacoco-resources/greenbar.gif\" width=\"1\" height=\"10\" title=\""+String.format("%,d", this.dependencyUsage.getCoveredBranches())+"\" alt=\""+String.format("%,d", this.dependencyUsage.getCoveredBranches())+"\"></td>\n" +
                "    <td class=\"ctr2\" id=\"e5\">"+percentage(this.dependencyUsage.getCoveredBranches(), totalBranches) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"f2\">"+ String.format("%,d", this.dependencyUsage.getMissedCyclomaticComplexity()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"g2\">"+ String.format("%,d", this.dependencyUsage.getCyclomaticComplexity()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"h2\">"+ String.format("%,d", this.dependencyUsage.getMissedLines()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"i2\">"+ String.format("%,d", this.dependencyUsage.getCoveredLines()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"j1\">"+ String.format("%,d", this.dependencyUsage.getMissedMethods()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"k1\">"+ String.format("%,d", this.dependencyUsage.getCoveredMethods()) +"</td>\n" +
                "    <td class=\"ctr1\" id=\"l13\">"+ String.format("%,d", this.dependencyUsage.getMissedClasses()) +"</td>\n" +
                "    <td class=\"ctr2\" id=\"m11\">"+ String.format("%,d", this.dependencyUsage.getCoveredClasses()) +"</td>\n" +
                "</tr>";
        return htmlString;
    }


}
