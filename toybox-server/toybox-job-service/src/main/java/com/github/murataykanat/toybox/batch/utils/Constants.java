package com.github.murataykanat.toybox.batch.utils;

public final class Constants {
    public static final String JOB_IMPORT_NAME = "toybox-import";
    public static final String STEP_IMPORT_GENERATE_THUMBNAILS = "toybox-import-generate-thumbnail";
    public static final String STEP_IMPORT_GENERATE_PREVIEWS = "toybox-import-generate-preview";
    public static final String STEP_IMPORT_GENERATE_ASSETS = "toybox-import-generate-asset";
    public static final String JOB_PARAM_UPLOADED_FILES = "toybox-import-param-uploaded-files";
    public static final String JOB_PARAM_SYSTEM_MILLIS = "toybox-job-param-system-millis";
    public static final int ASSET_ID_LENGTH = 40;
    // TODO: Move this to configuration
    public static final int IMAGEMAGICK_TIMEOUT = 60000;
}
