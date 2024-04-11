package jact.depUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jact.core.HtmlAugmenter.writeHTMLStringToFile;

/**
 * Tracks the usage of a dependency.
 */
public class ProjectDependency {
    public DependencyUsage dependencyUsage = new DependencyUsage();
    public Map<String, DependencyUsage> packageUsageMap = new HashMap<>();
    public boolean writtenEntryToFile = false;
    public boolean writtenTransitive = false;
    public boolean writtenTotalToFile = false;
    private String id;
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private List<ProjectDependency> children = new ArrayList<>();
    private List<ProjectDependency> parents = new ArrayList<>();
    private List<String> raportPaths = new ArrayList<>();

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return this.scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void addChildDep(ProjectDependency child) {
        children.add(child);
    }

    public List<ProjectDependency> getChildDeps() {
        return this.children;
    }

    public void addParentDep(ProjectDependency parent) {
        this.parents.add(parent);
    }

    public List<ProjectDependency> getParentDeps() {
        return this.parents;
    }

    public void addReportPath(String reportPath) {
        if(!presentReportPath(reportPath)){
            this.raportPaths.add(reportPath);
        }
    }

    public List<String> getReportPaths() {
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
                ", children=[" + childrenToString() + ']' +
                ", parents=[" + parentsToString() + ']' +
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

    private String parentsToString() {
        if (this.getParentDeps().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.getParentDeps().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.getParentDeps().get(i).getId());
        }
        return sb.toString();
    }

    public void writePackagesToFile(String path, DependencyUsage total) {
        // Iterate through the map entries
        for (Map.Entry<String, DependencyUsage> entry : this.packageUsageMap.entrySet()) {
            try {
                writeHTMLStringToFile(path + "/index.html", entry.getValue().usageToHTML(entry.getKey(), total, true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets the corresponding directory name for a dependency.
     * @param dependency
     * @return String
     */
    public static String depToDirName(ProjectDependency dependency) {
        return dependency.getGroupId().replace("-", ".") + "." +
                dependency.getArtifactId().replace("-", ".") + "-v" + dependency.getVersion();
    }

    public boolean presentReportPath(String reportPath){
        boolean present = false;
        for(String path : this.getReportPaths()){
            if(path.equals(reportPath)){
                present = true;
                break;
            }
        }
        return present;
    }

}
