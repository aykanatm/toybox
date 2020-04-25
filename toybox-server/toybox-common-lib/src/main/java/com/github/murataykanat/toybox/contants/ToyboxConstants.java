package com.github.murataykanat.toybox.contants;

public final class ToyboxConstants {
    private ToyboxConstants(){}

    public static final String JOB_SERVICE_LOAD_BALANCER_SERVICE_NAME = "toybox-job-loadbalancer";
    public static final String FOLDER_SERVICE_LOAD_BALANCER_SERVICE_NAME = "toybox-folder-loadbalancer";
    public static final String COMMON_OBJECTS_LOAD_BALANCER_SERVICE_NAME = "toybox-common-object-loadbalancer";
    public static final String NOTIFICATIONS_LOAD_BALANCER_SERVICE_NAME = "toybox-notification-loadbalancer";
    public static final String SHARE_LOAD_BALANCER_SERVICE_NAME = "toybox-share-loadbalancer";

    public static final String ASSET_SERVICE_NAME = "toybox-asset-service";
    public static final String COMMON_OBJECT_SERVICE_NAME = "toybox-common-object-service";
    public static final String FOLDER_SERVICE_NAME = "toybox-folder-service";
    public static final String JOB_SERVICE_NAME = "toybox-job-service";
    public static final String NOTIFICATION_SERVICE_NAME = "toybox-notification-service";
    public static final String RENDITION_SERVICE_NAME = "toybox-rendition-service";
    public static final String SHARE_SERVICE_NAME = "toybox-share-service";
    public static final String USER_SERVICE_NAME = "toybox-user-service";

    public static final int FOLDER_ID_LENGTH = 40;

    public static final int ASSET_ID_LENGTH = 40;

    public static final String JOB_IMPORT_NAME = "toybox-import";
    public static final String JOB_PACKAGING_NAME = "toybox-packaging";
    public static final String JOB_DELETE_NAME = "toybox-delete";

    public static final String STEP_IMPORT_GENERATE_ASSETS = "toybox-import-generate-asset";
    public static final String STEP_IMPORT_GENERATE_THUMBNAILS = "toybox-import-generate-thumbnail";
    public static final String STEP_IMPORT_GENERATE_PREVIEWS = "toybox-import-generate-preview";
    public static final String STEP_IMPORT_DELETE_TEMP_FILES = "toybox-import-delete-temp-files";
    public static final String STEP_PACKAGING_GENERATE_ARCHIVE = "toybox-packaging-generate-archive";
    public static final String STEP_DELETE_SET_ASSETS_AS_DELETED = "toybox-delete-set-assets-as-deleted";

    public static final String JOB_PARAM_UPLOADED_FILE = "toybox-import-param-uploaded-file";
    public static final String JOB_PARAM_USERNAME = "toybox-job-param-username";
    public static final String JOB_PARAM_SYSTEM_MILLIS = "toybox-job-param-system-millis";
    public static final String JOB_PARAM_PACKAGING_FILE = "toybox-job-param-packaging-file";
    public static final String JOB_PARAM_PACKAGING_FOLDER = "toybox-job-param-packaging-folder";
    public static final String JOB_PARAM_DELETE_ASSET_ID = "toybox-job-param-delete-asset-id";
    public static final String JOB_PARAM_CONTAINER_ID = "toybox-job-param-container-id";

    public static final String IMAGE_MIME_TYPE_PREFIX = "image";
    public static final String VIDEO_MIME_TYPE_PREFIX = "video";
    public static final String AUIDO_MIME_TYPE_PREFIX = "audio";

    public static final String IMAGE_MIME_TYPE_GIF = "image/gif";
    public static final String IMAGE_MIME_TYPE_EPS = "application/postscript";
    public static final String FILE_MIME_TYPE_PDF = "application/pdf";
    public static final String IMAGE_MIME_TYPE_PHOTOSHOP = "image/vnd.adobe.photoshop";
    public static final String FILE_MIME_TYPE_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String FILE_MIME_TYPE_PPT = "application/vnd.ms-powerpoint";
    public static final String FILE_MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String FILE_MIME_TYPE_DOC = "application/msword";
    public static final String FILE_MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String FILE_MIME_TYPE_XLS = "application/vnd.ms-excel";

    public static final String FFMPEG = "FFMpeg";
    public static final String IMAGEMAGICK = "ImageMagick";
    public static final String GIFSICLE = "Gifsicle";

    public static final String TOYBOX_NOTIFICATION_EXCHANGE = "toybox-notification-exchange";

    public static final String SHARE_PERMISSION_DOWNLOAD = "Download";
    public static final String SHARE_PERMISSION_COPY = "Copy";
    public static final String SHARE_PERMISSION_EDIT = "Edit";
    public static final String SHARE_PERMISSION_SHARE = "Share";

    public static final String LOOKUP_YES = "Y";
    public static final String LOOKUP_NO = "N";

    public static final String LOOKUP_TODAY = "Today";
    public static final String LOOKUP_NEXT_7_DAYS = "Next 7 days";
    public static final String LOOKUP_PAST_7_DAYS = "Past 7 days";
    public static final String LOOKUP_NEXT_30_DAYS = "Next 30 days";
    public static final String LOOKUP_PAST_30_DAYS = "Past 30 days";
    public static final String LOOKUP_PAST_30_PLUS_DAYS = "Past 30+ days";
    public static final String LOOKUP_NEXT_30_PLUS_DAYS = "Next 30+ days";

    public static final String FRIENDLY_DATE_FORMAT = "MM/dd/yyyy hh:mm:ss";
    public static final String FAR_FUTURE_DATE_TIME = "12/31/9999 23:59:59";

    public static final String SEARCH_OPERATOR_AND = "AND";
    public static final String SEARCH_OPERATOR_OR = "OR";
    public static final String SEARCH_OPERATOR_OR_IN = "OR_IN";
    public static final String SEARCH_OPERATOR_AND_IN = "AND_IN";

    public static final String SEARCH_CONDITION_EQUALS = "EQUALS";
    public static final String SEARCH_CONDITION_IS_NULL = "IS_NULL";
    public static final String SEARCH_CONDITION_IS_NOT_NULL = "IS_NOT_NULL";
    public static final String SEARCH_CONDITION_CONTAINS = "CONTAINS";
    public static final String SEARCH_CONDITION_STARTS_WITH = "STARTS_WITH";
    public static final String SEARCH_CONDITION_ENDS_WITH = "ENDS_WITH";
    public static final String SEARCH_CONDITION_IS_GREATER_THAN = "IS_GREATER_THAN";
    public static final String SEARCH_CONDITION_IS_LESS_THAN = "IS_LESS_THAN";
    public static final String SEARCH_CONDITION_BETWEEN = "BETWEEN";

    public static final String SEARCH_CONDITION_DATA_TYPE_STRING = "STRING";
    public static final String SEARCH_CONDITION_DATA_TYPE_INTEGER = "INTEGER";
    public static final String SEARCH_CONDITION_DATA_TYPE_DATE = "DATE";

    public static final String SORT_TYPE_DESCENDING = "DESC";
    public static final String SORT_TYPE_ASCENDING = "ASC";
}
