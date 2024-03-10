# JACT - Java Absolute Coverage Tracker <img src=".img/jact-logo.png" align="left" height="135px"/>

[comment]: <> (Include coverage stats etc here later)
<br/> 
<br/> 
<br/> 

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

### Building the test project and inspecting its coverage report:
- Clone this repo and build the project from the root folder:
    ```
    mvn clean install
    ```
  
- Navigate into the root of the test project: `./src/main/it/sanity-check-project` and again execute:
    ```
    mvn clean install
    ```

The report will now be located under the test project `/target/jact-report` where its `index.html` file is a good place to start. 


### Using JACT:
After fulfilling the prerequsites in your project, clone this repo and execute `mvn clean install` and add this plugin to your pom.xml file.
```xml
<plugin>
    <groupId>java.absolute.coverage.tracker</groupId>
    <artifactId>jact</artifactId>
    <version>1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>coverage-report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```