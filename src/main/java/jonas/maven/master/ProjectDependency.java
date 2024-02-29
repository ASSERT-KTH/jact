package jonas.maven.master;

import java.util.ArrayList;
import java.util.List;

public class ProjectDependency {
    String groupId;
    String artifactId;
    String version;
    String scope;
    Boolean optional;
    List<ProjectDependency> children = new ArrayList<>();
    ProjectDependency parent;

    public void setGroupId(String groupId){
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId){
        this.artifactId = artifactId;
    }

    public void setVersion(String version){
        this.version = version;
    }

    public void setScope(String scope){
        this.scope = scope;
    }

    public void setOptional(Boolean optional){
        this.optional = optional;
    }

    public void setParent(ProjectDependency parent){
        this.parent = parent;
    }

    public String getGroupId(){
        return groupId;
    }

    public String getArtifactId(){
        return artifactId;
    }

    public String getVersion(){
        return version;
    }

    public String getScope(){
        return scope;
    }

    public boolean getOptional(){
        return optional;
    }

    public ProjectDependency getParent(){
        return parent;
    }

    public List<ProjectDependency> getChildren(){
        return children;
    }


}
