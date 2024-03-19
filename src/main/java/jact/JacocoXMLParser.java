package jact;

import jact.plugin.XmlReportMojo;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

import static jact.PackageToDependencyResolver.packageToDepPaths;

public class JacocoXMLParser {

    public static final String REPORTPATH = "./target/jact-report/";
    private static ProjectDependency thisProject = new ProjectDependency();

    public static void groupPackageByDep(List<ProjectDependency> dependencies,
                                         Map<String, Set<String>> projPackagesAndClassMap,
                                         String localRepoPath, String projId){
        thisProject.setId(projId);

        // If the package-name contains artifactid + groupid then put that in its own report
        //List<String> depWords = new ArrayList<>();


        try {
            // Load Jacoco XML report
            File xmlFile = new File(REPORTPATH + "jacoco_report.xml"); // Currently manually generated.

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

            System.out.println("Separate XML reports created for each package in the current directory.");
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

    }

//    public static void extractAndAddPackageTotal(String inputFilePath, ProjectDependency matchedDep, String packageName) throws IOException {
//        try {
//            // Read the HTML file
//            File inputFile = new File(inputFilePath);
//            org.jsoup.nodes.Document doc = Jsoup.parse(inputFile, "UTF-8");
//
//            String formattedHtml = doc.outerHtml();
//
//            // Write the formatted HTML back to the original file, overwriting its previous content
//            org.apache.commons.io.FileUtils.writeStringToFile(inputFile, formattedHtml, "UTF-8");
//        } catch (IOException e) {
//            System.err.println("Error: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
//
//            String line;
//
//            // Flag to indicate if we are inside the <tbody> tag
//            boolean insideTbody = false;
//
//            // Iterate through the input HTML file
//            while ((line = br.readLine()) != null) {
//                // Check if we are inside the <tbody> tag
//                if (line.contains("<tfoot>")) {
//                    insideTbody = true;
//                    line = br.readLine();
//                }
//                // Check if we are inside a <tr> element
//                if (insideTbody && line.contains("<tr>")) {
//                    line = br.readLine();
//                    if (line != null) {
//                        int entryIndex = 1;
//                        DependencyUsage packageUsage = new DependencyUsage();
//                        while ((line = br.readLine()) != null) {
//                            //trContent.append(line).append("\n");
//                            if (line.contains("</tr>")) {
//                                break; // Stop processing when encountering </tr>
//                            }
//                            if (entryIndex > 0 && matchedDep.getId() != null) {
//                                extractUsage(line, entryIndex, matchedDep, packageUsage);
//                            }
//                            entryIndex++;
//                        }
//                        matchedDep.packageUsageMap.put(packageName, packageUsage);
//                        break;
//                    }
//                }
//            }
//        }
//    }



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

        System.out.println("Formatted XML has been written to: " + file.getAbsolutePath());
    }



}



