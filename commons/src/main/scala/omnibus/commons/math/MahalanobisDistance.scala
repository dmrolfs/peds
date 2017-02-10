package omnibus.commons.math

import scalaz._, Scalaz._
import org.apache.commons.math3.linear.{ LUDecomposition, RealMatrix, MatrixUtils }
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.stat.correlation.Covariance
import omnibus.commons.Valid


/**
  * Created by rolfsd on 11/25/15.
  */
trait MahalanobisDistance extends DistanceMeasure {
  def dimension: Int
  def covariance: RealMatrix

  lazy val inverseCovariance: RealMatrix = {
    val solver = new LUDecomposition( covariance ).getSolver
    if ( solver.isNonSingular ) solver.getInverse
    else new LUDecomposition( MahalanobisDistance.identityMatrix( dimension ) ).getSolver.getInverse
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
  def fromCovariance( covariance: RealMatrix ): Valid[MahalanobisDistance] = {
    checkCovarianceMatrixDimension( covariance ) map { cov =>
      SimpleMahalanobisDistance( dimension = cov.getRowDimension, covariance = cov )
    }
  }

  def fromPoints( group: RealMatrix ): Valid[MahalanobisDistance] = {
    checkCovariance( group )
    .disjunction
    .recover { case exs => identityMatrix( group.getColumnDimension ) }
    .map { cov => SimpleMahalanobisDistance( dimension = group.getColumnDimension, covariance = cov ) }
    .validation
  }

  def checkCovarianceMatrixDimension( square: RealMatrix ): Valid[RealMatrix] = {
    if ( square.getRowDimension == square.getColumnDimension ) square.successNel
    else {
      Validation.failureNel(
        new IllegalArgumentException(
          s"matrix must be square: Row Dimension [${square.getRowDimension}] " +
          s"not equal to Column Dimension [${square.getColumnDimension}]"
        )
      )
    }
  }

  def checkCovariance( group: RealMatrix ): Valid[RealMatrix] = {
    Validation.fromTryCatchNonFatal{ new Covariance( group ).getCovarianceMatrix }.toValidationNel
  }

  def identityMatrix( dimension: Int ): RealMatrix = MatrixUtils createRealIdentityMatrix dimension


  final case class SimpleMahalanobisDistance private[math](
    override val dimension: Int,
    override val covariance: RealMatrix
  ) extends MahalanobisDistance
}
