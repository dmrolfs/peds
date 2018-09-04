package omnibus.identifier

sealed abstract class Id[E] extends Equals with Serializable {
//  type Entity
  //todo: better handle primitive boxing
  type IdType

  def value: IdType

  protected def label: String

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
    val l = label
    if (l.isEmpty) value.toString
    else s"${l}(${value})"
  }
}

object Id {

  type Aux[E, I] = Id[E] {
//    type Entity = E
    type IdType = I
  }

  def unapply[E]( id: Id[E] ): Option[id.IdType] = Some( id.value )

  def unsafeOf[E: Labeling, I]( id: I ): Id.Aux[E, I] = unsafeCreate( id )

  // Due to the use of dependent types, `of` requires explicit type application,
  // merely adding a type signature to the returned value is not enough:
  // one should instead always use Id.of[TypeOfTheTag]
  def of[E, I]( id: I )( implicit i: Identifying.Aux[E, I], l: Labeling[E] ): Id.Aux[E, I] = {
    unsafeCreate( id )
  }

//  def compositeOf[C[_], E, I](
//    id: I
//  )( implicit i: Identifying.Aux[E, I], l: Labeling[E] ): Id.Aux[E, I] = {
//    unsafeCreate( id )
//  }

  def fromString[E, I](
    idRep: String
  )(
    implicit i: Identifying.Aux[E, I],
    l: Labeling[E]
  ): Id.Aux[E, I] = {
    unsafeCreate( i valueFromRep idRep )
  }

//  def compositeFromString[C[_], E, I](
//    idRep: String
//  )(
//    implicit i: Identifying.Aux[E, I],
//    l: Labeling[E]
//  ): Id.Aux[E, I] = {
//    val id: I = implicitly[Identifying[E]].valueFromRep( idRep ).asInstanceOf[I]
//    unsafeCreate( id )
//  }

  private[identifier] def unsafeCreate[E: Labeling, I]( id: I ): Id.Aux[E, I] = {
//    Composite( value = id )
    Simple( value = id )
  }

//  private final case class Composite[C[_], E: Labeling, I]( override val value: I ) extends Id[E] {
//    override type Entity = E
//    override type IdType = I
//    override protected val label: String = implicitly[Labeling[E]].label
//  }

  private final case class Simple[E: Labeling, I]( override val value: I ) extends Id[E] {
//    override type Entity = E
    override type IdType = I
    override protected val label: String = Labeling[E].label
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
