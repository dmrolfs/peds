package peds.commons.identifier

import scala.reflect.ClassTag
import scalaz._, Scalaz._
import com.typesafe.scalalogging.LazyLogging
import peds.commons.TryV
import peds.commons.util._


/**
  * Type class used to define fundamental ID operations for a specific type of identifier.
  *
  * @tparam T
  */
trait Identifying[T] extends LazyLogging {
  type ID
  val evID: ClassTag[ID]

  type TID = TaggedID[ID]
  val evTID: ClassTag[TID]

  val idTag: Symbol
  implicit def tag( id: ID ): TID = TaggedID[ID]( idTag, id )

  def castIntoTID( id: Any ): Option[TID] = evID.unapply( id ) map { tag }
  def idOf( o: T ): TID
  def fromString( idstr: String ): ID
  def nextId: TryV[TID]
  def nextIdAs[TI: ClassTag]: TryV[TI] = nextId flatMap { tidAs[TI] }
  def safeNextId: TID = {
    val result = nextId flatMap { nid =>
      nid match {
        case evTID( t ) => t.right
        case t => new ClassCastException( s"${t} id-type is not of type ${nid.id.getClass.getCanonicalName}" ).left
      }
    }

    result match {
      case \/-( r ) => r
      case -\/( ex ) => {
        logger.error( "failed to generate nextId", ex )
        throw ex
      }
    }
  }

  def safeParseId[I: ClassTag]( idstr: String ): I = {
    idAs[I]( fromString( idstr ) ) match {
      case scalaz.\/-( id ) => id
      case scalaz.-\/( ex ) => {
        logger.error( s"failed to parse id string:[${idstr}]", ex )
        throw ex
      }
    }
  }

  def bridgeIdClassTag[I: ClassTag]: TryV[ClassTag[I]] = bridgeClassTag[I, ID]( evID )
  def bridgeTidClassTag[TI: ClassTag]: TryV[ClassTag[TI]] = bridgeClassTag[TI, TID]( evTID )

  def bridgeClassTag[LHS: ClassTag, RHS]( evRHS: ClassTag[RHS] ): TryV[ClassTag[LHS]] = {
    val otag = for {
      lhs <- Option( implicitly[ClassTag[LHS]] )
      rhs <- Option( evRHS )
    } yield {
      if ( lhs == rhs ) lhs.right[Throwable]
      else {
        logger.error( "LHS ClassTag = [{}]", lhs )
        logger.error( "RHS ClassTag = [{}]", rhs )
        new ClassCastException(
          s"ID[${lhs.runtimeClass.getCanonicalName}] is equivalent to Identifying[T]#ID[${rhs.runtimeClass.getCanonicalName}]"
        ).left
      }
    }

    otag
    .getOrElse {
      new IllegalStateException( s"I[${implicitly[ClassTag[LHS]]}] or evID[${evRHS}] are not sufficient" ).left
    }
  }

  def idAs[I: ClassTag]( id: ID ): TryV[I] = rhsAsLhs[I, ID]( id )( evID )

  def tidAs[TI: ClassTag]( tid: TID ): TryV[TI] = rhsAsLhs[TI, TID]( tid )( evTID )

  def rhsAsLhs[LHS: ClassTag, RHS]( v: RHS )( evRHS: ClassTag[RHS] ): TryV[LHS] = {
    bridgeClassTag[LHS, RHS]( evRHS ) map { ctag =>
      val ctag( result ) = v
      result
    }
  }
}

object Identifying {
  final case class NotDefinedForId[T: ClassTag]( operation: String )
  extends IllegalStateException(
    s"${operation} is not defined for type: ${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}"
  )
}
