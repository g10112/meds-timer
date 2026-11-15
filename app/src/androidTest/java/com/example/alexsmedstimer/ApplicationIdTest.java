package com.example.alexsmedstimer;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ApplicationIdTest {
    @Test
    public void publicAppHasStableApplicationId() {
        assertEquals("io.github.g10112.medstimer",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName());
    }
}
