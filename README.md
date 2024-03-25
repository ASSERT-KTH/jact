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
- Use either the Maven Shade Plugin to create a FAT-jar (including all the dependencies along with their transitive 
  dependencies). Example provided under 'Using JACT'.
- Resulting FAT-jar has to be place under ./target/

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
After fulfilling the prerequsites in your project, clone this repo and execute `mvn clean install`.

For JACT to create a complete coverage report you need to package your project separately as a FAT-jar. This is done with
the Maven Shade Plugin. Add this minimal configuration to the pom.xml file in your project:
```xml
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-shade-plugin</artifactId>
<version>3.2.4</version> <!-- Adjust version if needed -->
<executions>
    <execution>
        <phase>package</phase>
        <goals>
            <goal>shade</goal>
        </goals>
        <configuration>
            <shadedArtifactAttached>true</shadedArtifactAttached>
        </configuration>
    </execution>
</executions>
</plugin>
```

Now just add the JACT plugin to the pom.xml file in your project:
```xml
<plugin>
    <groupId>java.absolute.coverage.tracker</groupId>
    <artifactId>jact</artifactId>
    <version>1.0</version> <!-- Adjust version if needed -->
    <executions>
        <execution>
            <goals>
                <goal>coverage-report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

JACT creates the report during the `install`-phase since it requires a packaged FAT-jar. Executing `mvn clean install`
in your project will create a `jact-report` directory under `./target/jact-report`. 



XML Report Structure:
```xml
<report name="JACT Coverage Report (Generated with JaCoCo)">
    <group name="Dependencies">
      <group name="dependencyId">
        <package name="packageName">
          <!-- Package usage info -->
          <!-- Total Package Coverage -->
        </package>
        <!-- Rest the packages in the dependency -->
            <!-- ... -->
      </group>
      <!-- Rest of the dependencies-->
        <!-- ... -->
      <!-- Dependency Coverage Total -->
    </group>
    <group name="Project Packages">
      <package name="packageName">
        <!-- Package usage info -->
        <!-- Total Package Coverage -->
      </package>
      <!-- Rest of the project packages-->
        <!-- ... -->
      <!-- Project Coverage Total -->
    </group>
  <!-- Overall Coverage Total -->
</report>
```

