package com.bharath.skillstudio.catalog;

import com.bharath.skillstudio.learn.CoreConcept;

final class Concepts {

    private Concepts() {
    }

    static CoreConcept of(String title, String why, String useCase) {
        return new CoreConcept(title, why, useCase);
    }
}
