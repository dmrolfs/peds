package omnibus.commons.math

import scala.util.Try
import cats.data.Validated
import cats.syntax.validated._
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix}
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.stat.correlation.Covariance
import omnibus.commons.AllIssuesOr


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
  def fromCovariance( covariance: RealMatrix ): AllIssuesOr[MahalanobisDistance] = {
    checkCovarianceMatrixDimension( covariance ) map { cov =>
      SimpleMahalanobisDistance( dimension = cov.getRowDimension, covariance = cov )
    }
  }

  def fromPoints( group: RealMatrix ): AllIssuesOr[MahalanobisDistance] = {
    checkCovariance( group )
    .orElse { identityMatrix( group.getColumnDimension ).validNel }
    .map { cov => SimpleMahalanobisDistance( dimension = group.getColumnDimension, covariance = cov ) }
  }

  def checkCovarianceMatrixDimension( square: RealMatrix ): AllIssuesOr[RealMatrix] = {
    if ( square.getRowDimension == square.getColumnDimension ) square.validNel
    else {
      new IllegalArgumentException(
        s"matrix must be square: Row Dimension [${square.getRowDimension}] " +
        s"not equal to Column Dimension [${square.getColumnDimension}]"
      ).invalidNel
    }
  }

  def checkCovariance( group: RealMatrix ): AllIssuesOr[RealMatrix] = {
    Validated.catchNonFatal{ new Covariance( group ).getCovarianceMatrix }.toValidatedNel
  }

  def identityMatrix( dimension: Int ): RealMatrix = MatrixUtils createRealIdentityMatrix dimension


  final case class SimpleMahalanobisDistance private[math](
    override val dimension: Int,
    override val covariance: RealMatrix
  ) extends MahalanobisDistance
}
