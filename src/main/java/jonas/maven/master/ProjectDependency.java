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

    public boolean writtenToFile = false;


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

}
