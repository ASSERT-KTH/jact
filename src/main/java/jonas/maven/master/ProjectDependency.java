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
}
