package dev.tensorappfactory.reference

import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceUnitTest {
    @Test
    fun referenceProductHasNonEmptyIdentity() {
        val productName = "Hello Tensor"
        assertTrue(productName.isNotBlank())
    }
}
