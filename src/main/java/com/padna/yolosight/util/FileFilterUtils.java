package com.padna.yolosight.util;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * File filter for supported image formats.
 */
public final class FileFilterUtils extends FileFilter {

    private final Set<String> extensions;

    public FileFilterUtils(String... extensions) {
        this.extensions = new HashSet<>();
        for (String ext : extensions) {
            this.extensions.add(ext.toLowerCase());
        }
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;
        String name = f.getName().toLowerCase();
        for (String ext : extensions) {
            if (name.endsWith("." + ext)) return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Image files (" + String.join(", ", extensions) + ")";
    }

    /** Convenience: check if a filename's extension is in the supported set. */
    public static boolean isSupported(String filename, String... extensions) {
        String lower = filename.toLowerCase();
        return Arrays.stream(extensions).anyMatch(ext -> lower.endsWith("." + ext));
    }
}
