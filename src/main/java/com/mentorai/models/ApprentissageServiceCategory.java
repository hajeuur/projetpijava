package com.mentorai.models;

import java.util.ArrayList;
import java.util.List;

public class ApprentissageServiceCategory {
    private String name;
    private List<ApprentissageServiceItem> items;

    public ApprentissageServiceCategory(String name) {
        this.name = name;
        this.items = new ArrayList<>();
    }

    public void addItem(ApprentissageServiceItem item) {
        items.add(item);
    }

    public String getName() { return name; }
    public List<ApprentissageServiceItem> getItems() { return items; }
}
