package jonas.maven.master;

import com.google.gson.JsonObject;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Array;
import java.util.*;

public class JacocoXMLParser {

    public static void groupPackageByDep(List<Dependency> dependencies) {

        // If the package-name contains artifactid + groupid then put that in its own report
        //List<String> depWords = new ArrayList<>();



        List<Set<String>> setOfAllDeps = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            List<String> depWords = new ArrayList<>();


            // Extract the words in the group id
            String depGroupId = dependency.getGroupId();
            String[] words = depGroupId.split("[.-]");
            depWords.addAll(Arrays.asList(words));

            // Extract the words in the artifact id
            String depArtifactId = dependency.getArtifactId();
            words = depArtifactId.split("[.-]");
            depWords.addAll(Arrays.asList(words));

            // Convert it to a set here
            Set<String> depWordsSet = new HashSet<>(depWords);
            setOfAllDeps.add(depWordsSet);
        }

        for(Set s : setOfAllDeps){
            System.out.println(s.toString());
        }


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
                String filename = packageName.replace("/", "-") + "_report.xml"; // Use package name for filename
                writeXML(packageReport, filename);
                //System.out.println("filename: " + filename);
            }

            System.out.println("Separate XML reports created for each package in the current directory.");
        } catch (Exception e) {
            e.printStackTrace();
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

    private static void writeXML(Document doc, String filename) throws Exception {
        javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
        javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
        javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(new File(filename));
        transformer.transform(source, result);
    }
}



