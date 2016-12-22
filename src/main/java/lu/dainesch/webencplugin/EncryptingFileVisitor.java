package lu.dainesch.webencplugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.maven.plugin.logging.Log;

public class EncryptingFileVisitor extends SimpleFileVisitor<Path> {

    private final EncryptionHelper helper;
    private final String password;
    private final Path basePath;
    private final Path outPath;
    private final Log log;
    private boolean parseHtml;

    public EncryptingFileVisitor( EncryptionHelper helper, String password, Path basePath, Path outPath, Log log) {
        this.password = password;
        this.basePath = basePath;
        this.outPath = outPath;
        this.log = log;
        this.helper = helper;
        this.parseHtml = true;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        log.info("Entering directory " + dir.relativize(basePath));

        Path target = outPath.resolve(dir.relativize(basePath));
        if (!Files.isDirectory(target)) {
            Files.createDirectories(target);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!Files.isDirectory(file) && Files.isReadable(file)) {
            Path target = outPath.resolve(basePath.relativize(file));
            
            String fileName = file.getFileName().toString().toLowerCase();
            
            
            log.info("Encrypting " + target.getFileName().toString());
            helper.encrypt(password, file, target);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.error("Error processing file " + file.relativize(basePath));
        return FileVisitResult.TERMINATE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        log.info("Exiting directory " + dir.relativize(basePath));
        return FileVisitResult.CONTINUE;
    }

    public boolean isParseHtml() {
        return parseHtml;
    }

    public void setParseHtml(boolean parseHtml) {
        this.parseHtml = parseHtml;
    }
    
    

}
