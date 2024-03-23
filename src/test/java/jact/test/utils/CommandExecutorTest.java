package jact.test.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static jact.utils.CommandExecutor.copyJacocoCliJar;
import static jact.utils.DirectoryUtils.removeDirectory;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandExecutorTest {

//    @AfterAll
//    public static void cleanupExecutorTestFiles(){
//        //Assertions.assertFalse(new File(testDirectory).exists());
//    }
//
//    @Test
//    public void copyJacocoCliJarTest() throws IOException, URISyntaxException {
//        Assertions.assertFalse(new File("./target/jact-resources/jacococli.jar").exists());
//        copyJacocoCliJar();
//        assertTrue(new File("./target/jact-resources/jacococli.jar").exists());
//        removeDirectory(new File("./target/jact-resources"));
//        Assertions.assertFalse(new File("./target/jact-resources").exists());
//    }
//    @Test
//    public void executeJacocoCLITest(){
//
//    }
//
//    @Test
//    public void copyDdtFileTest(){
//
//    }
//
//    @Test
//    public void copyPNGImageTest(){
//
//    }
//
//    @Test
//    public void genDependencyLockFileTest(){
//
//    }

}
