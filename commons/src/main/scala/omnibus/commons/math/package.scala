package omnibus.commons

import cats.syntax.validated._
import org.apache.commons.math3.analysis.interpolation.{ LinearInterpolator, SplineInterpolator }


package object math {

  type Interpolator = (Double) => Double

  object Interpolator {
    def apply( xs: Array[Double], ys: Array[Double] ): AllIssuesOr[Interpolator]= {
      checkDimensions( xs, ys ) map { case (x, y) => if ( x.size < 3 ) linearInterpolator(x, y) else splineInterpolator(x, y) }
    }
    
    def checkDimensions( xs: Array[Double], ys: Array[Double] ): AllIssuesOr[(Array[Double], Array[Double])] = {
      if ( xs.size < 2 ) NumberIsTooSmallError(xs).invalidNel
      else if ( xs.size != ys.size ) MismatchedDimensionsError(xs, ys).invalidNel
      else ( xs, ys ).validNel
    }

    val linearGenerator = new LinearInterpolator
    def linearInterpolator( xs: Array[Double], ys: Array[Double] ): Interpolator = {
      val linear = linearGenerator.interpolate( xs, ys )
      (x: Double) => { linear value x }
    }

    val splineGenerator = new SplineInterpolator
    def splineInterpolator( xs: Array[Double], ys: Array[Double] ): Interpolator = {
      val spline = splineGenerator.interpolate( xs, ys )
      (x: Double) => { spline value x }
    }
    

    final case class NumberIsTooSmallError private[math]( xs: Array[Double] )
    extends IllegalArgumentException( s"""not enough xs [${xs.mkString(",")}] to interpolate""" ) with MathError

    final case class MismatchedDimensionsError private[math]( xs: Array[Double], ys: Array[Double] )
    extends IllegalArgumentException( s"""xs [${xs.mkString(",")}] size different than ys [${ys.mkString(",")}]""" ) with MathError
  }

}
