package peds.commons.identifier


case class TaggedID[+T]( tag: Symbol, id: T ) {
  def get: T = id
  override lazy val toString: String = tag.name + TaggedID.Delimiter + id.toString
}

object TaggedID {
  val Delimiter: String = "-"
  val Regex = s"""((.+)${Delimiter})?(.+)""".r  // this breaks down if Delimiter needs to be escaped
  implicit def taggedId2Id[T]( tid: TaggedID[T] ): T = tid.id
}
