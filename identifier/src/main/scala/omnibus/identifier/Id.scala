package omnibus.identifier

sealed abstract class Id[E: Identifying: Labeling] extends Equals with Serializable {
  type IdType

  def value: IdType

  protected def label: String = implicitly[Labeling[E]].label

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Id[E]]

  override def hashCode(): Int = 41 * (41 + value.##)

  override def equals( rhs: Any ): Boolean = rhs match {
    case that @ Id( thatValue ) => {
      scribe.debug( s"comparing Ids: ${this} == ${that}" )
      if (this eq that) true
      else {
        (that.## == this.##) &&
        (that canEqual this) &&
        (thatValue == this.value)
      }
    }

    case _ => false
  }

  override def toString: String = {
    val l = implicitly[Labeling[E]].label
    if (l.isEmpty) value.toString
    else s"${l}(${value})"
  }
}

object Id {

  type Aux[E, I] = Id[E] { type IdType = I }

  // Due to the use of dependent types, `of` requires explicit type application,
  // merely adding a type signature to the returned value is not enough:
  // one should instead always use Id.of[TypeOfTheTag]
  def of[E, I]( id: I )( implicit i: Identifying.Aux[E, I], l: Labeling[E] ): Id[E] = {
    unsafeCreate( id )
  }

  def fromString[E: Identifying: Labeling]( idRep: String ): Id[E] = {
    val id = implicitly[Identifying[E]].valueFromRep( idRep )
    unsafeCreate( id )
  }

  def unapply[E]( id: Id[E] ): Option[Id[E]#IdType] = Some( id.value )

  private[identifier] def unsafeCreate[E: Identifying: Labeling, I]( id: I ): Id[E] = {
    Simple( value = id )
  }

  private final case class Simple[E: Identifying: Labeling, I](
    override val value: I
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
