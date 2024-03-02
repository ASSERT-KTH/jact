package jonas.maven.master;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.gson.*;

public class ProjectDependencies {

    public static List<ProjectDependency> projectDependencies = new ArrayList<>();

    public static List<ProjectDependency> getAllProjectDependencies(){
        if(projectDependencies.isEmpty()){
            generateAllProjectDependencies();
        }
        return projectDependencies;
    }

    private static void generateAllProjectDependencies() {
        CommandExecutor.generateDependencyLockfile();
        String filePath = "./target/jact-report/lockfile.json"; // Path to the JSON file
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new GsonBuilder().registerTypeAdapter(ProjectDependency.class, new ProjectDependencyDeserializer()).create();
            JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);
            JsonArray dependenciesArray = jsonElement.getAsJsonObject().getAsJsonArray("dependencies");
            for (JsonElement dependencyElement : dependenciesArray) {
                gson.fromJson(dependencyElement, ProjectDependency.class);
            }
//            for (ProjectDependency dependency : projectDependencies) {
//                System.out.println(dependency);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class ProjectDependencyDeserializer implements com.google.gson.JsonDeserializer<ProjectDependency> {
        @Override
        public ProjectDependency deserialize(JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();
            Set<String> visited = new HashSet<>();
            List<ProjectDependency> parentDeps = new ArrayList<>();
            return parseDependency(jsonObject, parentDeps);
        }

        private ProjectDependency parseDependency(JsonObject jsonObject, List<ProjectDependency> parentDeps) {
//            String dependencyId = jsonObject.get("id").getAsString();
//            if (visited.contains(dependencyId)) {
//                // If the dependency has been visited before, return null to avoid infinite recursion
//                return null;
//            }
//            visited.add(dependencyId);

            ProjectDependency projectDependency = new ProjectDependency();
            projectDependency.setId(jsonObject.has("id") ? jsonObject.get("id").getAsString() : "");
            projectDependency.setGroupId(jsonObject.has("groupId") ? jsonObject.get("groupId").getAsString() : "");
            projectDependency.setArtifactId(jsonObject.has("artifactId") ? jsonObject.get("artifactId").getAsString() : "");
            projectDependency.setVersion(jsonObject.has("selectedVersion") ? jsonObject.get("selectedVersion").getAsString() : "");
            projectDependency.setScope(jsonObject.has("scope") ? jsonObject.get("scope").getAsString() : "");

            String parentString = jsonObject.has("parent") ? jsonObject.get("parent").getAsString() : "";

            // First add the previous parents in order
            for(ProjectDependency parentDep : parentDeps){
                projectDependency.addParentDep(parentDep);
            }
            // Then add the immediate parent
            if (!parentString.isEmpty()) {
                for (ProjectDependency pd : projectDependencies) {
                    if (pd.getId().equals(parentString)) {
                        projectDependency.addParentDep(pd);
                        break;
                    }
                }
            }
            //System.out.println("ADDING: " + projectDependency.toString());
            projectDependencies.add(projectDependency);
            JsonArray childrenJsonArray = jsonObject.getAsJsonArray("children");
            if (childrenJsonArray != null) {
                for (JsonElement element : childrenJsonArray) {
                    ProjectDependency child = parseDependency(element.getAsJsonObject(), projectDependency.getParentDeps());
                    projectDependency.addChildDep(child);
                }
            }

            return projectDependency;
        }
    }
}
