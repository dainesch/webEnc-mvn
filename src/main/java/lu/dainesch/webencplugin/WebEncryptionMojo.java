package lu.dainesch.webencplugin;

import java.io.BufferedWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

@Mojo(name = "webEnc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
public class WebEncryptionMojo extends AbstractMojo {

    // encryption
    @Parameter(required = true)
    private String password;
    @Parameter(defaultValue = PluginConstants.ITERATIONS, required = true)
    private Integer iterations;
    @Parameter(defaultValue = PluginConstants.KEYSIZE, required = true)
    private Integer keySize;
    // path and files
    @Parameter(required = true)
    private File inputDirectory;
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;
    @Parameter(defaultValue = "${project.build.directory}/js", required = true)
    private File scriptDirectory;

    @Parameter(defaultValue = "true", required = true)
    private Boolean parseHtml;

    @Override
    public void execute() throws MojoExecutionException {

        if (keySize != 128 && keySize != 256) {
            throw new MojoExecutionException("keySize must be 128 or 256 bits");
        }

        EncryptionHelper helper = new EncryptionHelper(iterations, keySize);

        EncryptingFileVisitor visitor = new EncryptingFileVisitor(
                helper,
                password,
                inputDirectory.getAbsoluteFile().toPath(),
                outputDirectory.getAbsoluteFile().toPath(),
                getLog());
        visitor.setParseHtml(parseHtml);

        try {
            Files.walkFileTree(inputDirectory.getAbsoluteFile().toPath(), visitor);

            createConfigScript(helper);
            saveJSScript();

        } catch (IOException ex) {
            getLog().error("Error encrypting, aborting", ex);
            throw new MojoExecutionException("Error while encrypting", ex);
        }

    }

    private void createConfigScript(EncryptionHelper helper) throws IOException {

        byte[] testData = helper.getRandomArray(256 + 80);
        String hash = helper.hashBytes(testData);

        testData = helper.encrypt(password, testData);

        StringBuilder b = new StringBuilder();
        b.append("var WebEnc = WebEnc || {};\n\n");
        b.append("WebEnc.config = {\n");
        b.append("\tkeySize: ").append(keySize).append(",\n");
        b.append("\titerations: ").append(iterations).append(",\n");
        b.append("\ttestHash: '").append(hash).append("',\n");
        b.append("\ttestData: '").append(Base64.getEncoder().encodeToString(testData)).append("'\n");
        b.append("};");

        Files.createDirectories(scriptDirectory.toPath());

        Path configFile = scriptDirectory.toPath().resolve(PluginConstants.FILE_CONF);
        Files.deleteIfExists(configFile);
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(configFile, StandardOpenOption.CREATE), StandardCharsets.UTF_8.toString()))) {
            w.write(b.toString());
        }

    }

    private void saveJSScript() throws IOException {
        Files.createDirectories(scriptDirectory.toPath());
        Path scriptFile = scriptDirectory.toPath().resolve(PluginConstants.FILE_MAIN);
        Files.deleteIfExists(scriptFile);

        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(PluginConstants.FILE_MAIN);
                OutputStream out = Files.newOutputStream(scriptFile, StandardOpenOption.CREATE)) {
            byte[] buff = new byte[PluginConstants.BUFFSIZE];
            int read;
            while ((read = in.read(buff)) != -1) {
                out.write(buff, 0, read);
            }
        }

    }
}
