package jact.depUtils;

import java.util.ArrayList;
import java.util.List;

import static jact.depUtils.ProjectDependency.depToDirName;

public class TransitiveDependencies {

    public DependencyUsage transitiveUsage = new DependencyUsage();
    private String depId;

    private String parentDirName;

    public TransitiveDependencies(ProjectDependency pd){
        this.depId = pd.getId();
        this.parentDirName = depToDirName(pd);
    }

//    public void addReportPaths(ProjectDependency parent){
//        for(String path : parent.getReportPaths()){
//            if(!this.raportPaths.contains(path + "transitive-dependencies/")){
//                this.raportPaths.add(path + "transitive-dependencies/");
//            }
//        }
//    }

    public String getParentId(){
        return this.depId;
    }

    public String getParentDirName(){
        return this.parentDirName;
    }

//    public List<String> getReportPaths(){
//        return this.raportPaths;
//    }
}
