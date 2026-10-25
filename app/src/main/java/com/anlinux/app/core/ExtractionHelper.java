package com.anlinux.app.core;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtractionHelper {

    public interface ProgressListener {
        void onProgress(String currentFile, int count);
    }

    /**
     * Extract tar.gz, tar.xz, tar.bz2, or .tar rootfs archive
     */
    public static void extractArchive(File archiveFile, File destinationDir, ProgressListener listener) throws Exception {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        String fileName = archiveFile.getName().toLowerCase();
        InputStream fileInputStream = new BufferedInputStream(new FileInputStream(archiveFile));
        InputStream compressorStream;

        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            compressorStream = new GzipCompressorInputStream(fileInputStream);
        } else if (fileName.endsWith(".tar.xz") || fileName.endsWith(".txz")) {
            compressorStream = new XZCompressorInputStream(fileInputStream);
        } else if (fileName.endsWith(".tar.bz2") || fileName.endsWith(".tbz2")) {
            compressorStream = new BZip2CompressorInputStream(fileInputStream);
        } else {
            compressorStream = fileInputStream;
        }

        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(compressorStream)) {
            TarArchiveEntry entry;
            byte[] buffer = new byte[32768];
            int count = 0;

            while ((entry = tarInput.getNextEntry()) != null) {
                count++;
                if (listener != null && count % 50 == 0) {
                    listener.onProgress(entry.getName(), count);
                }

                File outputFile = new File(destinationDir, entry.getName());

                // Prevent zip-slip vulnerability
                String canonicalDest = destinationDir.getCanonicalPath();
                String canonicalOut = outputFile.getCanonicalPath();
                if (!canonicalOut.startsWith(canonicalDest)) {
                    continue;
                }

                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else if (entry.isSymbolicLink()) {
                    try {
                        Path link = Paths.get(outputFile.getAbsolutePath());
                        Path target = Paths.get(entry.getLinkName());
                        if (Files.exists(link) || Files.isSymbolicLink(link)) {
                            Files.delete(link);
                        }
                        // Create parent if needed
                        File parent = outputFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        Files.createSymbolicLink(link, target);
                    } catch (Exception ignored) {
                        // Symbolic links may fail on FAT32/SDCard, fallback gracefully
                    }
                } else {
                    File parent = outputFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (OutputStream out = new FileOutputStream(outputFile)) {
                        int read;
                        while ((read = tarInput.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }

                    // Restore executable permissions if applicable
                    int mode = entry.getMode();
                    if ((mode & 0111) != 0) {
                        outputFile.setExecutable(true, false);
                    }
                    if ((mode & 0444) != 0) {
                        outputFile.setReadable(true, false);
                    }
                }
            }
        }
    }
}
