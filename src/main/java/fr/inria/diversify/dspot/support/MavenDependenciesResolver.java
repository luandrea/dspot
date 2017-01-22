package fr.inria.diversify.dspot.support;

import fr.inria.diversify.buildSystem.maven.MavenBuilder;
import fr.inria.diversify.runner.InputConfiguration;
import fr.inria.diversify.runner.InputProgram;
import fr.inria.diversify.util.Log;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 1/17/17
 */
public class MavenDependenciesResolver {

    private static String pathToLocalMavenRepository = null;

    public static URL[] resolveDependencies(InputConfiguration configuration, InputProgram program) {
        if (pathToLocalMavenRepository == null)
            pathToLocalMavenRepository = configuration.getProperty("maven.localRepository", System.getProperty("user.home") + "/.m2/repository");
        runMavenGoals(configuration, program);
        Set<URL> classpath = buildUrls(program.getProgramDir());
        return classpath.toArray(new URL[classpath.size()]);
    }

    private static Set<URL> buildUrls(String path) {
        File classPathFile = new File(path + "/.classpath");
        Set<URL> classpath = new HashSet<>();
        if (classPathFile.exists()) {
            classpath.addAll(resolveDependenciesInFile(classPathFile));
        }
        MavenProject project = getMavenProject(path);
        project.getModules().forEach(module ->
                classpath.addAll(buildUrls(path + "/" + module))
        );
        return classpath;
    }

    private static Set<URL> resolveDependenciesInFile(File classPathFile) {
        Set<URL> urls = new HashSet<>();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(classPathFile);
            Node currentNode = doc.getDocumentElement().getFirstChild();
            do {
                String url = getUrlsOfNodes(currentNode);
                if (url != null) {
                    urls.add(new URL(url));
                }
                currentNode = currentNode.getNextSibling();
            } while (currentNode != null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return urls;
    }

    private static String getUrlsOfNodes(Node element) {
        try {
            NamedNodeMap attributes = element.getAttributes();
            if ("var".equals(attributes.getNamedItem("kind").getNodeValue())) {
                return attributes.getNamedItem("path").getNodeValue().replace("M2_REPO", "file:" + pathToLocalMavenRepository);
            } else {
                return null;
            }
        } catch (Exception e) {
            //skipping this element;
            return null;
        }
    }

    private static MavenProject getMavenProject(String baseDirectory) {
        FileReader reader = null;
        try {
            MavenProject mavenProject;
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            String pom = baseDirectory + "/pom.xml";
            File pomFile = new File(pom);
            Log.info("resolveURL dependencies of {}", pomFile);
            //Removed null and file exists protections that mask errors
            reader = new FileReader(pomFile);
            Model model = mavenReader.read(reader);
            model.setPomFile(pomFile);
            mavenProject = new MavenProject(model);
            if (model.getParent() != null) {
                MavenProject parent = new MavenProject();
                parent.setGroupId(model.getParent().getGroupId());
                parent.setArtifactId(model.getParent().getArtifactId());
                parent.setVersion(model.getParent().getVersion());
                mavenProject.setParent(parent);
            }
            reader.close();
            return mavenProject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runMavenGoals(InputConfiguration configuration, InputProgram program) {
        try {
            String mavenHome = configuration.getProperty("maven.home", null);
            MavenBuilder builder = new MavenBuilder(program.getProgramDir());
            builder.setBuilderPath(mavenHome);
            String[] phases = new String[]{"eclipse:eclipse"};
            builder.runGoals(phases, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}