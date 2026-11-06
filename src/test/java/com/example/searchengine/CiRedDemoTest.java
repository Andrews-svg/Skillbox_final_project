package com.example.searchengine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CiRedDemoTest {

    @Test
    void intentionalFailure() {
        assertTrue(false, "demo: intentional failure to see CI red");
    }
}