package jact;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class DirectoryUtils {
    public static void copyDirectory(File sourceDir, File destDir) {
        // Create the destination directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Get all files from the source directory
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                // Copy the file to the destination directory
                try {
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void removeDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    removeDirectory(file); // Recursive call to remove subdirectories
                } else {
                    if (!file.delete()) {
                        throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        if (!dir.delete()) {
            throw new RuntimeException("Failed to delete directory: " + dir.getAbsolutePath());
        }
    }


    public static void createDir(String directoryPath) {
        // Will take a list of dependencies later

        File dir = new File(directoryPath);

        boolean success = dir.mkdirs();

        // Check if directory creation was successful
        if (success) {
            System.out.println("Report directory created successfully.");
        } else {
            System.out.println("Failed to create dependency report directory." + directoryPath);
        }
    }

    public static void moveDirectory(File sourceDir, String destDirName) {
        Path sourcePath = sourceDir.toPath();
        Path destPath = Paths.get(destDirName).resolve(sourcePath.getFileName());
        try {
            Files.move(sourcePath, destPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
