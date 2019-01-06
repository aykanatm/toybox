package com.github.murataykanat.toybox.batch.utils;

public final class Constants {
    private Constants(){}
    public static final String JOB_IMPORT_NAME = "toybox-import";

    public static final String STEP_IMPORT_GENERATE_ASSETS = "toybox-import-generate-asset";
    public static final String STEP_IMPORT_GENERATE_THUMBNAILS = "toybox-import-generate-thumbnail";
    public static final String STEP_IMPORT_GENERATE_PREVIEWS = "toybox-import-generate-preview";
    public static final String STEP_IMPORT_DELETE_TEMP_FILES = "toybox-import-delete-temp-files";

    public static final String JOB_PARAM_UPLOADED_FILE = "toybox-import-param-uploaded-file";
    public static final String JOB_PARAM_USERNAME = "toybox-job-param-username";
    public static final String JOB_PARAM_SYSTEM_MILLIS = "toybox-job-param-system-millis";

    public static final int ASSET_ID_LENGTH = 40;

    public static final String IMAGE_MIME_TYPE_PREFIX = "image";
    public static final String VIDEO_MIME_TYPE_PREFIX = "video";
    public static final String AUIDO_MIME_TYPE_PREFIX = "audio";

    public static final String IMAGE_MIME_TYPE_GIF = "image/gif";
    public static final String IMAGE_MIME_TYPE_EPS = "application/postscript";
    public static final String FILE_MIME_TYPE_PDF = "application/pdf";
    public static final String IMAGE_MIME_TYPE_PHOTOSHOP = "image/vnd.adobe.photoshop";
}
