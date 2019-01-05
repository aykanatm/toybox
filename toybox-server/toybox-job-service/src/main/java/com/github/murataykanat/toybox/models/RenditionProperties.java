package com.github.murataykanat.toybox.models;

import java.io.File;

public class RenditionProperties {
    private File outputFile;
    private String renditionSettings;

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public String getRenditionSettings() {
        return renditionSettings;
    }

    public void setRenditionSettings(String renditionSettings) {
        this.renditionSettings = renditionSettings;
    }
}
