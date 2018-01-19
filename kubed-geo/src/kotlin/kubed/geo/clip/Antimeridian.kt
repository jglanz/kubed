package kubed.geo.clip

import kubed.geo.GeometryStream
import kubed.math.EPSILON
import kubed.math.HALF_PI
import kotlin.math.*

fun clipAntimeridian() = { stream: GeometryStream -> ClipStream(AntimeridianClip(), stream) }

class AntimeridianClip : Clip {
    override var start = doubleArrayOf(-PI, -HALF_PI)
    override fun isVisible(x: Double, y: Double) = true

    override fun clipLine(stream: GeometryStream): IntersectStream {
        var lambda0 = Double.NaN
        var phi0 = Double.NaN
        var sign0 = Double.NaN

        return object : IntersectStream {
            override var clean = 0

            override fun lineStart() {
                stream.lineStart()
                clean = 1
            }

            override fun point(x: Double, y: Double, z: Double) {
                var lambda1 = x
                val phi1 = y
                val sign1 = if(lambda1 > 0) PI else -PI
                val delta = abs(lambda1 - lambda0)
                if(abs(delta - PI) < EPSILON) { // Line crosses pole
                    phi0 = if((phi0 + phi1) / 2 > 0) HALF_PI else -HALF_PI
                    stream.point(lambda0, phi0, 0.0)
                    stream.point(sign0, phi0, 0.0)
                    stream.lineEnd()
                    stream.lineStart()
                    stream.point(sign1, phi0, 0.0)
                    stream.point(lambda1, phi0, 0.0)
                    clean = 0
                }
                else if(sign0 != sign1 && delta >= PI) {
                    if(abs(lambda0 - sign0) < EPSILON) lambda0 -= sign0 * EPSILON
                    if(abs(lambda1 - sign1) < EPSILON) lambda1 -= sign1 * EPSILON
                    phi0 = intersect(lambda0, phi0, lambda1, phi1)
                    stream.point(sign0, phi0, 0.0)
                    stream.lineEnd()
                    stream.lineStart()
                    stream.point(sign1, phi0, 0.0)
                    clean = 0
                }
                lambda0 = lambda1
                phi0 = phi1
                stream.point(lambda0, phi0, 0.0)
                sign0 = sign1
            }

            override fun lineEnd() {
                stream.lineEnd()
                lambda0 = Double.NaN
                phi0 = Double.NaN
            }

            private fun intersect(lambda0: Double, phi0: Double, lambda1: Double, phi1: Double): Double {
                val sinLambda0Lambda1 = sin(lambda0 - lambda1)
                return when {
                    abs(sinLambda0Lambda1) > EPSILON -> {
                        val cosPhi0 = cos(phi0)
                        val cosPhi1 = cos(phi1)
                        atan((sin(phi0) * cosPhi1 * sin(lambda1)
                                - sin(phi1) * cosPhi0 * sin(lambda0))
                                / (cosPhi0 * cosPhi1 * sinLambda0Lambda1))
                    }
                    else -> (phi0 + phi1) / 2
                }
            }
        }
    }

    override fun interpolate(from: DoubleArray?, to: DoubleArray?, direction: Int, stream: GeometryStream) {
        when {
            from == null -> {
                val phi = direction * HALF_PI
                stream.point(-PI, phi, 0.0)
                stream.point(0.0, phi, 0.0)
                stream.point(PI, phi, 0.0)
                stream.point(PI, 0.0, 0.0)
                stream.point(PI, -phi, 0.0)
                stream.point(0.0, -phi, 0.0)
                stream.point(-PI, -phi, 0.0)
                stream.point(-PI, 0.0, 0.0)
                stream.point(-PI, phi, 0.0)
            }
            to == null -> throw IllegalStateException("to is required when from is provided")
            abs(from[0] - to[0]) > EPSILON -> {
                val lambda = if(from[0] < to[0]) PI else -PI
                val phi = direction * lambda / 2
                stream.point(-lambda, phi, 0.0)
                stream.point(0.0, phi, 0.0)
                stream.point(lambda, phi, 0.0)
            }
            else -> stream.point(to[0], to[1], 0.0)
        }
    }
}