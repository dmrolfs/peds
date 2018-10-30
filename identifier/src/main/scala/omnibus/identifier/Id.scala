package omnibus.identifier

import io.circe.Decoder.Result
import io.circe.{ Json => CJson, _ }
import io.circe.syntax._

import scala.language.{ higherKinds, implicitConversions }
import scala.reflect.{ classTag, ClassTag }
import omnibus.core.syntax.clazz._
import play.api.libs.json.{ Format, JsResult, JsSuccess, JsValue, Writes, Json => PJson }

sealed abstract class Id[E] extends Equals with Product with Serializable {
  //todo: better handle primitive boxing
  type IdType

  def value: IdType

  def as[T]( implicit evI: Identifying.Aux[T, IdType], evL: Labeling[T] ): Id.Aux[T, IdType] = {
    Id.of[T, IdType]( this.value )
  }

  def unsafeAs[T: Labeling]: Id.Aux[T, IdType] = Id.unsafeOf[T, IdType]( this.value )

  protected def label: String

  override def productPrefix: String = "Id"
  override def productArity: Int = 1

  @throws( classOf[IndexOutOfBoundsException] )
  override def productElement( n: Int ): Any = n match {
    case 0 => value
    case _ => throw new IndexOutOfBoundsException( n.toString() )
  }

  @transient lazy private val IdClassType: ClassTag[Id.Aux[E, IdType]] = classTag[Id.Aux[E, IdType]]
  override def canEqual( rhs: Any ): Boolean = IdClassType.unapply( rhs ).isDefined
//    rhs.isInstanceOf[Id[E]]
//  }

  override def hashCode(): Int = 41 * (41 + value.##)

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: Id[_] => {
      if (this eq that) true
      else {
        (that.## == this.##) &&
        (that canEqual this) &&
        (that.value == this.value)
      }
    }

    case _ => false
  }

  override def toString: String = {
    val l = label
    if (l.isEmpty) value.toString
    else s"${l}${productPrefix}(${value})"
  }
}

trait LowPriorityFormats {
  implicit def idPlayJsonWrites[E]: Writes[Id[E]] = new Writes[Id[E]] {
    override def writes( id: Id[E] ): JsValue = PJson toJson id.value.toString
  }
}

object Id extends LowPriorityFormats {

  type Aux[E, I] = Id[E] {
    type IdType = I
  }

  def unapply[E]( id: Id[E] ): Option[id.IdType] = Some( id.value )

  def unsafeOf[E: Labeling, I]( id: I ): Id.Aux[E, I] = {
    unsafeCreate( id, label = Labeling[E].label )
  }

  // Due to the use of dependent types, `of` requires explicit type application,
  // merely adding a type signature to the returned value is not enough:
  // one should instead always use Id.of[TypeOfTheTag]
  def of[E, I](
    id: I
  )(
    implicit evIdentifying: Identifying.Aux[E, I],
    l: Labeling[E]
  ): Id.Aux[E, I] = {
    unsafeCreate( id, l.label )
  }

  def fromString[E, I](
    idRep: String
  )(
    implicit i: Identifying.Aux[E, I],
    l: Labeling[E]
  ): Id.Aux[E, I] = {
    unsafeCreate( i valueFromRep idRep, l.label )
  }

  implicit def unwrap[C[_], E: Labeling, I]( composite: Id.Aux[C[E], I] ): Id.Aux[E, I] = {
    unsafeCreate( composite.value, Labeling[E].label )
  }

  implicit def wrap[C[_], E: Labeling, I]( id: Id.Aux[E, I] ): Id.Aux[C[E], I] = {
    unsafeCreate( id.value, Labeling[E].label )
  }

  implicit def auxPlayJsonFormat[E, I](
    implicit i: Identifying.Aux[E, I],
    l: Labeling[E]
  ): Format[Id.Aux[E, I]] = new Format[Id.Aux[E, I]] {
    override def reads( json: JsValue ): JsResult[Id.Aux[E, I]] =
      JsSuccess( fromString[E, I]( json.as[String] ) )
    override def writes( o: Id.Aux[E, I] ): JsValue = {
      PJson toJson o.value.toString
    }
  }

  implicit def idJsonEncoder[E]: Encoder[Id[E]] = new Encoder[Id[E]] {
    override def apply( id: Id[E] ): CJson = CJson fromString id.value.toString
  }

  implicit def auxJsonEncoder[E, I]: Encoder[Id.Aux[E, I]] = new Encoder[Id.Aux[E, I]] {
    override def apply( id: Id.Aux[E, I] ): CJson = CJson fromString id.value.toString
  }

  implicit def auxJsonDecoder[E, I](
    implicit i: Identifying.Aux[E, I],
    l: Labeling[E]
  ): Decoder[Aux[E, I]] = {
    new Decoder[Aux[E, I]] {
      override def apply( c: HCursor ): Result[Aux[E, I]] = {
        import cats.syntax.either._
        Either
          .fromOption[DecodingFailure, String](
            c.value.asString,
            DecodingFailure( "not a valid id string", c.history )
          )
          .map { fromString( _ )( i, l ) }
      }
    }
  }

  private[identifier] def unsafeCreate[E, I]( id: I, label: String ): Id.Aux[E, I] = {
    Simple( value = id, label = label )
  }

  private[identifier] final case class Simple[E, I](
    override val value: I,
    override protected val label: String
  ) extends Id[E] {
    override type IdType = I
  }
}

//  @annotation.implicitNotFound(
//    "Descriptor is not a valid identifying Tag. Declare it to be a case object to fix this error"
//  )
//  private sealed trait IsCaseObject[D]
//  private object IsCaseObject {
//    implicit def ev[D <: Singleton with Product]: IsCaseObject[D] = null
//  }

//  @annotation.implicitNotFound(
//    s"an identifier must be a serializable type to fix this error"
//  )
//  private sealed trait IsSerializable[A]
//  private object IsSerializable {
//    implicit def ev[I <: Serializable]: IsSerializable[I] = null
//  }
