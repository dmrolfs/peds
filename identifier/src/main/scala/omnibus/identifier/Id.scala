package omnibus.identifier

sealed abstract class Id[E: Identifying] extends Serializable {
  type IdType

  def value: IdType

  protected def label: Label

  override def toString: String = {
    val l = label.label
    if (l.isEmpty) value.toString
    else s"${l}(${value})"
  }
}

object Id {

  type Aux[E, I] = Id[E] { type IdType = I }

  private[identifier] def unsafeCreate[E, I](
    id: I
  )(
    implicit identifying: Identifying.Aux[E, I],
  ): Id[E] = {
    Simple( value = id )
  }

  private final case class Simple[E: Identifying, I](
    override val value: I
  ) extends Id[E] {
    override type IdType = I
    override protected val label: Label = implicitly[Identifying[E]].label
  }

  // Due to the use of dependent types, `of` requires explicit type application,
  // merely adding a type signature to the returned value is not enough:
  // one should instead always use Id.of[TypeOfTheTag]
  def of[E, I]( id: I )( implicit i: Identifying.Aux[E, I] ): Id[E] = unsafeCreate( id )

  def fromString[E: Identifying]( idRep: String ): Id[E] = {
    type I = Identifying[E]#ID
    implicit val identifying = implicitly[Identifying[E]].asInstanceOf[Identifying.Aux[E, I]]
    val id = implicitly[Identifying[E]].valueFromRep( idRep )
    unsafeCreate( id )
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
