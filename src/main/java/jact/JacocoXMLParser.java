package jact;

import org.apache.maven.model.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;

import static jact.PackageToDependencyResolver.packageToDepPaths;

public class JacocoXMLParser {

    public static final String REPORTPATH = "./target/jact-report/";
    private static final String projId = HtmlReportMojo.getProjectGroupId() + ":" +
            HtmlReportMojo.getProjectArtifactId() + ":" + HtmlReportMojo.getProjectVersion();
    private static ProjectDependency thisProject = new ProjectDependency();


    public static void main(String[] args){
        List<ProjectDependency> pdList = new ArrayList<>();
        groupPackageByDep(pdList);
    }

    public static void groupPackageByDep(List<ProjectDependency> dependencies) {
        thisProject.setId(projId);

        // If the package-name contains artifactid + groupid then put that in its own report
        //List<String> depWords = new ArrayList<>();


        try {
            // Load Jacoco XML report
            File xmlFile = new File("jacoco_report.xml"); // Currently manually generated.
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); // Disable DTD validation
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

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
                writeXML(packageReport, "./xml_reports/" + filename);
                //System.out.println("filename: " + filename);
            }

            System.out.println("Separate XML reports created for each package in the current directory.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        File reportDir = new File("./xml_reports");

        // Check if the directory exists
        if (reportDir.exists() && reportDir.isDirectory()) {
            // List all files in the directory
            File[] files = reportDir.listFiles();
            if (files != null) {
                // Iterate through each file
                for (File file : files) {
                    // Extract filename without extension
                    String filename = file.getName().replaceAll("\\.xml$", "");
                    System.out.println(filename);
                    // Call your function with the filename
                    ProjectDependency matchedDep = packageToDepPaths(filename, dependencies);
                    System.out.println(matchedDep.getId());
                }
            } else {
                System.out.println("No files found in the directory.");
            }
        } else {
            System.out.println("Directory does not exist or is not a directory.");
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
    }
}



