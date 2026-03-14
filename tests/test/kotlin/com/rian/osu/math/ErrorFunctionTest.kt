package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class ErrorFunctionTest {
    private data class ClassicTestCase(val expected: Double, val value: Double, val delta: Double = 1e-10)
    private data class FastTestCase(val value: Double, val delta: Double = 1e-5)

    @Test
    fun `Test error function`() {
        listOf(
            ClassicTestCase(Double.NaN, Double.NaN),
            ClassicTestCase(-0.8427007929497149, -1.0),
            ClassicTestCase(0.0, 0.0, 0.0),
            ClassicTestCase(0.000000000000001128379167095513, 1e-15),
            ClassicTestCase(0.1124629160182849, 0.1),
            ClassicTestCase(0.2227025892104785, 0.2),
            ClassicTestCase(0.3286267594591274, 0.3),
            ClassicTestCase(0.4283923550466685, 0.4),
            ClassicTestCase(0.5204998778130465, 0.5),
            ClassicTestCase(0.8427007929497149, 1.0),
            ClassicTestCase(0.966105146475311, 1.5),
            ClassicTestCase(0.9953222650189527, 2.0),
            ClassicTestCase(0.999593047982555, 2.5),
            ClassicTestCase(0.9999779095030014, 3.0),
            ClassicTestCase(0.9999999845827421, 4.0),
            ClassicTestCase(0.9999999999984625, 5.0),
            ClassicTestCase(0.9999999999999999, 6.0),
            ClassicTestCase(1.0, Double.POSITIVE_INFINITY, 0.0),
            ClassicTestCase(-1.0, Double.NEGATIVE_INFINITY, 0.0)
        ).forEach { (expected, value, delta) ->
            Assert.assertEquals(
                "Invalid: erf($value) ≠ $expected",
                expected,
                ErrorFunction.erf(value),
                delta
            )
        }
    }

    @Test
    fun `Test fast error function`() {
        listOf(
            FastTestCase(Double.NaN),
            FastTestCase(-1.0),
            FastTestCase(0.0, 0.0),
            FastTestCase(1e-15),
            FastTestCase(0.1),
            FastTestCase(0.2),
            FastTestCase(0.3),
            FastTestCase(0.4),
            FastTestCase(0.5),
            FastTestCase(1.0),
            FastTestCase(1.5),
            FastTestCase(2.0),
            FastTestCase(2.5),
            FastTestCase(3.0),
            FastTestCase(4.0),
            FastTestCase(5.0),
            FastTestCase(6.0),
            FastTestCase(Double.POSITIVE_INFINITY, 0.0),
            FastTestCase(Double.NEGATIVE_INFINITY, 0.0)
        ).forEach { (value, delta) ->
            val expected = ErrorFunction.erf(value)

            Assert.assertEquals(
                "Invalid: erfFast($value) ≠ $expected",
                expected,
                ErrorFunction.erfFast(value),
                delta
            )
        }
    }

    @Test
    fun `Test complementary error function`() {
        listOf(
            ClassicTestCase(Double.NaN, Double.NaN),
            ClassicTestCase(1.8427007929497148, -1.0),
            ClassicTestCase(1.0, 0.0, 0.0),
            ClassicTestCase(0.887537083981715, 0.1),
            ClassicTestCase(0.7772974107895215, 0.2),
            ClassicTestCase(0.6713732405408726, 0.3),
            ClassicTestCase(0.5716076449533315, 0.4),
            ClassicTestCase(0.4795001221869535, 0.5),
            ClassicTestCase(0.15729920705028513, 1.0),
            ClassicTestCase(0.03389485352468927, 1.5),
            ClassicTestCase(0.004677734981047266, 2.0),
            ClassicTestCase(0.0004069520174449589, 2.5),
            ClassicTestCase(0.00002209049699858544, 3.0),
            ClassicTestCase(0.00000001541725790028002, 4.0),
            ClassicTestCase(0.000000000001537459794428035, 5.0),
            ClassicTestCase(2.1519736712498913e-17, 6.0),
            ClassicTestCase(12.088487583762546e-45, 10.0),
            ClassicTestCase(17.212994172451206e-100, 15.0),
            ClassicTestCase(25.3958656116079e-176, 20.0),
            ClassicTestCase(32.5646562037561116000333972775014471465488897227786155e-393, 30.0),
            ClassicTestCase(52.0709207788416560484484478751657887929322509209953988e-1088, 50.0),
            ClassicTestCase(82.3100265595063985852034904366341042118385080919280966e-2782, 80.0),
            ClassicTestCase(0.0, Double.POSITIVE_INFINITY, 0.0),
            ClassicTestCase(2.0, Double.NEGATIVE_INFINITY, 0.0)
        ).forEach { (expected, value, delta) ->
            Assert.assertEquals(
                "Invalid: erfc($value) ≠ $expected",
                expected,
                ErrorFunction.erfc(value),
                delta
            )
        }
    }

    @Test
    fun `Test fast complementary error function`() {
        listOf(
            FastTestCase(Double.NaN),
            FastTestCase(-1.0),
            FastTestCase(0.0, 0.0),
            FastTestCase(0.1),
            FastTestCase(0.2),
            FastTestCase(0.3),
            FastTestCase(0.4),
            FastTestCase(0.5),
            FastTestCase(1.0),
            FastTestCase(1.5),
            FastTestCase(2.0),
            FastTestCase(2.5),
            FastTestCase(3.0),
            FastTestCase(4.0),
            FastTestCase(5.0),
            FastTestCase(6.0),
            FastTestCase(10.0),
            FastTestCase(15.0),
            FastTestCase(20.0),
            FastTestCase(30.0),
            FastTestCase(50.0),
            FastTestCase(80.0),
            FastTestCase(Double.POSITIVE_INFINITY, 0.0),
            FastTestCase(Double.NEGATIVE_INFINITY, 0.0)
        ).forEach { (value, delta) ->
            val expected = ErrorFunction.erfc(value)

            Assert.assertEquals(
                "Invalid: erfcFast($value) ≠ $expected",
                expected,
                ErrorFunction.erfcFast(value),
                delta
            )
        }
    }

    @Test
    fun `Test inverse error function`() {
        listOf(
            ClassicTestCase(Double.NaN, Double.NaN, 1e-4),
            ClassicTestCase(-1.0, -0.8427007929497149, 1e-4),
            ClassicTestCase(0.0, 0.0, 0.0),
            ClassicTestCase(1e-15, 0.0000000000000011283791670955127, 1e-4),
            ClassicTestCase(0.1, 0.1124629160182849, 1e-4),
            ClassicTestCase(0.2, 0.2227025892104785, 1e-4),
            ClassicTestCase(0.3, 0.3286267594591274, 1e-4),
            ClassicTestCase(0.4, 0.4283923550466685, 1e-4),
            ClassicTestCase(0.5, 0.5204998778130465, 1e-4),
            ClassicTestCase(1.0, 0.8427007929497149, 1e-4),
            ClassicTestCase(1.5, 0.966105146475311, 1e-4),
            ClassicTestCase(2.0, 0.9953222650189527, 1e-4),
            ClassicTestCase(2.5, 0.999593047982555, 1e-4),
            ClassicTestCase(3.0, 0.9999779095030014, 1e-4),
            ClassicTestCase(4.0, 0.9999999845827421, 1e-4),
            ClassicTestCase(5.0, 0.9999999999984625, 1e-4),
            ClassicTestCase(Double.POSITIVE_INFINITY, 1.0, 1e-4),
            ClassicTestCase(Double.NEGATIVE_INFINITY, -1.0, 1e-4)
        ).forEach { (expected, value, delta) ->
            Assert.assertEquals(
                "Invalid: erfInv($value) ≠ $expected",
                expected,
                ErrorFunction.erfInv(value),
                delta
            )
        }
    }

    @Test
    fun `Test fast inverse error function`() {
        listOf(
            FastTestCase(Double.NaN, 1e-3),
            FastTestCase(-0.8427007929497149, 1e-3),
            FastTestCase(0.0, 0.0),
            FastTestCase(0.0000000000000011283791670955127, 1e-3),
            FastTestCase(0.1124629160182849, 1e-3),
            FastTestCase(0.2227025892104785, 1e-3),
            FastTestCase(0.3286267594591274, 1e-3),
            FastTestCase(0.4283923550466685, 1e-3),
            FastTestCase(0.5204998778130465, 1e-3),
            FastTestCase(0.8427007929497149, 1e-3),
            FastTestCase(0.966105146475311, 1e-3),
            FastTestCase(0.9953222650189527, 1e-3),
            FastTestCase(0.999593047982555, 1e-3),
            FastTestCase(0.9999779095030014, 1e-3),
            FastTestCase(0.9999999845827421, 1e-2),
            FastTestCase(0.9999999999984625, 1e-2),
            FastTestCase(1.0, 1e-3),
            FastTestCase(-1.0, 1e-3)
        ).forEach { (value, delta) ->
            val expected = ErrorFunction.erfInv(value)

            Assert.assertEquals(
                "Invalid: erfInvFast($value) ≠ $expected",
                expected,
                ErrorFunction.erfInvFast(value),
                delta
            )
        }
    }

    @Test
    fun `Test inverse complementary error function`() {
        listOf(
            ClassicTestCase(Double.NaN, Double.NaN, 1e-7),
            ClassicTestCase(Double.POSITIVE_INFINITY, 0.0, 1e-7),
            ClassicTestCase(15.065574702593, 1e-100, 1e-7),
            ClassicTestCase(8.1486162231699, 1e-30, 1e-7),
            ClassicTestCase(6.6015806223551, 1e-20, 1e-7),
            ClassicTestCase(4.572824958544925, 1e-10, 1e-7),
            ClassicTestCase(3.123413274341571, 1e-5, 1e-7),
            ClassicTestCase(1.163087153676674, 0.1, 1e-7),
            ClassicTestCase(0.9061938024368233, 0.2, 1e-7),
            ClassicTestCase(0.4769362762044699, 0.5, 1e-7),
            ClassicTestCase(0.0, 1.0, 1e-7),
            ClassicTestCase(-0.4769362762044699, 1.5, 1e-7),
            ClassicTestCase(Double.NEGATIVE_INFINITY, 2.0, 1e-7)
        ).forEach { (expected, value, delta) ->
            Assert.assertEquals(
                "Invalid: erfcInv($value) ≠ $expected",
                expected,
                ErrorFunction.erfcInv(value),
                delta
            )
        }
    }

    @Test
    fun `Test fast inverse complementary error function`() {
        listOf(
            FastTestCase(Double.NaN),
            FastTestCase(0.0, 0.0),
            // 1e-100, 1e-30, 1e-20 will underflow in calculation and result in infinity, so we do not test them.
            FastTestCase(1e-10, 1e-2),
            FastTestCase(1e-5, 1e-3),
            FastTestCase(0.1, 1e-3),
            FastTestCase(0.2, 1e-3),
            FastTestCase(0.5, 1e-3),
            FastTestCase(1.0, 1e-3),
            FastTestCase(1.5, 1e-3),
            FastTestCase(2.0)
        ).forEach { (value, delta) ->
            val expected = ErrorFunction.erfcInv(value)

            Assert.assertEquals(
                "Invalid: erfcInvFast($value) ≠ $expected",
                expected,
                ErrorFunction.erfcInvFast(value),
                delta
            )
        }
    }
}