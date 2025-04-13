package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class ErrorFunctionTest {
    @Test
    fun testErrorFunction() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-10) {
            Assert.assertEquals(ErrorFunction.erf(expected), value, delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(-1.0, -0.8427007929497149)
        assert(0.0, 0.0, 0.0)
        assert(1e-15, 0.000000000000001128379167095513)
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
        assert(6.0, 0.9999999999999999)
        assert(Double.POSITIVE_INFINITY, 1.0, 0.0)
        assert(Double.NEGATIVE_INFINITY, -1.0, 0.0)
    }

    @Test
    fun testComplementaryErrorFunction() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-10) {
            Assert.assertEquals(ErrorFunction.erfc(expected), value, delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(-1.0, 1.8427007929497148)
        assert(0.0, 1.0, 0.0)
        assert(0.1, 0.887537083981715)
        assert(0.2, 0.77729741078952153)
        assert(0.3, 0.6713732405408726)
        assert(0.4, 0.5716076449533315)
        assert(0.5, 0.4795001221869535)
        assert(1.0, 0.15729920705028513)
        assert(1.5, 0.03389485352468927)
        assert(2.0, 0.004677734981047266)
        assert(2.5, 0.0004069520174449589)
        assert(3.0, 0.000022090496998585441)
        assert(4.0, 0.00000001541725790028002)
        assert(5.0, 0.000000000001537459794428035)
        assert(6.0, 2.1519736712498913e-17)
        assert(10.0, 2.088487583762545e-45)
        assert(15.0, 7.212994172451207e-100)
        assert(20.0, 5.395865611607901e-176)
        assert(30.0, 2.5646562037561116000333972775014471465488897227786155e-393)
        assert(50.0, 2.0709207788416560484484478751657887929322509209953988e-1088)
        assert(80.0, 2.3100265595063985852034904366341042118385080919280966e-2782)
        assert(Double.POSITIVE_INFINITY, 0.0, 0.0)
        assert(Double.NEGATIVE_INFINITY, 2.0, 0.0)
    }

    @Test
    fun testInverseErrorFunction() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-4) {
            Assert.assertEquals(ErrorFunction.erfInv(expected), value, delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(-0.8427007929497149, -1.0)
        assert(0.0, 0.0, 0.0)
        assert(0.0000000000000011283791670955127, 1e-15)
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
        assert(1.0, Double.POSITIVE_INFINITY)
        assert(-1.0, Double.NEGATIVE_INFINITY)
    }

    @Test
    fun testInverseComplementaryErrorFunction() {
        fun assert(expected: Double, value: Double, delta: Double = 1e-7) {
            Assert.assertEquals(ErrorFunction.erfcInv(expected), value, delta)
        }

        assert(Double.NaN, Double.NaN)
        assert(0.0, Double.POSITIVE_INFINITY)
        assert(1e-100, 15.065574702593)
        assert(1e-30, 8.1486162231699)
        assert(1e-20, 6.6015806223551)
        assert(1e-10, 4.572824958544925)
        assert(1e-5, 3.123413274341571)
        assert(0.1, 1.163087153676674)
        assert(0.2, 0.9061938024368233)
        assert(0.5, 0.4769362762044699)
        assert(1.0, 0.0)
        assert(1.5, -0.476936276204469878)
        assert(2.0, Double.NEGATIVE_INFINITY)
    }
}