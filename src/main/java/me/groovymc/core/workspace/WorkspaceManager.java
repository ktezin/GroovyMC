package me.groovymc.core.workspace;

import me.groovymc.view.MessageView;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkspaceManager {
    private final JavaPlugin plugin;
    private final File projectRoot;

    public WorkspaceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.projectRoot = plugin.getDataFolder();
        setupWorkspace();
    }

    public void setupWorkspace() {
        if (!projectRoot.exists()) projectRoot.mkdirs();

        try {
            updatePomFile();
        } catch (Exception e) {
            MessageView.logError("Failed to update developer workspace!", e);
        }
    }

    private void updatePomFile() throws IOException {
        File pomFile = new File(projectRoot, "pom.xml");

        String currentJarName = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getName();

        String expectedPath = "${project.basedir}/../" + currentJarName;

        if (!pomFile.exists()) {
            createInitialPom(pomFile, currentJarName);
            return;
        }

        String content = Files.readString(pomFile.toPath());
        Pattern pattern = Pattern.compile("(<artifactId>groovymc-api<\\/artifactId>[\\s\\S]*?<systemPath>)(.*?)(<\\/systemPath>)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String currentPathInFile = matcher.group(2).trim();

            if (!currentPathInFile.equals(expectedPath)) {
                String newContent = matcher.replaceFirst("$1" + expectedPath + "$3");
                Files.writeString(pomFile.toPath(), newContent, StandardOpenOption.TRUNCATE_EXISTING);
                MessageView.log("Workspace: API path in pom.xml updated -> " + currentJarName);
            }
        }
    }

    private void createInitialPom(File pomFile, String currentJarName) throws IOException {
        String userHelp = """
                """;

        String content = """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>me.groovymc</groupId>
                    <artifactId>groovymc-workspace</artifactId>
                    <version>1.0.0</version>
                
                    <properties>
                        <maven.compiler.source>21</maven.compiler.source>
                        <maven.compiler.target>21</maven.compiler.target>
                    </properties>
                
                    <dependencies>
                        <dependency>
                            <groupId>io.papermc.paper</groupId>
                            <artifactId>paper-api</artifactId>
                            <version>1.21.1-R0.1-SNAPSHOT</version>
                            <scope>provided</scope>
                        </dependency>
                
                        <dependency>
                            <groupId>org.codehaus.groovy</groupId>
                            <artifactId>groovy</artifactId>
                            <version>3.0.25</version>
                        </dependency>
                        <dependency>
                            <groupId>org.codehaus.groovy</groupId>
                            <artifactId>groovy-sql</artifactId>
                            <version>3.0.25</version>
                        </dependency>
                
                        <dependency>
                            <groupId>me.groovymc</groupId>
                            <artifactId>groovymc-api</artifactId>
                            <version>1.0</version>
                            <scope>system</scope>
                            <systemPath>${project.basedir}/../%s</systemPath>
                        </dependency>
                
                        %s
                    </dependencies>
                
                    <repositories>
                        <repository>
                            <id>papermc</id>
                            <url>https://repo.papermc.io/repository/maven-public/</url>
                        </repository>
                    </repositories>
                
                    <build>
                        <sourceDirectory>modules</sourceDirectory>
                    </build>
                </project>
                """.formatted(currentJarName, userHelp);

        Files.writeString(pomFile.toPath(), content, StandardOpenOption.CREATE);
    }
}