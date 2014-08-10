package peds.commons.identifier

//dmr: covariant?
case class TaggedID[T]( tag: Symbol, id: T ) {
  override def toString: String = s"${tag.name}:${id}"
}

object TaggedID {
  implicit def taggedId2Id[T]( tid: TaggedID[T] ): T = tid.id
}
