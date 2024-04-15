package jact.depUtils;

import com.google.gson.*;
import jact.plugin.AbstractReportMojo;

import java.io.FileReader;
import java.util.*;

import static jact.depUtils.ProjectDependency.depIdToDirName;
import static jact.depUtils.ProjectDependency.depToDirName;
import static jact.plugin.AbstractReportMojo.getReportPath;
import static jact.utils.CommandExecutor.generateDependencyLockfile;

/**
 * Creates all the project dependencies and their ProjectDependency objects
 * in order to calculate and write the reported usage from jacoco.
 */
public class ProjectDependencies {
    public static Map<String, ProjectDependency> projectDependenciesMap;
    public static Map<String, DependencyUsage> transitiveUsageMap;
    public static List<String> rootDepIds;

    private static Set<String> visited;

    private static boolean skipTestDependencies;

    public static Map<String, ProjectDependency> getAllProjectDependencies(String targetDirectory,
                                                                           boolean genLockfile,
                                                                           boolean skipTestDeps) {
        projectDependenciesMap = new HashMap<>();
        transitiveUsageMap = new HashMap<>();
        rootDepIds = new ArrayList<>();
        visited = new HashSet<>();

        skipTestDependencies = skipTestDeps;

        if (projectDependenciesMap.isEmpty()) {
            generateAllProjectDependencies(targetDirectory, genLockfile);
        }
        return projectDependenciesMap;
    }

    /**
     * Generate the project lockfile containing all the project dependencies
     * including their transitive dependencies and creates their corresponding
     * ProjectDependency object with child/parent dependencies.
     * @param targetDirectory
     * @param genLockfile
     */
    private static void generateAllProjectDependencies(String targetDirectory, boolean genLockfile) {
        if(genLockfile){
            generateDependencyLockfile(targetDirectory);
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
            ProjectDependency parentDep = new ProjectDependency();
            return parseDependency(jsonObject, parentDep);
        }

        private ProjectDependency parseDependency(JsonObject jsonObject, ProjectDependency parentDep) {
            String dependencyId = jsonObject.get("id").getAsString();
            String dependencyScope = jsonObject.get("scope").getAsString();
            String parentString = jsonObject.has("parent") ? jsonObject.get("parent").getAsString() : "";

            if((skipTestDependencies && dependencyScope.equals("test")) || dependencyScope.equals("provided")){
                //Skipping provided- and test-scope dependencies
                return new ProjectDependency();
            }
            if (visited.contains(dependencyId)) {
                // If the dependency has been visited before, find it, add the parent and return it.
                ProjectDependency pd = projectDependenciesMap.get(dependencyId);
                if(parentDep.getId() != null){
                    pd.addParentDep(parentDep);
                    //setupChildDependency(pd, parentDep);
                }else if(!parentString.isEmpty()){
                    pd.addParentDep(parentDep);
                    //setupChildDependency(pd, projectDependenciesMap.get(depIdToDirName(parentString)));
                }else{
                    pd.rootDep = true;
                }
                JsonArray childrenJsonArray = jsonObject.getAsJsonArray("children");
                if (!childrenJsonArray.isEmpty()) {
                    addTransitive(pd);
                    for (JsonElement element : childrenJsonArray) {
                        ProjectDependency child = parseDependency(element.getAsJsonObject(), pd);
                        pd.addChildDep(child);
                    }
                }
                return pd;
            }
            visited.add(dependencyId);

            ProjectDependency projectDependency = new ProjectDependency();
            projectDependency.setId(jsonObject.has("id") ? jsonObject.get("id").getAsString() : "");
            projectDependency.setGroupId(jsonObject.has("groupId") ? jsonObject.get("groupId").getAsString() : "");
            projectDependency.setArtifactId(jsonObject.has("artifactId") ? jsonObject.get("artifactId").getAsString() : "");
            projectDependency.setVersion(jsonObject.has("selectedVersion") ? jsonObject.get("selectedVersion").getAsString() : "");
            projectDependency.setScope(jsonObject.has("scope") ? jsonObject.get("scope").getAsString() : "");


            // Adding the directory name to the potential paths to report the usage
            // Build the report path
            if(parentDep.getId() != null){
                projectDependency.addParentDep(parentDep);
                //setupChildDependency(projectDependency, parentDep);
            }else if(!parentString.isEmpty()){
                projectDependency.addParentDep(parentDep);
                //setupChildDependency(projectDependency, projectDependenciesMap.get(depIdToDirName(parentString)));
            }else{
                projectDependency.rootDep = true;
            }

            projectDependency.setReportPath(getReportPath() + "dependencies/" + depToDirName(projectDependency) + "/");

            //System.out.println("ADDING: " + projectDependency.toString());
            projectDependenciesMap.put(projectDependency.getId(), projectDependency);
            JsonArray childrenJsonArray = jsonObject.getAsJsonArray("children");
            if (!childrenJsonArray.isEmpty()) {
                addTransitive(projectDependency);
                for (JsonElement element : childrenJsonArray) {
                    ProjectDependency child = parseDependency(element.getAsJsonObject(), projectDependency);
                    projectDependency.addChildDep(child);
                }
            }
            return projectDependency;
        }
    }


    private static void addTransitive(ProjectDependency pd){
        if(!transitiveUsageMap.containsKey(pd.getId())){
            transitiveUsageMap.put(pd.getId(), new DependencyUsage());
        }
    }

}