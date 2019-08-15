package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;

import java.util.List;

public class SelectionUtils {
    private static SelectionUtils selectionUtils;

    private SelectionUtils(){}

    public static SelectionUtils getInstance(){
        if(selectionUtils != null){
            return selectionUtils;
        }

        selectionUtils = new SelectionUtils();
        return selectionUtils;
    }

    public boolean isSelectionContextValid(SelectionContext selectionContext){
        List<Asset> selectedAssets = selectionContext.getSelectedAssets();
        List<Container> selectedContainers = selectionContext.getSelectedContainers();

        if(selectedAssets != null && selectedContainers != null){
            return true;
        }

        return false;
    }
}
