package peds.commons.math

import org.apache.commons.math3.linear.{ LUDecomposition, RealMatrix, MatrixUtils }
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.stat.correlation.Covariance


/**
  * Created by rolfsd on 11/25/15.
  */
case class MahalanobisDistance( group: RealMatrix ) extends DistanceMeasure {

  def covariance: RealMatrix = new Covariance( group ).getCovarianceMatrix

  override def compute( centroid: Array[Double], point: Array[Double] ): Double = {
    if ( centroid.deep == point.deep ) 0D
    else {
      val diffVector = MatrixUtils.createRowRealMatrix( centroid.zip( point ) map { case (c, p) => c - p } )
      val inverseCovariance = new LUDecomposition( covariance ).getSolver.getInverse
      math.sqrt( diffVector.multiply(inverseCovariance).multiply(diffVector.transpose).getEntry(0,0) )
    }
  }
}
