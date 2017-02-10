package omnibus.commons.identifier

import scala.reflect.ClassTag


case class TaggedID[+T]( tag: Symbol, id: T ) extends Equals {
  def get: T = id

  def mapTo[B: ClassTag]: TaggedID[B] = {
    val boxedClass = {
      val b = implicitly[ClassTag[B]].runtimeClass
      if ( b.isPrimitive ) TaggedID.toBoxed( b ) else b
    }
    require( boxedClass ne null )
    TaggedID[B]( tag, boxedClass.cast( id ).asInstanceOf[B] )
  }

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
          ( this.tag.name == that.tag.name )
        }
      }

      case _ => false
    }
  }

  override val hashCode: Int = 41 * ( 41 + id.## ) + tag.name.##

  override lazy val toString: String = tag.name + TaggedID.Delimiter + id.toString

}

object TaggedID {
  val Delimiter: String = "-"
  val Regex = s"""((.+)${Delimiter})?(.+)""".r  // this breaks down if Delimiter needs to be escaped
  implicit def taggedId2Id[T]( tid: TaggedID[T] ): T = tid.id

  private[identifier] val toBoxed: Map[Class[_], Class[_]] = Map(
    classOf[Boolean] -> classOf[java.lang.Boolean],
    classOf[Byte]    -> classOf[java.lang.Byte],
    classOf[Char]    -> classOf[java.lang.Character],
    classOf[Short]   -> classOf[java.lang.Short],
    classOf[Int]     -> classOf[java.lang.Integer],
    classOf[Long]    -> classOf[java.lang.Long],
    classOf[Float]   -> classOf[java.lang.Float],
    classOf[Double]  -> classOf[java.lang.Double],
    classOf[Unit]    -> classOf[scala.runtime.BoxedUnit]
  )
}
