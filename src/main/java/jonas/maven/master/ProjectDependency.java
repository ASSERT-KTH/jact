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
    private  ProjectDependency parent;


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

    public void setArtifactIdId(String artifactId){
        this.artifactId = artifactId;
    }

    public String getArtifactIdId(){
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

    public void setParent(ProjectDependency parent){
        this.parent = parent;
    }

    public ProjectDependency getParent(){
        return this.parent;
    }

    public void addChildDep(ProjectDependency child){
        children.add(child);
    }



    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                ", children=[" + childrenToString() + ']'+
                ", parent='" + parentToString() + '\'' +
                '}';
    }

    private String childrenToString() {
        StringBuilder sb = new StringBuilder();
        if (!children.isEmpty()) {

            for (int i = 0; i < children.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(children.get(i).id);
            }
            return sb.toString();
        }
        return "";
    }

    private String parentToString(){
        if(this.getParent() == null){
            return "";
        }
        return this.getParent().id;
    }
}
