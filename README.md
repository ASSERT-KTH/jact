# JACT - Java Absolute Coverage Tracker
#### *(NAME IS NOT FINAL, used to create a unique report directory)*

This is a work in progress Maven plugin for my master's thesis which will generate a code coverage report of a Java 
project using Maven that includes coverage all of its dependencies (even transitive dependencies). It currently uses
JaCoCo together with its CLI-module to generate the coverage report, and the goal is to later improve upon JaCoCo to 
capture even more accurate coverage.