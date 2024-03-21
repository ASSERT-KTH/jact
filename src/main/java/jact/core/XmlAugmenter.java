package jact.core;

import jact.depUtils.DependencyUsage;
import jact.depUtils.ProjectDependency;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

import static jact.depUtils.PackageToDependencyResolver.packageToDepPaths;

public class XmlAugmenter {
    public static DependencyUsage dependencyUsage = new DependencyUsage();
    public static DependencyUsage projectUsage = new DependencyUsage();

    public static final String REPORTPATH = "./target/jact-report/";
    private static ProjectDependency thisProject = new ProjectDependency();

    private static final String FINALREPORTPATH = REPORTPATH + "jact_report.xml";

    private static String xmlReportTag = "<report name=\"JACT Coverage Report (Generated with JaCoCo)\">";
    private static String sessionInfo;

    private static DependencyUsage totalUsage = new DependencyUsage();


    public static String extractXmlHeader(String xmlFilePath) {

            StringBuilder sessionInfo = new StringBuilder();

            try {
                // Create a DocumentBuilder
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder builder = factory.newDocumentBuilder();

                // Parse the XML file
                Document doc = builder.parse(new File(xmlFilePath));

                // Get the root element
                Element root = doc.getDocumentElement();

                // Find the sessioninfo node
                NodeList sessionInfoList = root.getElementsByTagName("sessioninfo");
                if (sessionInfoList.getLength() > 0) {
                    Node sessionInfoNode = sessionInfoList.item(0);
                    sessionInfo.append(nodeToString(sessionInfoNode));
                }
            } catch (Exception e) {
                System.err.println("Error reading XML file: " + e.getMessage());
                e.printStackTrace();
            }

            return sessionInfo.toString();
        }

// Helper method to convert a Node to String
        private static String nodeToString(Node node) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<").append(node.getNodeName());

            // Append attributes
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    stringBuilder.append(" ").append(attr.getNodeName()).append("=\"").append(attr.getNodeValue()).append("\"");
                }
            }

            stringBuilder.append("/>");
            return stringBuilder.toString();
        }


    public static void writeXMLString(String xmlContent, String outputPath) throws Exception {
        // Create parent directories if they don't exist
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalStateException("Couldn't create directory: " + parentDir);
        }

        // Create a new Document from the cleaned XML content string
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

        // Write the Document to the output file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }



    public static void groupPackageByDep(List<ProjectDependency> dependencies,
                                         Map<String, Set<String>> projPackagesAndClassMap,
                                         String localRepoPath, String projId){
        thisProject.setId(projId);

        Map<String, Document> packageReports2 = new HashMap<>();
        try {
            // Load Jacoco XML report
            File xmlFile = new File(REPORTPATH + "jacoco_report.xml");
            sessionInfo = extractXmlHeader(REPORTPATH + "jacoco_report.xml");
            extractCounterValues(REPORTPATH + "jacoco_report.xml", new ProjectDependency(), totalUsage, "total");

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
            packageReports2 = packageReports;
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
                        extractCounterValues(REPORTPATH + "xml_reports/" + file.getName(), thisProject, projectUsage, file.getName());

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

        writeCompleteReport(dependencies, packageReports2);

    }

    public static void writeCompleteReport(List<ProjectDependency> dependencies, Map<String, Document> packageReports) {
        String depOpeningTag = "<group name=\"Dependencies\">";
        String projOpeningTag = "<group name=\"Project\">";
        String closingTag = "</group>";
        File finalReport = new File(FINALREPORTPATH);

        StringBuilder finalReportString = new StringBuilder();

        try (FileWriter writer = new FileWriter(finalReport)) {
            writer.write(xmlReportTag);
            writer.write(sessionInfo);
            writer.write(depOpeningTag);
            for (ProjectDependency pd : dependencies) {
                if(pd.getScope().equals("test")){
                    continue;
                }
                String openingTag = "<group name=\"" + pd.getId() + "\">";
                writer.write(openingTag.trim());

                for (Map.Entry<String, DependencyUsage> entry : pd.packageUsageMap.entrySet()) {
                    File packageFile = new File(REPORTPATH + "xml_reports/" + entry.getKey());
                    try (BufferedReader reader = new BufferedReader(new FileReader(packageFile))) {
                        String line;
                        boolean firstLineSkipped = false;
                        boolean reportTagSkipped = false;

                        while ((line = reader.readLine()) != null) {
                            if (!firstLineSkipped && line.startsWith("<?xml")) {
                                // Skip the first line
                                firstLineSkipped = true;
                                continue;
                            }
                            if (!reportTagSkipped && line.trim().startsWith("<report")) {
                                // Skip the report tag and its closing tag
                                reportTagSkipped = true;
                                continue;
                            }
                            if (reportTagSkipped && line.trim().startsWith("</report>")) {
                                // Skip the closing tag
                                continue;
                            }
                            writer.write(line.trim());
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading package file: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                writer.write(closingTag);
            }
            // Write total dependency usage
            writer.write(dependencyUsage.totalUsageToXML());
            writer.write(closingTag);

            // Write the project packages
            String openingTag = "<group name=\"" + "Project Packages" + "\">";
            writer.write(openingTag);
            for (Map.Entry<String, DependencyUsage> entry : thisProject.packageUsageMap.entrySet()) {

                File packageFile = new File(REPORTPATH + "xml_reports/" + entry.getKey());
                try (BufferedReader reader = new BufferedReader(new FileReader(packageFile))) {
                    String line;
                    boolean firstLineSkipped = false;
                    boolean reportTagSkipped = false;

                    while ((line = reader.readLine()) != null) {
                        if (!firstLineSkipped && line.startsWith("<?xml")) {
                            // Skip the first line
                            firstLineSkipped = true;
                            continue;
                        }
                        if (!reportTagSkipped && line.trim().startsWith("<report")) {
                            // Skip the report tag and its closing tag
                            reportTagSkipped = true;
                            continue;
                        }
                        if (reportTagSkipped && line.trim().startsWith("</report>")) {
                            // Skip the closing tag
                            continue;
                        }
                        writer.write(line.trim());
                    }
                } catch (IOException e) {
                    System.err.println("Error reading package file: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            writer.write(closingTag);

            // Write overall total here
            writer.write(totalUsage.totalUsageToXML());

            writer.write("</report>");

        } catch (IOException e) {
            System.err.println("Error writing final report: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // Disable DTD validation
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document formattedDoc = dBuilder.parse(finalReport);
            formattedDoc.getDocumentElement().normalize();
            // Format and overwrite the XML file
            formatXml(finalReport, formattedDoc);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        System.out.println("Final report has been written to: " + finalReport.getAbsolutePath());
    }





    public static void extractCounterValues(String inputFilePath, ProjectDependency matchedDep, DependencyUsage usage, String packageName) {
        try {
            // Parse the XML file
            File inputFile = new File(inputFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
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
        // Add the total here!
        switch (type){
            case "INSTRUCTION":
                packageUsage.addMissedInstructions(missed);
                packageUsage.addTotalInstructions(missed + covered);
                break;
            case "BRANCH":
                packageUsage.addMissedBranches(missed);
                packageUsage.addTotalBranches(missed + covered);
                break;
            case "LINE":
                packageUsage.addMissedLines(missed);
                packageUsage.addTotalLines(missed + covered);
                break;
            case "COMPLEXITY":
                packageUsage.addMissedCyclomaticComplexity(missed);
                packageUsage.addCyclomaticComplexity(missed + covered);
                break;
            case "METHOD":
                packageUsage.addMissedMethods(missed);
                packageUsage.addTotalMethods(missed + covered);
                break;
            case "CLASS":
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



