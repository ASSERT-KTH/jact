# JACT - Java Absolute Coverage Tracker
#### *(NAME IS NOT FINAL, used to create a unique report directory)*

### What is JACT?
JACT is a work in progress Maven plugin for my master's thesis which will generate a complete code coverage report of a
Java project using Maven. The report includes coverage of both the project and all of its dependencies (even transitive).
It currently uses JaCoCo together with its CLI-module to generate the coverage report, and the goal is to later improve 
upon JaCoCo to capture even more accurate coverage.

##### Current prerequisites:
- Required to use JaCoCo
- Need to be able to build a FAT-jar (including the project and its dependencies)
- Provide the name of the FAT-jar before packaging (for report generation)
- Resulting jar has to be place under ./target/