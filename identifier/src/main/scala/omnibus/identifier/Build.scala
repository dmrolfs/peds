//package omnibus.identifier
//
//sealed trait Build[E] {
//  type Out <: Serializable
//  def fromString[E]( s: String )( implicit l: Label[IdType], i: Identifying[E] ): Out
//  def next()( implicit l: Label[A], i: Identifying[Out] ): Out
//}
//
//object Build {
//  type Aux[I, O <: Serializable] = Build[I] { type Out = O }
//
//  private[identifier] def default[A]: Aux[A, Id[A]] = new Build[A] {
//    type Out = Id[A]
//
//    override def next()( implicit l: Label[A], i: Identifying[Id[A]] ): Id[A] = {
//      Id.unsafeCreate( i.next )
//    }
//
//    override def fromString( v: String )( implicit l: Label[A], i: Identifying[Out] ): Out = {
//      Id.unsafeCreate( i.fromString( v ) )
//    }
//  }
//
//  trait Validated[A] extends Build[A] {
//    final override type Out = Option[Id[A]]
//
//    override def next()( implicit l: Label[A], i: Identifying[Option[Id[A]]] ): Option[Id[A]] = {
//      Option( Id.unsafeCreate( i.next ) )
//    }
//
//    final override def fromString( s: String )( implicit l: Label[A], i: Identifying[Out] ): Out = {
//      validate( s ) map { rep =>
//        Id.unsafeCreate( i.fromString( rep ) )
//      }
//    }
//
//    def validate( s: String ): Option[String]
//  }
//}
