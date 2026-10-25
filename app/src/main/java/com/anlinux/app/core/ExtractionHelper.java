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
     * Extract rootfs archive with automatic format detection via magic bytes.
     * Supports .tar.xz, .tar.gz, .tar.bz2, and uncompressed .tar without relying on file extension.
     */
    public static void extractArchive(File archiveFile, File destinationDir, ProgressListener listener) throws Exception {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        if (!archiveFile.exists() || archiveFile.length() < 512) {
            throw new Exception("Berkas arsip tidak ditemukan atau terlalu kecil (" + archiveFile.length() + " bytes).");
        }

        // Read magic header bytes to determine compression format accurately
        byte[] magic = new byte[8];
        try (InputStream testIn = new FileInputStream(archiveFile)) {
            int read = testIn.read(magic);
            if (read < 2) {
                throw new Exception("Berkas arsip tidak dapat dibaca.");
            }
        }

        // Check if server returned an HTML error or text page instead of an archive
        if (magic[0] == '<' || (magic[0] == '{' && magic[1] == '"')) {
            byte[] errBuf = new byte[512];
            try (InputStream testIn = new FileInputStream(archiveFile)) {
                int read = testIn.read(errBuf);
                String errStr = new String(errBuf, 0, Math.max(0, read));
                throw new Exception("Server mengembalikan halaman web / teks error, bukan berkas sistem: " + errStr.trim());
            }
        }

        InputStream fileInputStream = new BufferedInputStream(new FileInputStream(archiveFile));
        InputStream compressorStream;

        // GZIP: 0x1F, 0x8B
        if (magic[0] == 0x1F && (magic[1] & 0xFF) == 0x8B) {
            compressorStream = new GzipCompressorInputStream(fileInputStream);
        }
        // XZ: 0xFD, '7', 'z', 'X', 'Z', 0x00
        else if ((magic[0] & 0xFF) == 0xFD && magic[1] == 0x37 && magic[2] == 0x7A && magic[3] == 0x58 && magic[4] == 0x5A && magic[5] == 0x00) {
            compressorStream = new XZCompressorInputStream(fileInputStream);
        }
        // BZIP2: 'B', 'Z', 'h'
        else if (magic[0] == 'B' && magic[1] == 'Z' && magic[2] == 'h') {
            compressorStream = new BZip2CompressorInputStream(fileInputStream);
        }
        // Fallback: Check file extension or treat as uncompressed tar
        else {
            String fileName = archiveFile.getName().toLowerCase();
            if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                compressorStream = new GzipCompressorInputStream(fileInputStream);
            } else if (fileName.endsWith(".tar.xz") || fileName.endsWith(".txz")) {
                compressorStream = new XZCompressorInputStream(fileInputStream);
            } else if (fileName.endsWith(".tar.bz2") || fileName.endsWith(".tbz2")) {
                compressorStream = new BZip2CompressorInputStream(fileInputStream);
            } else {
                compressorStream = fileInputStream;
            }
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
                        File parent = outputFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        Files.createSymbolicLink(link, target);
                    } catch (Exception ignored) {}
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

                    // Restore executable / readable permissions
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
