package com.shoprunner.baleen.version2;

import com.shoprunner.baleen.DataTrace;
import com.shoprunner.baleen.ValidationError;
import com.shoprunner.baleen.ValidationResult;

import java.util.Collections;

// @DataDescription
public class Wolf {
    private String name;
    private Integer numLegs;

    public Wolf(String name, Integer numLegs) {
        this.name = name;
        this.numLegs = numLegs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumLegs() {
        return numLegs;
    }

    public void setNumLegs(Integer numLegs) {
        this.numLegs = numLegs;
    }

    public static Iterable<ValidationResult> assertNameNotWolfy(Wolf wolf, DataTrace dataTrace) {
        if(wolf.getName().equals("Wolfy")) {
            return Collections.singletonList(new ValidationError(dataTrace, "Name should not be 'Wolfy'", wolf.getName()));
        } else {
            return Collections.emptyList();
        }
    }
}
