package jact.core;

import jact.depUtils.DependencyUsage;
import jact.depUtils.ProjectDependency;
import jact.utils.CommandExecutor;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static jact.depUtils.PackageToDependencyResolver.packageToDependency;
import static jact.plugin.AbstractReportMojo.getJactReportPath;
import static jact.utils.FileSystemUtils.removeFile;

/**
 * Creates the XML version of the JACT Report
 */
public class XmlAugmenter {
    private static DependencyUsage dependencyUsage;
    private static DependencyUsage projectUsage;
    private static ProjectDependency thisProject;
    private static DependencyUsage totalUsage;

    private static final String FINALREPORTPATH = getJactReportPath() + "jact_report.xml";

    private static String xmlDtd = "<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\">";
    private static String xmlReportTag = "<report name=\"JACT Coverage Report (Generated with JaCoCo)\">";
    private static String sessionInfo;

    private static Map<String, String> fileNameToPackageMap = new HashMap<>();


    public static void generateXmlReport(Map<String, ProjectDependency> dependenciesMap,
                                         Map<String, Set<String>> projPackagesAndClassMap,
                                         String localRepoPath, String projId) {
        dependencyUsage = new DependencyUsage();
        projectUsage = new DependencyUsage();
        thisProject = new ProjectDependency();
        totalUsage = new DependencyUsage();

        Map<String, Document> packageReports =
                extractUsageAndGeneratePackageReports(dependenciesMap, projPackagesAndClassMap, localRepoPath, projId);
        writeCompleteReport(dependenciesMap, packageReports);
    }


    /**
     * Extracts the header information form the jacoco XML report.
     *
     * @param xmlFilePath
     * @return String
     */
    private static String extractXmlHeader(String xmlFilePath) {

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

    /**
     * Creates individual XML reports for each package
     * in the jacoco XML report.
     *
     * @param dependenciesMap
     * @param projPackagesAndClassMap
     * @param localRepoPath
     * @param projId
     */
    private static Map<String, Document> extractUsageAndGeneratePackageReports(Map<String, ProjectDependency> dependenciesMap,
                                                                               Map<String, Set<String>> projPackagesAndClassMap,
                                                                               String localRepoPath, String projId) {
        thisProject.setId(projId);
        Map<String, Document> packageReports = new HashMap<>();
        try {
            // Load Jacoco XML report
            File xmlFile = new File(getJactReportPath() + "jacoco_report.xml");
            sessionInfo = extractXmlHeader(getJactReportPath() + "jacoco_report.xml");

            // Disable DTD validation
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Format and overwrite the XML file
            formatXml(xmlFile, doc, false);

            // Group coverage data by package name
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
                // Add to th
                fileNameToPackageMap.put(filename, packageName);
                writeXML(packageReport, getJactReportPath() + "jact_xml_package_reports/" + filename);
            }

            //System.out.println("Separate XML reports created for each package in the current directory.");
            readAndExtractPackageUsage(getJactReportPath() + "jact_xml_package_reports/", dependenciesMap, projPackagesAndClassMap, localRepoPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packageReports;
    }

    /**
     * Reads the individual package reports and
     * extracts the usage of each dependency or
     * project code.
     *
     * @param pathToReport
     * @param dependenciesMap
     * @param projPackagesAndClassMap
     * @param localRepoPath
     */
    private static void readAndExtractPackageUsage(String pathToReport,
                                                   Map<String, ProjectDependency> dependenciesMap,
                                                   Map<String, Set<String>> projPackagesAndClassMap,
                                                   String localRepoPath) {
        File reportDir = new File(pathToReport);
        // Check if the directory exists
        if (reportDir.exists() && reportDir.isDirectory()) {
            // List all files in the directory
            File[] files = reportDir.listFiles();
            if (files != null) {
                // Iterate through each file
                for (File file : files) {
                    String packageName = fileNameToPackageMap.get(file.getName()).replaceAll("/", ".");
                    if (projPackagesAndClassMap.containsKey(packageName)) {
                        extractCounterValues(getJactReportPath() + "jact_xml_package_reports/" + file.getName(),
                                thisProject, projectUsage, file.getName());
                    } else {
                        // Match the package to its dependency
                        ProjectDependency matchedDep = packageToDependency(packageName, dependenciesMap, localRepoPath);
                        if (matchedDep.getId() != null) {
                            extractCounterValues(getJactReportPath() + "jact_xml_package_reports/" + file.getName(),
                                    matchedDep, dependencyUsage, file.getName());
                        }else{
                            removeFile(getJactReportPath() + "jact_xml_package_reports/" + file.getName());
                        }
                    }
                }
            } else {
                System.out.println("No files found in the directory.");
            }
        } else {
            System.out.println("Directory does not exist or is not a directory.");
        }
        totalUsage.addAll(projectUsage);
        totalUsage.addAll(dependencyUsage);
    }

    /**
     * Writes the complete XML report from the individual
     * project XML reports. The report is separated by
     * Dependency/Project packages easily identifying
     * the source of packages where totals for the
     * all dependencies, project and the overall
     * total is written in each section.
     *
     * @param dependenciesMap
     * @param packageReports
     */
    private static void writeCompleteReport(Map<String, ProjectDependency> dependenciesMap, Map<String, Document> packageReports) {
        try {
            CommandExecutor.copyDtdFile("report.dtd", "./target/jact-report");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String depOpeningTag = "<group name=\"Dependencies\">";
        String projOpeningTag = "<group name=\"" + "Project Packages" + "\">";
        String groupClosingTag = "</group>";
        File finalReport = new File(FINALREPORTPATH);

        try (FileWriter writer = new FileWriter(finalReport)) {
            writer.write(xmlDtd);
            writer.write(xmlReportTag);
            writer.write(sessionInfo);
            writer.write(depOpeningTag);

            for (ProjectDependency pd : dependenciesMap.values()) {
                String openingTag = "<group name=\"" + pd.getId() + "\">";
                writer.write(openingTag.trim());

                writePackageReportsFromMap(pd, writer);

                writer.write(groupClosingTag);
            }

            // Write total dependency usage
            writer.write(dependencyUsage.totalUsageToXML());
            writer.write(groupClosingTag);

            // Write the project packages
            writer.write(projOpeningTag);

            writePackageReportsFromMap(thisProject, writer);

            writer.write(projectUsage.totalUsageToXML());
            writer.write(groupClosingTag);

            // Write overall total here
            writer.write(totalUsage.totalUsageToXML());

            writer.write("</report>");

        } catch (IOException e) {
            System.err.println("Error writing final report: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            String dtdPath = "./target/jact-report/report.dtd";

            // Set system property to specify the DTD location
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
            System.setProperty("org.xml.sax.driver", "com.sun.org.apache.xerces.internal.parsers.SAXParser");
            System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema", dtdPath);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setValidating(true);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // Set ErrorHandler
            dBuilder.setErrorHandler(new DefaultHandler());

            Document formattedDoc = dBuilder.parse(finalReport);
            formattedDoc.getDocumentElement().normalize();

            // Format and overwrite the XML file
            formatXml(finalReport, formattedDoc, true);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Final report has been written to: " + finalReport.getAbsolutePath());
    }


    /**
     * Writes the dependency packages
     * to the complete XML report.
     *
     * @param dependency
     * @param writer
     */
    private static void writePackageReportsFromMap(ProjectDependency dependency, FileWriter writer) {
        for (Map.Entry<String, DependencyUsage> entry : dependency.packageUsageMap.entrySet()) {
            File packageFile = new File(getJactReportPath() + "jact_xml_package_reports/" + entry.getKey());
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
                    if (line.trim().equals("</report>")) {
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
    }


    /**
     * Extracts the total values from the jacoco XML report.
     *
     * @param inputFilePath
     * @param matchedDep
     * @param usage
     * @param packageFileName
     */
    private static void extractCounterValues(String inputFilePath, ProjectDependency matchedDep, DependencyUsage usage, String packageFileName) {
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
                            matchedDep.packageUsageMap.put(packageFileName, packageUsage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        // No usage, such packages are not included in the html version.
        if (!matchedDep.packageUsageMap.containsKey(packageFileName)) {
            removeFile(getJactReportPath() + "jact_xml_package_reports/" + packageFileName);
        }
    }

    private static void processCounterValues(String type, long missed, long covered, ProjectDependency matchedDep, DependencyUsage packageUsage) {
        // Add the total here!
        switch (type) {
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

    /**
     * Creates the individual XML package reports
     * from the jacoco XML report.
     *
     * @param packageElement
     * @return
     * @throws Exception
     */
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
        formatXml(outputFile, formattedDoc, false);
    }

    /**
     * Formats the XML to correctly ident each line.
     *
     * @param file
     * @param doc
     * @param validateWithDTD
     * @throws Exception
     */
    private static void formatXml(File file, Document doc, boolean validateWithDTD) throws Exception {
        // Create transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set indentation properties
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // Indentation size

        // Set standalone="yes" in the XML declaration
        doc.setXmlStandalone(true);

        // Write the XML declaration with standalone="yes"
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String xmlString = writer.toString();

        // Add standalone="yes" to the XML declaration if not already present
        if (!xmlString.contains("standalone=\"yes\"")) {
            int index = xmlString.indexOf("?>");
            if (index != -1) {
                xmlString = xmlString.substring(0, index) + " standalone=\"yes\"" + xmlString.substring(index);
            }
        }

        // Write the modified XML to the file
        try (PrintWriter printWriter = new PrintWriter(file)) {
            printWriter.write(xmlString);
        }
    }
}



