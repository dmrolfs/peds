package peds.commons.math

import scalaz._, Scalaz._
import org.apache.commons.math3.linear.{ LUDecomposition, RealMatrix, MatrixUtils }
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.stat.correlation.Covariance
import peds.commons.V


/**
  * Created by rolfsd on 11/25/15.
  */
trait MahalanobisDistance extends DistanceMeasure {
  def dimension: Int
  def covariance: RealMatrix

  lazy val inverseCovariance: RealMatrix = {
    val solver = new LUDecomposition( covariance ).getSolver
    if ( solver.isNonSingular ) solver.getInverse
    else new LUDecomposition( MatrixUtils createRealIdentityMatrix dimension ).getSolver.getInverse
  }

  override def compute( centroid: Array[Double], point: Array[Double] ): Double = {
    if ( centroid.deep == point.deep ) 0D
    else {
      val diffVector = MatrixUtils.createRowRealMatrix( centroid.zip( point ) map { case (c, p) => c - p } )
      math.sqrt( diffVector.multiply( inverseCovariance ).multiply( diffVector.transpose() ).getEntry( 0, 0 ) )
    }
  }
}

object MahalanobisDistance {
  def fromCovariance( covariance: RealMatrix ): V[MahalanobisDistance] = {
    checkDimension( covariance ) map { c =>
      SimpleMahalanobisDistance( dimension = covariance.getRowDimension, covariance = covariance )
    }
  }

  def fromPoints( group: RealMatrix ): V[MahalanobisDistance] = {
    checkDimension( group ) map { g =>
      SimpleMahalanobisDistance( dimension = g.getRowDimension, covariance = new Covariance( g ).getCovarianceMatrix )
    }
  }

  def checkDimension( square: RealMatrix ): V[RealMatrix] = {
    if ( square.getRowDimension == square.getColumnDimension ) square.right
    else {
      NonEmptyList[Throwable](
        new IllegalArgumentException(
          s"matrix must be square: Row Dimension [${square.getRowDimension}] " +
          s"not equal to Column Dimension [${square.getColumnDimension}]"
        )
      ).left
    }
  }


  final case class SimpleMahalanobisDistance private[math](
    override val dimension: Int,
    override val covariance: RealMatrix
  ) extends MahalanobisDistance
}
