# JACT - Java Absolute Coverage Tracker <img src=".img/jact-logo.png" align="left" height="135px"/>

[comment]: <> (Include coverage stats etc here later)
<br/> 
<br/> 
<br/> 

### What is JACT?
JACT is a Maven plugin for my master's thesis generates a complete code coverage report of a Java project using Maven. 
The generated report includes the conventional project coverage in addition to the coverage of the entire dependency 
tree. JACT uses the JaCoCo CLI to generate the coverage report on the packaged Uber-jar. Thereafter, it augments the 
unorganized package report by resolving the package dependency heritage and calculates the coverage of all dependencies.

##### Current prerequisites:
- Required to use JaCoCo
- Use either the Maven Shade Plugin to create an Uber-jar (including all the dependencies along with their transitive 
  dependencies). Example provided under 'Using JACT'.
- Resulting Uber-jar has to be placed under ./target/

### Building the test project and inspecting its coverage report:
- Clone this repo and build the project from the root folder:
    ```
    mvn clean install
    ```
  
- Navigate into the root of the test project: `./src/main/it/test-project` and again execute:
    ```
    mvn clean install
    ```

The report will now be located under the test project `/target/jact-report` where its `index.html` file is a good place to start. 


### Using JACT:
After fulfilling the prerequsites in your project, clone this repo and execute `mvn clean install`.

For JACT to create a complete coverage report you need to package your project separately as a Uber-jar. This is done with
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

JACT creates the report during the `install`-phase since it requires a packaged Uber-jar. Executing `mvn clean install`
in your project will create a `jact-report` directory under `./target/jact-report`.


**_Custom Maven-Shade-Plugin jar name:_** </br>
If your project packages a Uber-jar under a custom name the custom name can be provided by adding this to your JACT
configuration:

```xml

<configuration>
  <shadedJarName>${Custom-Jar-Name}</shadedJarName> <!-- Without .jar -->
</configuration>
```

#### Example JACT configuration with a custom jar-name:
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

