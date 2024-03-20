package jact;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

import static jact.PackageToDependencyResolver.packageToDepPaths;

public class JacocoXMLParser {
    /*
    TODO:
        - Write the complete XML report
     */
    public static DependencyUsage dependencyUsage = new DependencyUsage();
    public static DependencyUsage projectUsage = new DependencyUsage();

    public static final String REPORTPATH = "./target/jact-report/";
    private static ProjectDependency thisProject = new ProjectDependency();

    private static final String FINALREPORTPATH = REPORTPATH + "jact_report.xml";

    public static void groupPackageByDep(List<ProjectDependency> dependencies,
                                         Map<String, Set<String>> projPackagesAndClassMap,
                                         String localRepoPath, String projId){
        thisProject.setId(projId);


        try {
            // Load Jacoco XML report
            File xmlFile = new File(REPORTPATH + "jacoco_report.xml");

            // Disable DTD validation
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Format and overwrite the XML file
            formatXml(xmlFile, doc);

            // Group coverage data by package name
            Map<String, Document> packageReports = new HashMap<>();
            NodeList nodeList = doc.getElementsByTagName("package");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element packageElement = (Element) nodeList.item(i);
                String packageName = packageElement.getAttribute("name");
                Document packageReport = createPackageReport(packageElement);
                packageReports.put(packageName, packageReport);
            }

            // Write separate XML reports for each package in the current directory
            for (Map.Entry<String, Document> entry : packageReports.entrySet()) {
                String packageName = entry.getKey();
                Document packageReport = entry.getValue();
                String filename = packageName.replace("/", "-") + ".xml"; // Use package name for filename
                writeXML(packageReport, REPORTPATH + "xml_reports/" + filename);
                //System.out.println("filename: " + filename);
            }

            //System.out.println("Separate XML reports created for each package in the current directory.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        File reportDir = new File(REPORTPATH + "xml_reports/");

        // Check if the directory exists
        if (reportDir.exists() && reportDir.isDirectory()) {
            // List all files in the directory
            File[] files = reportDir.listFiles();
            if (files != null) {
                // Iterate through each file
                for (File file : files) {
                    // Extract filename without extension
                    String filename = file.getName().replaceAll("\\.xml$", "");
                    //System.out.println(filename);
                    // Call your function with the filename
                    filename = filename.replace("-", ".");
                    ProjectDependency matchedDep = packageToDepPaths(filename, dependencies, projPackagesAndClassMap, localRepoPath);
                    //System.out.println(matchedDep.getId());
                    //extractUsage()

                    // It needs to add to either the dependencyUsage or the project usage,
                    // If the matched dependency is a project package then
                    if(matchedDep.getId() != null){
                        // We know the package comes from a dependency
                        extractCounterValues(REPORTPATH + "xml_reports/" + file.getName(), matchedDep, dependencyUsage, file.getName());
                    }else{
                        extractCounterValues(REPORTPATH + "xml_reports/" + file.getName(), matchedDep, projectUsage, file.getName());

                    }


                    // Add the dependency usage to each matched dependency.
                    // Create templates or a way to write the xml package to the report.
                    //matchedDep.
                }
            } else {
                System.out.println("No files found in the directory.");
            }
        } else {
            System.out.println("Directory does not exist or is not a directory.");
        }

        System.out.println("PROJECT USAGE TOTAL: " + projectUsage.totalUsageToXML());
        System.out.println("DEPENDENCY USAGE TOTAL: " + dependencyUsage.totalUsageToXML());

        writeCompleteReport(dependencies);

    }


    public static void writeCompleteReport(List<ProjectDependency> dependencies) {
        File finalReport = new File(FINALREPORTPATH);

        try (FileWriter writer = new FileWriter(finalReport)) {
            for (ProjectDependency pd : dependencies) {
                for (Map.Entry<String, DependencyUsage> entry : pd.packageUsageMap.entrySet()) {
                    File packageFile = new File(REPORTPATH + "xml_reports/" + entry.getKey());
                    try (BufferedReader reader = new BufferedReader(new FileReader(packageFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line + "\n");
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading package file: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing final report: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Final report has been written to: " + finalReport.getAbsolutePath());
    }



    public static void extractCounterValues(String inputFilePath, ProjectDependency matchedDep, DependencyUsage usage, String packageName) {
        try {
            // Parse the XML file
            File inputFile = new File(inputFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Get the <package> nodes
            NodeList packageNodes = doc.getElementsByTagName("package");
            for (int i = 0; i < packageNodes.getLength(); i++) {
                Element packageElement = (Element) packageNodes.item(i);
                // Get the child nodes of the package
                NodeList childNodes = packageElement.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    if (childNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element childElement = (Element) childNodes.item(j);
                        // Check if the child element is a counter
                        if (childElement.getNodeName().equals("counter")) {
                            // Extract the attributes and call a function with the values
                            String type = childElement.getAttribute("type");
                            long missed = Long.parseLong(childElement.getAttribute("missed"));
                            long covered = Long.parseLong(childElement.getAttribute("covered"));
                            DependencyUsage packageUsage = new DependencyUsage();
                            processCounterValues(type, missed, covered, matchedDep, packageUsage);
                            //matchedDep.packageUsageMap.put(file.getName(), packageUsage);
                            matchedDep.dependencyUsage.addAll(packageUsage);
                            usage.addAll(packageUsage);
                            matchedDep.packageUsageMap.put(packageName, packageUsage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCounterValues(String type, long missed, long covered, ProjectDependency matchedDep, DependencyUsage packageUsage) {
        // Process the counter values here
        //System.out.println("Type: " + type + ", Missed: " + missed + ", Covered: " + covered);
        // You can call other functions or perform any other operations as needed

        // Add the total here!
        switch (type){
            case "INSTRUCTION":
                // do stuff
                packageUsage.addMissedInstructions(missed);
                packageUsage.addTotalInstructions(missed + covered);
                break;
            case "BRANCH":
                // do stuff
                packageUsage.addMissedBranches(missed);
                packageUsage.addTotalBranches(missed + covered);
                break;
            case "LINE":
                // do stuff
                packageUsage.addMissedLines(missed);
                packageUsage.addTotalLines(missed + covered);
                break;
            case "COMPLEXITY":
                // do stuff
                packageUsage.addMissedCyclomaticComplexity(missed);
                packageUsage.addCyclomaticComplexity(missed + covered);
                break;
            case "METHOD":
                // do stuff
                packageUsage.addMissedMethods(missed);
                packageUsage.addTotalMethods(missed + covered);
                break;
            case "CLASS":
                // do stuff
                packageUsage.addMissedClasses(missed);
                packageUsage.addTotalClasses(missed + covered);
                break;
            default:
                System.out.println("Could not match usage type with parsed type: " + type);
        }
    }


    private static Document createPackageReport(Element packageElement) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); // Disable DTD validation
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        Element rootElement = doc.createElement("report");
        doc.appendChild(rootElement);

        // Copy package node and its children to the new document
        Element importedPackage = (Element) doc.importNode(packageElement, true);
        rootElement.appendChild(importedPackage);

        return doc;
    }

    private static void writeXML(Document doc, String outputPath) throws Exception {
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();

        // Create parent directories if they don't exist
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalStateException("Couldn't create directory: " + parentDir);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outputFile);
        transformer.transform(source, result);


        // Disable DTD validation
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document formattedDoc = dBuilder.parse(outputFile);
        formattedDoc.getDocumentElement().normalize();

        // Format and overwrite the XML file
        formatXml(outputFile, formattedDoc);
    }

    private static void formatXml(File file, Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set indentation properties
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // Indentation size

        // Write the DOM document to the file
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);

        //System.out.println("Formatted XML has been written to: " + file.getAbsolutePath());
    }



}



