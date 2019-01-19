package com.github.murataykanat.toybox.models.asset;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ToyboxAsset {
    private Log _logger = LogFactory.getLog(ToyboxAsset.class);

    private String id;
    private String name;
    private String extension;
    private String type;
    private String path;
    private String previewPath;
    private String thumbnailPath;
    @FacetColumnName(value = "Imported By")
    private String importedByUsername;
    @FacetColumnName(value = "Import Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date importDate;

    public ToyboxAsset(Asset asset){
        this.id = asset.getId();
        this.name = asset.getName();
        this.extension = asset.getExtension();
        this.type = asset.getType();
        this.path = asset.getPath();
        this.previewPath = asset.getPreviewPath();
        this.thumbnailPath = asset.getThumbnailPath();
        this.importedByUsername = asset.getImportedByUsername();
        this.importDate = asset.getImportDate();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @FacetColumnName(value = "Asset Type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    @FacetColumnName(value = "Imported By")
    public String getImportedByUsername() {
        return importedByUsername;
    }

    public void setImportedByUsername(String importedByUsername) {
        this.importedByUsername = importedByUsername;
    }

    @FacetColumnName(value = "Import Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    public Date getImportDate() {
        return importDate;
    }

    public void setImportDate(Date importDate) {
        this.importDate = importDate;
    }

    public boolean hasFacetValue(List<SearchRequestFacet> searchRequestFacetList) {
        boolean result = true;

        try{
            for(SearchRequestFacet searchRequestFacet: searchRequestFacetList){
                boolean hasFacet = false;
                for(Field field: this.getClass().getDeclaredFields()){
                    if(field.isAnnotationPresent(FacetColumnName.class)){
                        String fieldName = field.getAnnotation(FacetColumnName.class).value();
                        if(searchRequestFacet.getFieldName().equalsIgnoreCase(fieldName)){
                            if(fieldName.equalsIgnoreCase("Import Date")){
                                if(field.isAnnotationPresent(FacetDefaultLookup.class)){
                                    String fieldValue = searchRequestFacet.getFieldValue();

                                    Calendar cal;
                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    Date today = cal.getTime();

                                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                                    _logger.debug("Today: " + formatter.format(today));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, 1);
                                    Date tomorrow = cal.getTime();
                                    _logger.debug("Tomorrow: " + formatter.format(tomorrow));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, -7);
                                    Date sevenDaysAgo = cal.getTime();
                                    _logger.debug("Past 7 days: " + formatter.format(sevenDaysAgo));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, -30);
                                    Date thirtyDaysAgo = cal.getTime();
                                    _logger.debug("Past 30 days: " + formatter.format(sevenDaysAgo));

                                    Date value = null;
                                    if(fieldName.equalsIgnoreCase("Import Date")){
                                        value = this.getImportDate();
                                    }
                                    else{
                                        throw new Exception("Field name " + fieldName + " is not recognized.");
                                    }

                                    if(fieldValue.equalsIgnoreCase("Today")){
                                        if((value.after(today) || value.equals(today)) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else if(fieldValue.equalsIgnoreCase("Past 7 days")){
                                        if(value.after(sevenDaysAgo) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else if(fieldValue.equalsIgnoreCase("Past 30 days")){
                                        if(value.after(thirtyDaysAgo) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else{
                                        throw new Exception("Field value " + fieldValue + " is not recognized.");
                                    }
                                }
                            }
                            else{
                                if(searchRequestFacet.getFieldValue().equalsIgnoreCase((String) field.get(this))){
                                    hasFacet = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                result = result && hasFacet;
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while determining if the job has the facet value. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
        }

        return result;
    }
}
