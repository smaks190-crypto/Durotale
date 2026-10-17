package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun exampleTest() {
        // Simple smoke test to ensure Robolectric test class compiles and runs in CI
        assertTrue(true)
    }
}
