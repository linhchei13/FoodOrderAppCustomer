package com.example.foodorderappcustomer.Models;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {
    private String id;
    private String name;
    private int maxSelections;
    private boolean required;
    private List<Option> options;

    private OptionGroup() {

    }

    public OptionGroup(String id, String name, int maxSelections, boolean required) {
        this.id = id;
        this.name = name;
        this.maxSelections = maxSelections;
        this.required = required;
        this.options = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public boolean isRequired() {
        return required;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public void addOption(Option option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
    }
    
    // Return all selected options from this group
    public List<Option> getSelectedOptions() {
        List<Option> selectedOptions = new ArrayList<>();
        if (options != null) {
            for (Option option : options) {
                if (option.isSelected()) {
                    selectedOptions.add(option);
                }
            }
        }
        return selectedOptions;
    }
}
