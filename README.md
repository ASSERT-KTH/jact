# JACT - Java Absolute Coverage Tracker <img src=".img/jact-logo.png" align="left" height="135px"/>

[comment]: <> (Include coverage stats etc here later)
<br/> 
<br/> 
<br/> 

### What is JACT?
JACT is a work in progress Maven plugin for my master's thesis which will generate a complete code coverage report of a
Java project using Maven. The report includes coverage of both the project and all of its dependencies (even transitive).
JACT uses the JaCoCo CLI to generate the coverage report on the packaged FAT-jar and augments the unorganized package 
report by resolving the package dependency heritage uncovering the complete dependency usage by including the transitive
dependency usage.

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
                <goal>${report-format}</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
The property `${report-format}` can take one of the following values depending on the desired report format:
* **html-report** Generates the HTML report.
* **xml-report** Generates the XML report.
* **combined-report** Generates both the HTML and XML report.

Custom Maven-Shade-Plugin jar name:
If your project packages a FAT-jar under a custom name the custom name can be provided by adding this to your JACT
configuration:

```xml

<configuration>
  <shadedJarName>${Custom-Jar-Name}</shadedJarName> <!-- Without .jar -->
</configuration>
```

JACT creates the report during the `install`-phase since it requires a packaged FAT-jar. Executing `mvn clean install`
in your project will create a `jact-report` directory under `./target/jact-report`. 

Example JACT configuration with a custom jar-name:
```xml
<plugin>
    <groupId>java.absolute.coverage.tracker</groupId>
    <artifactId>jact</artifactId>
    <version>1.0</version> <!-- Adjust version if needed -->
    <configuration>
      <shadedJarName>jact-example</shadedJarName> <!-- Without .jar -->
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>html-report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Report Formats Details
JACT supports both HTML and XML report formats. The intended usage for the HTML version is to create a human-readable
format that is quickly interpreted. The XML format is intended as a raw report format for any purpose, which could for
instance be to get dependency coverage down to the method level for removing unwanted bytecode.

##### The XML report structure can be seen in the following example:
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

