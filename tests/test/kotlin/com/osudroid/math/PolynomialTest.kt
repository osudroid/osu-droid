package com.osudroid.math

import com.osudroid.math.Polynomial
import org.junit.Assert
import org.junit.Test

class PolynomialTest {
    @Test
    fun `Test polynomial`() {
        class Case(val expected: Double, val z: Double, val coefficients: DoubleArray)

        listOf(
            Case(0.0, 0.0, doubleArrayOf()),
            Case(0.0, 123.0, doubleArrayOf()),
            Case(0.0, 0.0, doubleArrayOf(0.0)),
            Case(0.0, 123.0, doubleArrayOf(0.0)),
            Case(1.0, 0.0, doubleArrayOf(1.0)),
            Case(1.0, 123.0, doubleArrayOf(1.0)),
            Case(2.0, 0.0, doubleArrayOf(2.0)),
            Case(2.0, 123.0, doubleArrayOf(2.0)),
            Case(1.0, 0.0, doubleArrayOf(1.0, 2.0)),
            Case(7.0, 3.0, doubleArrayOf(1.0, 2.0)),
            Case(1.0, 0.0, doubleArrayOf(1.0, 2.0, 3.0)),
            Case(57.0, 4.0, doubleArrayOf(1.0, 2.0, 3.0))
        ).forEach { case ->
             Assert.assertEquals(
                 "Invalid polynomial evaluation at ${case.z} for coefficients (${case.coefficients.joinToString(", ")})",
                 case.expected,
                 Polynomial.evaluate(case.z, case.coefficients),
                 0.0
             )
        }
    }
}