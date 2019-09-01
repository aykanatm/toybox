package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SelectionUtils {
    private static final Log _logger = LogFactory.getLog(SelectionUtils.class);

    @LogEntryExitExecutionTime
    public boolean isSelectionContextValid(SelectionContext selectionContext){
        List<Asset> selectedAssets = selectionContext.getSelectedAssets();
        List<Container> selectedContainers = selectionContext.getSelectedContainers();

        if(selectedAssets != null && selectedContainers != null){
            return true;
        }

        return false;
    }
}
