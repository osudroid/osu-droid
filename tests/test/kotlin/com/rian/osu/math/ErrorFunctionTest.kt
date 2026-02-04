package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class ErrorFunctionTest {
    @Test
    fun `Test error function`() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-10) {
            Assert.assertEquals(expected, ErrorFunction.erf(value), delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(-0.8427007929497149, -1.0)
        assert(0.0, 0.0, 0.0)
        assert(0.000000000000001128379167095513, 1e-15)
        assert(0.1124629160182849, 0.1)
        assert(0.2227025892104785, 0.2)
        assert(0.3286267594591274, 0.3)
        assert(0.4283923550466685, 0.4)
        assert(0.5204998778130465, 0.5)
        assert(0.8427007929497149, 1.0)
        assert(0.966105146475311, 1.5)
        assert(0.9953222650189527, 2.0)
        assert(0.999593047982555, 2.5)
        assert(0.9999779095030014, 3.0)
        assert(0.9999999845827421, 4.0)
        assert(0.9999999999984625, 5.0)
        assert(0.9999999999999999, 6.0)
        assert(1.0, Double.POSITIVE_INFINITY, 0.0)
        assert(-1.0, Double.NEGATIVE_INFINITY, 0.0)
    }

    @Test
    fun `Test complementary error function`() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-10) {
            Assert.assertEquals(expected, ErrorFunction.erfc(value), delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(1.8427007929497148, -1.0)
        assert(1.0, 0.0, 0.0)
        assert(0.887537083981715, 0.1)
        assert(0.7772974107895215, 0.2)
        assert(0.6713732405408726, 0.3)
        assert(0.5716076449533315, 0.4)
        assert(0.4795001221869535, 0.5)
        assert(0.15729920705028513, 1.0)
        assert(0.03389485352468927, 1.5)
        assert(0.004677734981047266, 2.0)
        assert(0.0004069520174449589, 2.5)
        assert(0.00002209049699858544, 3.0)
        assert(0.00000001541725790028002, 4.0)
        assert(0.000000000001537459794428035, 5.0)
        assert(2.1519736712498913e-17, 6.0)
        assert(12.088487583762546e-45, 10.0)
        assert(17.212994172451206e-100, 15.0)
        assert(25.3958656116079e-176, 20.0)
        assert(32.5646562037561116000333972775014471465488897227786155e-393, 30.0)
        assert(52.0709207788416560484484478751657887929322509209953988e-1088, 50.0)
        assert(82.3100265595063985852034904366341042118385080919280966e-2782, 80.0)
        assert(0.0, Double.POSITIVE_INFINITY, 0.0)
        assert(2.0, Double.NEGATIVE_INFINITY, 0.0)
    }

    @Test
    fun `Test inverse error function`() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-4) {
            Assert.assertEquals(expected, ErrorFunction.erfInv(value), delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(-1.0, -0.8427007929497149)
        assert(0.0, 0.0, 0.0)
        assert(1e-15, 0.0000000000000011283791670955127)
        assert(0.1, 0.1124629160182849)
        assert(0.2, 0.2227025892104785)
        assert(0.3, 0.3286267594591274)
        assert(0.4, 0.4283923550466685)
        assert(0.5, 0.5204998778130465)
        assert(1.0, 0.8427007929497149)
        assert(1.5, 0.966105146475311)
        assert(2.0, 0.9953222650189527)
        assert(2.5, 0.999593047982555)
        assert(3.0, 0.9999779095030014)
        assert(4.0, 0.9999999845827421)
        assert(5.0, 0.9999999999984625)
        assert(Double.POSITIVE_INFINITY, 1.0)
        assert(Double.NEGATIVE_INFINITY, -1.0)
    }

    @Test
    fun `Test inverse complementary error function`() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-7) {
            Assert.assertEquals(expected, ErrorFunction.erfcInv(value), delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(Double.POSITIVE_INFINITY, 0.0)
        assert(15.065574702593, 1e-100)
        assert(8.1486162231699, 1e-30)
        assert(6.6015806223551, 1e-20)
        assert(4.572824958544925, 1e-10)
        assert(3.123413274341571, 1e-5)
        assert(1.163087153676674, 0.1)
        assert(0.9061938024368233, 0.2)
        assert(0.4769362762044699, 0.5)
        assert(0.0, 1.0)
        assert(-0.4769362762044699, 1.5)
        assert(Double.NEGATIVE_INFINITY, 2.0)
    }
}