package jact.test.depUtils;

import jact.depUtils.DependencyUsage;
import jact.depUtils.DependencyUsage.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static jact.depUtils.DependencyUsage.barLength;
import static jact.depUtils.DependencyUsage.percentage;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;


public class DependencyUsageTest {
    DependencyUsage testUsage = new DependencyUsage();

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *      Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Adding missed/total branches correctly updates
     *                     its corresponding fields.
     */
    public void addBranchNrTest(){
        Assertions.assertEquals(0, testUsage.getMissedBranches());
        testUsage.addMissedBranches(3);
        Assertions.assertEquals(3, testUsage.getMissedBranches());
        Assertions.assertEquals(0, testUsage.getTotalBranches());
        testUsage.addTotalBranches(3);
        Assertions.assertEquals(3, testUsage.getTotalBranches());
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *      Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Adding missed/total lines correctly updates
     *                     its corresponding fields.
     */
    public void addLinesNrTest(){
        Assertions.assertEquals(0, testUsage.getMissedLines());
        testUsage.addMissedLines(3);
        Assertions.assertEquals(3, testUsage.getMissedLines());
        Assertions.assertEquals(0, testUsage.getTotalLines());
        testUsage.addTotalLines(3);
        Assertions.assertEquals(3, testUsage.getTotalLines());
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *      Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Adding missed/total instructions correctly updates
     *                     its corresponding fields.
     */
    public void addInstructionsNrTest(){
        Assertions.assertEquals(0, testUsage.getMissedInstructions());
        testUsage.addMissedInstructions(3);
        Assertions.assertEquals(3, testUsage.getMissedInstructions());
        Assertions.assertEquals(0, testUsage.getTotalInstructions());
        testUsage.addTotalInstructions(3);
        Assertions.assertEquals(3, testUsage.getTotalInstructions());
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *      Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Adding missed/total Cyclomatic Complexity correctly
     *                     updates its corresponding fields.
     */
    public void addCyclomaticComplexityNrTest(){
        Assertions.assertEquals(0, testUsage.getMissedCyclomaticComplexity());
        testUsage.addMissedCyclomaticComplexity(3);
        Assertions.assertEquals(3, testUsage.getMissedCyclomaticComplexity());
        Assertions.assertEquals(0, testUsage.getCyclomaticComplexity());
        testUsage.addCyclomaticComplexity(3);
        Assertions.assertEquals(3, testUsage.getCyclomaticComplexity());
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *      Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Adding missed/total methods correctly updates
     *                     its corresponding fields.
     */
    public void addMethodNrTest(){
        Assertions.assertEquals(0, testUsage.getMissedMethods());
        testUsage.addMissedMethods(3);
        Assertions.assertEquals(3, testUsage.getMissedMethods());
        Assertions.assertEquals(0, testUsage.getTotalMethods());
        testUsage.addTotalMethods(3);
        Assertions.assertEquals(3, testUsage.getTotalMethods());
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *      Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Adding missed/total classes correctly updates
     *                     its corresponding fields.
     */
    public void addClassesNrTest(){
        Assertions.assertEquals(0, testUsage.getMissedClasses());
        testUsage.addMissedClasses(3);
        Assertions.assertEquals(3, testUsage.getMissedClasses());
        Assertions.assertEquals(0, testUsage.getTotalClasses());
        testUsage.addTotalClasses(3);
        Assertions.assertEquals(3, testUsage.getTotalClasses());
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *     Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Calculating the percentage correctly returns
     *                     a string representation of the result.
     */
    public void percentageTest(){
        assertEquals("44%", percentage(44, 100));
        assertEquals("67%", percentage(67, 100));
        assertEquals("20%", percentage(1, 5));
        assertEquals("12%", percentage(151432412, 1231252144));
    }

    @Test
    /**
     * Requirements: An initialized DependencyUsage object.
     * Contract:
     *     Pre-condition: A newly initialized DependencyUsage Object.
     *     Post-condition: Calculating the bar-length correctly returns
     *                     the percentage number.
     */
    public void barLengthTest(){
        assertEquals(44, barLength(44, 100));
        assertEquals(67, barLength(67, 100));
        assertEquals(20, barLength(1, 5));
        assertEquals(12, barLength(151432412, 1231252144));
    }

}
