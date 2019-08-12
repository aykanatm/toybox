package com.github.murataykanat.toybox.utilities;

import java.util.Comparator;
import java.util.List;

public class SortUtils{

    private static SortUtils sortUtils;

    private SortUtils(){}

    public static SortUtils getInstance(){
        if(sortUtils != null){
            return sortUtils;
        }

        sortUtils = new SortUtils();
        return sortUtils;
    }

    public  <T> List<T> sortItems(String sortType, List<T> items, Comparator<T> comparing) {
        if(sortType.equalsIgnoreCase("des")){
            items.sort(comparing.reversed());
        }
        else if(sortType.equalsIgnoreCase("asc")){
            items.sort(comparing);
        }
        else{
            items.sort(comparing.reversed());
        }

        return items;
    }
}
