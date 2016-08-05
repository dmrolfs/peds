package peds.commons.identifier


case class TaggedID[+T]( tag: Symbol, id: T ) extends Equals {
  def get: T = id

  override def canEqual( rhs: Any ): Boolean = {
    rhs match {
      case tagged: TaggedID[_] => {
        val thatClazz = tagged.id.getClass
        this.id.getClass.isAssignableFrom( thatClazz )
      }
      case _ => false
    }
  }

  override def equals( rhs: Any ): Boolean = {
    rhs match {
      case that: TaggedID[_] => {
        if ( this eq that ) true
        else {
          ( that.## == this.## ) &&
          ( that canEqual this ) &&
          ( this.id == that.id ) &&
          ( this.tag == that.tag )
        }
      }

      case _ => false
    }
  }

  override def hashCode(): Int = 41 * ( 41 + id.## ) + tag.##

  override lazy val toString: String = tag.name + TaggedID.Delimiter + id.toString

}

object TaggedID {
  val Delimiter: String = "-"
  val Regex = s"""((.+)${Delimiter})?(.+)""".r  // this breaks down if Delimiter needs to be escaped
  implicit def taggedId2Id[T]( tid: TaggedID[T] ): T = tid.id
}
