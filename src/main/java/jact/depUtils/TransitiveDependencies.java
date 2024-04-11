package jact.depUtils;

import java.util.ArrayList;
import java.util.List;

import static jact.depUtils.ProjectDependency.depToDirName;

public class TransitiveDependencies {

    public DependencyUsage transitiveUsage = new DependencyUsage();
    private List<String> raportPaths = new ArrayList<>();
    private String parentId;

    private String parentDirName;

    public TransitiveDependencies(ProjectDependency parent){
        this.parentId = parent.getId();
        this.parentDirName = depToDirName(parent);
        addReportPaths(parent);
    }

    public void addReportPaths(ProjectDependency parent){
        for(String path : parent.getReportPaths()){
            if(!this.raportPaths.contains(path + "transitive-dependencies/")){
                this.raportPaths.add(path + "transitive-dependencies/");
            }
        }
    }

    public String getParentId(){
        return this.parentId;
    }

    public String getParentDirName(){
        return this.parentDirName;
    }

    public List<String> getReportPaths(){
        return this.raportPaths;
    }
}
