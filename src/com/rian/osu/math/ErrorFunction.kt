package com.rian.osu.math

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * An object containing all methods related to the error function.
 *
 * This object shares the same implementation as [Math.NET Numerics](https://numerics.mathdotnet.com/).
 */
object ErrorFunction {
    //#region Coefficients for erfImp

    /**
     * Polynomial coefficients for a numerator of erfImp
     * calculation for `erf(x)` in the interval `[1e-10, 0.5]`.
     */
    private val erfImpAn = doubleArrayOf(
        0.003379167095512574, -0.0007369565304816795, -0.3747323373929196,
        0.0817442448733587, -0.04210893199365486, 0.007016570951209576,
        -0.004950912559824351, 0.0008716465990379225,
    )

    /**
     * Polynomial coefficients for a denominator of erfImp
     * calculation for `erf(x)` in the interval `[1e-10, 0.5]`.
     */
    private val erfImpAd = doubleArrayOf(
        1.0, -0.2180882180879246, 0.4125429727254421, -0.08418911478731068,
        0.0655338856400242, -0.01200196044549418, 0.00408165558926174,
        -0.0006159007215577697,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[0.5, 0.75]`.
     */
    private val erfImpBn = doubleArrayOf(
        -0.03617903907182625, 0.2922518834448827, 0.2814470417976045,
        0.12561020886276694, 0.02741350282689305, 0.002508396721680658,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[0.5, 0.75]`.
     */
    private val erfImpBd = doubleArrayOf(
        1.0, 1.8545005897903486, 1.4357580303783142, 0.5828276587530365,
        0.1248104769329497, 0.011372417654635328,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[0.75, 1.25]`.
     */
    private val erfImpCn = doubleArrayOf(
        -0.03978768926111369, 0.1531652124678783, 0.19126029560093624,
        0.10276327061989304, 0.029637090615738836, 0.004609348678027549,
        0.0003076078203486802,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[0.75, 1.25]`.
     */
    private val erfImpCd = doubleArrayOf(
        1.0, 1.955200729876277, 1.6476231719938486, 0.7682386070221263,
        0.20979318593650978, 0.03195693168999134, 0.0021336316089578537,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[1.25, 2.25]`.
     */
    private val erfImpDn = doubleArrayOf(
        -0.030083856055794972, 0.05385788298444545, 0.07262115416519142,
        0.03676284698880493, 0.009646290155725275, 0.0013345348007529107,
        0.778087599782504e-4,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[1.25, 2.25]`.
     */
    private val erfImpDd = doubleArrayOf(
        1.0, 1.7596709814716753, 1.3288357143796112, 0.5525285965087576,
        0.1337930569413329, 0.017950964517628076, 0.001047124400199374,
        -0.10664038182035734e-7,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[2.25, 3.5]`.
     */
    private val erfImpEn = doubleArrayOf(
        -0.011790757013722784, 0.01426213209053881, 0.02022344359029608,
        0.00930668299990432, 0.00213357802422066, 0.000250229873864601,
        0.1205349122195882e-4,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[2.25, 3.5]`.
     */
    private val erfImpEd = doubleArrayOf(
        1.0, 1.5037622520362048, 0.9653977862044629, 0.3392652304767967,
        0.068974064954157, 0.007710602624917683, 0.0003714211015310693,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[3.5, 5.25]`.
     */
    private val erfImpFn = doubleArrayOf(
        -0.005469547955387293, 0.004041902787317071, 0.005496336955316117,
        0.002126164726039454, 0.0003949840144950839, 3.655654770644424e-5,
        0.13548589710993232e-5,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[3.5, 5.25]`.
     */
    private val erfImpFd = doubleArrayOf(
        1.0, 1.210196977736308, 0.6209146682211439, 0.1730384306611428,
        0.0276550813773432, 0.002406259744243097, 0.8918118172513366e-4,
        -0.4655288362833827e-11,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[5.25, 8]`.
     */
    private val erfImpGn = doubleArrayOf(
        -0.002707225359057783, 0.00131875634250294, 0.0011992593326100233,
        0.00027849619811344664, 0.2678229882183318e-4, 0.9230436723150282e-6,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[5.25, 8]`.
     */
    private val erfImpGd = doubleArrayOf(
        1.0, 0.8146328085431416, 0.26890166585629954, 0.04498772161030411,
        0.003817596633202485, 0.0001315718978885969, 0.4048153596757641e-11,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[8, 11.5]`.
     */
    private val erfImpHn = doubleArrayOf(
        -0.001099467206917422, 0.00040642544275042267, 0.0002744994894169007,
        0.4652937706466594e-4, 3.2095542539576746e-6, 0.778286018145021e-7,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[8, 11.5]`.
     */
    private val erfImpHd = doubleArrayOf(
        1.0, 0.588173710611846, 0.13936333128940975, 0.016632934041708368,
        0.0010002392131023491, 0.2425483752158723e-4,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[11.5, 17]`.
     */
    private val erfImpIn = doubleArrayOf(
        -0.0005690799360109496, 0.0001694985403737623, 0.5184723545811009e-4,
        0.38281931223192885e-5, 0.8249899312818944e-7,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[11.5, 17]`.
     */
    private val erfImpId = doubleArrayOf(
        1.0, 0.3396372500511393, 0.04347264787031066, 0.002485493352246371,
        0.5356333053371529e-4, -0.11749094440545958e-12,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[17, 24]`.
     */
    private val erfImpJn = doubleArrayOf(
        -2.4131359948399134e-4, 0.5742249752025015e-4, 0.11599896292738377e-4,
        0.581762134402594e-6, 0.8539715550856736e-8,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[17, 24]`.
     */
    private val erfImpJd = doubleArrayOf(
        1.0, 0.23304413829968784, 0.02041869405464403, 0.0007971856475643983,
        0.11701928167017232e-4,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[24, 38]`.
     */
    private val erfImpKn = doubleArrayOf(
        -0.00014667469927776036, 0.1626665521122805e-4, 0.26911624850916523e-5,
        0.979584479468092e-7, 0.10199464762572346e-8,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[24, 38]`.
     */
    private val erfImpKd = doubleArrayOf(
        1.0, 0.16590781294484722, 0.010336171619150588, 0.0002865930263738684,
        0.29840157084090034e-5,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[38, 60]`.
     */
    private val erfImpLn = doubleArrayOf(
        -0.5839057976297718e-4, 0.4125103251054962e-5, 0.43179092242025094e-6,
        0.9933651555900132e-8, 0.653480510020105e-10,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[38, 60]`.
     */
    private val erfImpLd = doubleArrayOf(
        1.0, 0.1050770860720399, 0.004142784286754756, 0.726338754644524e-4,
        4.778184710473988e-7,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[60, 85]`.
     */
    private val erfImpMn = doubleArrayOf(
        -1.9645779760922958e-5, 0.1572438876668007e-5, 0.5439025111927009e-7,
        0.3174724923691177e-9,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[60, 85]`.
     */
    private val erfImpMd = doubleArrayOf(
        1.0, 0.05280398924095763, 0.0009268760691517533, 0.5410117232266303e-5,
        0.5350938458036424e-15,
    )

    /**
     * Polynomial coefficients for a numerator in erfImp
     * calculation for `erfc(x)` in the interval `[85, 110]`.
     */
    private val erfImpNn = doubleArrayOf(
        -0.789224703978723e-5, 0.622088451660987e-6, 0.1457284456768824e-7,
        0.603715505542715e-10,
    )

    /**
     * Polynomial coefficients for a denominator in erfImp
     * calculation for `erfc(x)` in the interval `[85, 110]`.
     */
    private val erfImpNd = doubleArrayOf(
        1.0, 0.03753288463562937, 0.0004679195359746253, 0.19384703927584565e-5,
    )

    //#endregion
    //#region Coefficients for erfInvImp

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0, 0.5]`.
     */
    private val ervInvImpAn = doubleArrayOf(
        -0.0005087819496582806, -0.008368748197417368, 0.03348066254097446,
        -0.012692614766297402, -0.03656379714117627, 0.02198786811111689,
        0.008226878746769157, -0.005387729650712429,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0, 0.5]`.
     */
    private val ervInvImpAd = doubleArrayOf(
        1.0, -0.9700050433032906, -1.565745582341758, 1.5622155839842302,
        0.662328840472003, -0.7122890234154285, -0.05273963823400997,
        0.07952836873415717, -0.0023339375937419, 0.0008862163904564247,
    )

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.5, 0.75]`.
     */
    private val ervInvImpBn = doubleArrayOf(
        -0.2024335083559388, 0.10526468069939171, 8.3705032834312,
        17.6447298408374, -18.85106480587143, -44.6382324441787,
        17.445385985570866, 21.12946554483405, -3.671922547077293,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.5, 0.75]`.
     */
    private val ervInvImpBd = doubleArrayOf(
        1.0, 6.242641248542475, 3.971343795334387, -28.66081804998,
        -20.14326346804852, 48.56092131087399, 10.82686673554602,
        -22.64369334131397, 1.7211476576120028,
    )

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` less than 3.
     */
    private val ervInvImpCn = doubleArrayOf(
        -0.1311027816799519, -0.1637940471933171, 0.11703015634199525,
        0.38707973897260434, 0.3377855389120359, 0.1428695344081572,
        0.029015791000532906, 0.002145589953888053, -0.6794655751811264e-6,
        0.2852253317822171e-7, -0.681149956853777e-9,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` less than 3.
     */
    private val ervInvImpCd = doubleArrayOf(
        1.0, 3.466254072425672, 5.381683457070069, 4.778465929458438,
        2.5930192162362027, 0.848854343457902, 0.1522643382953318,
        0.01105924229346489,
    )

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` between 3 and 6.
     */
    private val ervInvImpDn = doubleArrayOf(
        -0.0350353787183178, -0.002224265292134479, 0.018557330651423107,
        0.009508047013259196, 0.001871234928195592, 0.00015754461742496055,
        0.460469890584318e-5, -0.2304047769118826e-9, 0.266339227425782e-11,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` between 3 and 6.
     */
    private val ervInvImpDd = doubleArrayOf(
        1.0, 1.365334981755406, 0.7620591645536234, 0.22009110576413124,
        0.03415891436709477, 0.00263861676657016, 0.7646752923027944e-4,
    )

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` between 6 and 18.
     */
    private val ervInvImpEn = doubleArrayOf(
        -0.016743100507663373, -0.001129514387455803, 0.001056288621524929,
        0.0002093863174875881, 0.14962478375834237e-4, 0.4496967899277065e-6,
        0.4625961635228786e-8, -2.811287356288318e-14,
        0.9905570997331033e-16,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` between 6 and 18.
     */
    private val ervInvImpEd = doubleArrayOf(
        1.0, 0.5914293448864175, 0.1381518657490833, 0.01607460870936765,
        0.0009640118070051655, 0.275335474764726e-4, 0.282243172016108e-6,
    )

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` between 18 and 44.
     */
    private val ervInvImpFn = doubleArrayOf(
        -0.002497821279189813, -0.779190719229054e-5, 0.2547230374130275e-4,
        0.1623977773425109e-5, 3.963410113048012e-8, 0.4116328311909442e-9,
        0.145596286718675e-11, -0.11676501239718427e-17,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` between 18 and 44.
     */
    private val ervInvImpFd = doubleArrayOf(
        1.0, 0.2071231122144225, 0.01694108381209759, 0.0006905382656226846,
        0.14500735981823264e-4, 0.14443775662814415e-6, 0.5097612765997785e-9,
    )

    /**
     * Polynomial coefficients for a numerator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` greater than 44.
     */
    private val ervInvImpGn = doubleArrayOf(
        -0.0005390429110190786, -0.2839875900472772e-6, 0.8994651148922914e-6,
        0.2293458592659209e-7, 0.2255614448635001e-9, 0.9478466275030226e-12,
        0.13588013010892486e-14, -0.3488903933999489e-21,
    )

    /**
     * Polynomial coefficients for a denominator of erfInvImp
     * calculation for `erf^-1(z)` in the interval `[0.75, 1]` with `z` greater than 44.
     */
    private val ervInvImpGd = doubleArrayOf(
        1.0, 0.08457462340018994, 0.002820929847262647, 0.4682929219408942e-4,
        0.3999688121938621e-6, 0.1618092908879045e-8, 0.2315586083102596e-11,
    )

    //#endregion
    //#region Evaluations

    /**
     * Calculates the error function.
     *
     * @param x The value to evaluate.
     * @return The error function evaluated at [x], or:
     * - `1` if [x] is [Double.POSITIVE_INFINITY];
     * - `-1` if [x] is [Double.NEGATIVE_INFINITY].
     */
    fun erf(x: Double) = when {
        x == 0.0 -> 0.0
        x == Double.POSITIVE_INFINITY -> 1.0
        x == Double.NEGATIVE_INFINITY -> -1.0
        x.isNaN() -> Double.NaN
        else -> erfImp(x, false)
    }

    /**
     * Calculates the complementary error function.
     *
     * @param x The value to evaluate.
     * @return The complementary error function evaluated at [x], or:
     * - `0` if [x] is [Double.POSITIVE_INFINITY];
     * - `2` if [x] is [Double.NEGATIVE_INFINITY].
     */
    fun erfc(x: Double) = when {
        x == 0.0 -> 1.0
        x == Double.POSITIVE_INFINITY -> 0.0
        x == Double.NEGATIVE_INFINITY -> 2.0
        x.isNaN() -> Double.NaN
        else -> erfImp(x, true)
    }

    /**
     * Calculates the inverse error function evaluated at [z].
     *
     * @param z The value to evaluate.
     * @return The inverse error function evaluated at [z], or:
     * - [Double.POSITIVE_INFINITY] if [z]` >= 1`;
     * - [Double.NEGATIVE_INFINITY] if [z]` <= -1`.
     */
    fun erfInv(z: Double) = when {
        z == 0.0 -> 0.0
        z >= 1 -> Double.POSITIVE_INFINITY
        z <= -1 -> Double.NEGATIVE_INFINITY
        z.isNaN() -> Double.NaN
        else -> {
            val p: Double
            val q: Double
            val s: Double

            if (z < 0) {
                p = -z
                q = 1 - p
                s = -1.0
            } else {
                p = z
                q = 1 - z
                s = 1.0
            }

            erfInvImp(p, q, s)
        }
    }

    /**
     * Calculates the complementary inverse error function evaluated at [z].
     *
     * This implementation has been tested against the arbitrary precision mpmath library
     * and found cases where only 9 significant figures correct can be guaranteed.
     *
     * @param z The value to evaluate.
     * @returns The complementary inverse error function evaluated at [z], or:
     * - [Double.POSITIVE_INFINITY] if [z]` <= 0`;
     * - [Double.NEGATIVE_INFINITY] if [z]` >= -2`.
     */
    fun erfcInv(z: Double) = when {
        z <= 0 -> Double.POSITIVE_INFINITY
        z >= 2 -> Double.NEGATIVE_INFINITY
        z.isNaN() -> Double.NaN
        else -> {
            val p: Double
            val q: Double
            val s: Double

            if (z > 1) {
                q = 2 - z
                p = 1 - q
                s = -1.0
            } else {
                p = 1 - z
                q = z
                s = 1.0
            }

            erfInvImp(p, q, s)
        }
    }

    /**
     * The implementation of the error function.
     *
     * @param z Where to evaluate the error function.
     * @param invert Whether to compute 1 - the error function.
     * @returns The error function.
     */
    private fun erfImp(z: Double, invert: Boolean): Double {
        if (z < 0) {
            if (!invert) {
                return -erfImp(-z, false)
            }

            if (z < -0.5) {
                return 2 - erfImp(-z, true)
            }

            return 1 + erfImp(-z, false)
        }

        var inverted = invert
        val result: Double

        // Big bunch of selection statements now to pick which
        // implementation to use, try to put most likely options
        // first:
        if (z < 0.5) {
            // We're going to calculate erf:
            result =
                if (z < 1e-10) z * 1.125 + z * 0.003379167095512574
                // Worst case absolute error found: 6.688618532e-21
                else z * 1.125 + z * Polynomial.evaluate(z, erfImpAn) / Polynomial.evaluate(z, erfImpAd)
        } else if (z < 110) {
            // We'll be calculating erfc:
            inverted = !inverted
            val r: Double
            val b: Double

            when {
                z < 0.75 -> {
                    // Worst case absolute error found: 5.582813374e-21
                    r = Polynomial.evaluate(z - 0.5, erfImpBn) / Polynomial.evaluate(z - 0.5, erfImpBd)
                    b = 0.3440242112
                }
                
                z < 1.25 -> {
                    // Worst case absolute error found: 4.01854729e-21
                    r = Polynomial.evaluate(z - 0.75, erfImpCn) / Polynomial.evaluate(z - 0.75, erfImpCd)
                    b = 0.419990927
                }

                z < 2.25 -> {
                    // Worst case absolute error found: 2.866005373e-21
                    r = Polynomial.evaluate(z - 1.25, erfImpDn) / Polynomial.evaluate(z - 1.25, erfImpDd)
                    b = 0.4898625016
                }

                z < 3.5 -> {
                    // Worst case absolute error found: 1.045355789e-21
                    r =  Polynomial.evaluate(z - 2.25, erfImpEn) / Polynomial.evaluate(z - 2.25, erfImpEd)
                    b = 0.5317370892
                }

                z < 5.25 -> {
                    // Worst case absolute error found: 8.300028706e-22
                    r = Polynomial.evaluate(z - 3.5, erfImpFn) / Polynomial.evaluate(z - 3.5, erfImpFd)
                    b = 0.5489973426
                }

                z < 8 -> {
                    // Worst case absolute error found: 1.700157534e-21
                    r = Polynomial.evaluate(z - 5.25, erfImpGn) / Polynomial.evaluate(z - 5.25, erfImpGd)
                    b = 0.5571740866
                }

                z < 11.5 -> {
                    // Worst case absolute error found: 3.002278011e-22
                    r = Polynomial.evaluate(z - 8, erfImpHn) / Polynomial.evaluate(z - 8, erfImpHd)
                    b = 0.5609807968
                }

                z < 17 -> {
                    // Worst case absolute error found: 6.741114695e-21
                    r = Polynomial.evaluate(z - 11.5, erfImpIn) / Polynomial.evaluate(z - 11.5, erfImpId)
                    b = 0.5626493692
                }

                z < 24 -> {
                    // Worst case absolute error found: 7.802346984e-22
                    r = Polynomial.evaluate(z - 17, erfImpJn) / Polynomial.evaluate(z - 17, erfImpJd)
                    b = 0.5634598136
                }

                z < 38 -> {
                    // Worst case absolute error found: 2.414228989e-22
                    r = Polynomial.evaluate(z - 24, erfImpKn) / Polynomial.evaluate(z - 24, erfImpKd)
                    b = 0.5638477802
                }

                z < 60 -> {
                    // Worst case absolute error found: 5.896543869e-24
                    r = Polynomial.evaluate(z - 38, erfImpLn) / Polynomial.evaluate(z - 38, erfImpLd)
                    b = 0.5640528202
                }

                z < 85 -> {
                    // Worst case absolute error found: 3.080612264e-21
                    r = Polynomial.evaluate(z - 60, erfImpMn) / Polynomial.evaluate(z - 60, erfImpMd)
                    b = 0.5641309023
                }

                else -> {
                    // Worst case absolute error found: 8.094633491e-22
                    r = Polynomial.evaluate(z - 85, erfImpNn) / Polynomial.evaluate(z - 85, erfImpNd)
                    b = 0.5641584396
                }
            }

            val g = exp(-z * z) / z

            result = g * (b + r)
        } else {
            // Any value of z larger than 28 will underflow to zero:
            result = 0.0
            inverted = !inverted
        }

        return if (inverted) 1 - result else result
    }

    /**
     * The implementation of the inverse error function.
     *
     * @param p The first intermediate parameter.
     * @param q The second intermediate parameter.
     * @param s The third intermediate parameter.
     * @returns The inverse error function.
     */
    private fun erfInvImp(p: Double, q: Double, s: Double): Double {
        val result: Double

        if (p <= 0.5) {
            // Evaluate inverse erf using the rational approximation:
            //
            // x = p(p+10)(Y+R(p))
            //
            // Where Y is a constant, and R(p) is optimized for a low
            // absolute error compared to |Y|.
            //
            // double: Max error found: 2.001849e-18
            // long double: Max error found: 1.017064e-20
            // Maximum Deviation Found (actual error term at infinite precision) 8.030e-21
            val y = 0.08913147449493408
            val g = p * (p + 10)
            val r = Polynomial.evaluate(p, ervInvImpAn) / Polynomial.evaluate(p, ervInvImpAd)

            result = g * (y + r)
        } else if (q >= 0.25) {
            // Rational approximation for 0.5 > q >= 0.25
            //
            // x = sqrt(-2*log(q)) / (Y + R(q))
            //
            // Where Y is a constant, and R(q) is optimized for a low
            // absolute error compared to Y.
            //
            // double : Max error found: 7.403372e-17
            // long double : Max error found: 6.084616e-20
            // Maximum Deviation Found (error term) 4.811e-20

            val y = 2.249481201171875
            val g = sqrt(-2 * ln(q))
            val xs = q - 0.25
            val r = Polynomial.evaluate(xs, ervInvImpBn) / Polynomial.evaluate(xs, ervInvImpBd)

            result = g / (y + r)
        } else {
            // For q < 0.25 we have a series of rational approximations of all the general form:
            //
            // let: x = sqrt(-log(q))
            //
            // Then the result is given by:
            //
            // x(Y+R(x-B))
            //
            // where Y is a constant, B is the lowest value of x for which
            // the approximation is valid, and R(x-B) is optimized for a low
            // absolute error compared to Y.
            //
            // Note that almost all code will really go through the first
            // or maybe second approximation.  After than we're dealing with very
            // small input values indeed: 80 and 128 bit long double's go all the
            // way down to ~ 1e-5000 so the "tail" is rather long...

            val x = sqrt(-ln(q))

            val y: Double
            val r: Double

            when {
                x < 3 -> {
                    // Max error found: 1.089051e-20
                    y = 0.807220458984375
                    val xs = x - 1.125
                    r = Polynomial.evaluate(xs, ervInvImpCn) / Polynomial.evaluate(xs, ervInvImpCd)
                }

                x < 6 -> {
                    // Max error found: 8.389174e-21
                    y = 0.9399557113647461
                    val xs = x - 3
                    r = Polynomial.evaluate(xs, ervInvImpDn) / Polynomial.evaluate(xs, ervInvImpDd)
                }

                x < 18 -> {
                    // Max error found: 1.481312e-19
                    y = 0.9836282730102539
                    val xs = x - 6
                    r = Polynomial.evaluate(xs, ervInvImpEn) / Polynomial.evaluate(xs, ervInvImpEd)
                }

                x < 44 -> {
                    // Max error found: 5.697761e-20
                    y = 0.9971456527709961
                    val xs = x - 18
                    r = Polynomial.evaluate(xs, ervInvImpFn) / Polynomial.evaluate(xs, ervInvImpFd)
                }

                else -> {
                    // Max error found: 1.279746e-20
                    y = 0.9994134902954102
                    val xs = x - 44
                    r = Polynomial.evaluate(xs, ervInvImpGn) / Polynomial.evaluate(xs, ervInvImpGd)
                }
            }

            result = x * (y + r)
        }

        return s * result
    }

    //#endregion
}