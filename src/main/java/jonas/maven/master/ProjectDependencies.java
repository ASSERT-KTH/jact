package jonas.maven.master;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jonas.maven.master.CompleteCoverageMojo.readInputStream;

import com.google.gson.*;
import jonas.maven.master.ProjectDependency;
import org.checkerframework.checker.units.qual.A;

public class ProjectDependencies {

    static List<ProjectDependency> projectDependencies = new ArrayList<>();

    public static List<ProjectDependency> getAllProjectDependencies(){
        if(projectDependencies.isEmpty()){
            generateAllProjectDependencies();
        }
        return projectDependencies;
    }

    public static void generateAllProjectDependencies() {
        generateDependencyLockfile();
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


    public static void generateDependencyLockfile(){
        try {
            // Command to be executed
            String command = "mvn io.github.chains-project:maven-lockfile:generate -Dreduced=true";

            // Create a process builder with a shell
            //ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command); // For Windows
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command); // For Linux

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Get the input stream of the process
            InputStream inputStream = process.getInputStream();

            // Read the output
            String output = readInputStream(inputStream);

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Print the output
            System.out.println("Exit Code: " + exitCode);
            System.out.println("Output:\n" + output);

            // Move the generated lockfile.json to ./target/jact-report/lockfile.json
            File sourceFile = new File("./lockfile.json");
            File targetDir = new File("./target/jact-report/");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            File targetFile = new File(targetDir, "lockfile.json");

            if (sourceFile.renameTo(targetFile)) {
                System.out.println("File moved successfully to " + targetFile.getAbsolutePath());
            } else {
                System.out.println("Failed to move the file");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
