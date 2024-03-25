package jact.test.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static jact.utils.CommandExecutor.copyPNGImage;
import static jact.utils.DirectoryUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DirectoryUtilsTest {

    static String testDirectory = "./src/test/java/jact/test/testingDir/";

    @AfterAll
    public static void cleanUpTestDirs(){
        removeDirectory(new File(testDirectory));
        Assertions.assertFalse(new File(testDirectory).exists());
    }

    @Test
    /**
     * Requirements: Nothing.
     * Contract:
     *     Post-condition: Creating and removing empty directories
     *                     is performed correctly.
     */
    public void createAndRemoveEmptyDirectoryTest(){
        Assertions.assertFalse(new File(testDirectory + "createdDir").exists());
        createDir(testDirectory + "createdDir");
        assertTrue(new File(testDirectory + "createdDir").exists());

        removeDirectory(new File(testDirectory + "createdDir"));
        Assertions.assertFalse(new File(testDirectory + "createdDir").exists());
    }

    @Test
    /**
     * Requirements: Nothing.
     * Contract:
     *     Post-condition: Creating and removing directories with content
     *                     is performed correctly.
     */
    public void createAndRemoveDirectoryTest() throws IOException {
        createDir(testDirectory + "createdDir");
        assertTrue(new File(testDirectory + "createdDir").exists());
        Assertions.assertFalse(new File(testDirectory + "createdDir" + "/jact-logo.png").exists());
        copyPNGImage("jact-logo.png", testDirectory + "createdDir");
        assertTrue(new File(testDirectory + "createdDir" + "/jact-logo.png").exists());

        // Recursive deletion of files contained in the directory
        removeDirectory(new File(testDirectory + "createdDir"));
        Assertions.assertFalse(new File(testDirectory + "createdDir" + "/jact-logo.png").exists());
        Assertions.assertFalse(new File(testDirectory + "createdDir").exists());
    }


    @Test
    /**
     * Requirements: Nothing.
     * Contract:
     *     Post-condition: Copying and moving directories with content
     *                     is performed correctly.
     */
    public void copyAndMoveDirectoryTest() throws IOException {
        createDir(testDirectory + "createdDir");
        assertTrue(new File(testDirectory + "createdDir").exists());
        copyPNGImage("jact-logo.png", testDirectory + "createdDir");
        assertTrue(new File(testDirectory + "createdDir" + "/jact-logo.png").exists());

        // Recursive deletion of files contained in the directory
        createDir(testDirectory + "movedDir");
        moveDirectory(new File(testDirectory + "createdDir"), testDirectory + "movedDir");
        Assertions.assertFalse(new File(testDirectory + "createdDir" + "/jact-logo.png").exists());
        Assertions.assertFalse(new File(testDirectory + "createdDir").exists());
        assertTrue(new File(testDirectory + "/movedDir" +"/createdDir").exists());
        assertTrue(new File(testDirectory + "/movedDir" +"/createdDir" + "/jact-logo.png").exists());


        // Remove the moved directory
        removeDirectory(new File(testDirectory + "movedDir"));
        Assertions.assertFalse(new File(testDirectory + "/movedDir" +"/createdDir" + "/jact-logo.png").exists());
        Assertions.assertFalse(new File(testDirectory + "/movedDir" +"/createdDir").exists());
    }
}
