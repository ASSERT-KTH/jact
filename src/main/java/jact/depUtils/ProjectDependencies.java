package jact.depUtils;

import com.google.gson.*;
import jact.depUtils.ProjectDependency;
import jact.utils.CommandExecutor;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates all the project dependencies and their ProjectDependency objects
 * in order to calculate and write the reported usage from jacoco.
 */
public class ProjectDependencies {

    public static List<ProjectDependency> projectDependencies = new ArrayList<>();

    public static List<ProjectDependency> getAllProjectDependencies(CommandExecutor cmdExec, String targetDirectory, boolean genLockfile) {
        if (projectDependencies.isEmpty()) {
            generateAllProjectDependencies(cmdExec, targetDirectory, genLockfile);
        }
        return projectDependencies;
    }

    /**
     * Generate the project lockfile containing all the project dependencies
     * including their transitive dependencies and creates their corresponding
     * ProjectDependency object with child/parent dependencies.
     * @param cmdExec
     * @param targetDirectory
     * @param genLockfile
     */
    private static void generateAllProjectDependencies(CommandExecutor cmdExec, String targetDirectory, boolean genLockfile) {
        if(genLockfile){
            cmdExec.generateDependencyLockfile(targetDirectory);
        }
        String filePath = targetDirectory + "lockfile.json"; // Path to the JSON file
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new GsonBuilder().registerTypeAdapter(ProjectDependency.class, new ProjectDependencyDeserializer()).create();
            JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);
            JsonArray dependenciesArray = jsonElement.getAsJsonObject().getAsJsonArray("dependencies");
            for (JsonElement dependencyElement : dependenciesArray) {
                gson.fromJson(dependencyElement, ProjectDependency.class);
            }
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
            for (ProjectDependency parentDep : parentDeps) {
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
