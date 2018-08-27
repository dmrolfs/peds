package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.identifier.{ Id, Identifying, ShortUUID }

abstract class Identifiable[E <: Identifiable[E, ID], ID](
  implicit protected val identifying: Identifying.Aux[E, ID]
) {
  type TID = Id.Aux[E, ID]
  def id: TID
}
//trait IdentifiableLike {
//  type E <: IdentifiableLike
//  implicit val identifying: Identifying[E]
//  type TID = identifying.TID
//  def id: TID
//}
//
//object IdentifiableLike {
//
//  type Aux[E0, TID0] = IdentifiableLike {
//    type E = E0
//    type TID = TID0
//  }
//
//  def apply( implicit i: IdentifiableLike ): Aux[i.E, i.TID] = i
//
//  //todo with or without Aux and type param?
//  def unapply( i: IdentifiableLike ): Option[i.TID] = Option( i.id )
//}
//
//abstract class Identifiable[E0 <: IdentifiableLike, ID](
//  implicit val identifying: Identifying.IdAux[E0, ID]
//) extends IdentifiableLike {
//  override type E = E0
//  override type TID = Id.Aux[E, ID]
//}

object Identifiable {

//  def apply[E]( implicit i: Identifiable[E] ): Aux[E, i.TID] = i
//
  //todo with or without Aux and type param?
  def unapply[E <: Identifiable[E, ID], ID]( i: Identifiable[E, ID] ): Option[i.TID] =
    Option( i.id )
}

abstract class IdentifiableLensProvider[E <: Identifiable[E, ID], ID] {
  implicit val identifying: Identifying.Aux[E, ID]
//  type TID = identifying.TID
  def nextId: E#TID = identifying.next
  def idLens: Lens[E, E#TID]
}

////////////////////////////////////////////

//object Zed {
//
//  def main( args: Array[String] ): Unit = {
//    val b = Bar( Bar.nextId, "bar" )
//    println( s"b = ${b}" )
//    val b1 = Bar( Id of 13, "bar" )
//    println( s"b1 = ${b1}" )
//
//    val f = Foo( Foo.nextId, "foo" )
//    println( s"f = ${f}" )
//    val f1 = Foo( Id of ShortUUID(), "f1" )
//    println( s"f1 = ${f1}" )
//  }
//}
//
//case class Bar( override val id: Bar#TID, name: String ) extends Identifiable[Bar, Long]
//
//object Bar extends IdentifiableLensProvider[Bar, Long] {
//  override val identifying = Identifying.byLong[Bar]
//  def idLens: Lens[Bar, Bar#TID] = shapeless.lens[Bar].id
//}
//
//abstract class Foo extends Identifiable[Foo, ShortUUID] {
//  def name: String
//}
//
//object Foo extends IdentifiableLensProvider[Foo, ShortUUID] {
//  def apply( id: Foo#TID, name: String ): Foo = SimpleFoo( id, name )
//
//  override implicit val identifying: Identifying.Aux[Foo, ShortUUID] = Identifying.byShortUuid
//
//  override def idLens: Lens[Foo, Foo#TID] = new Lens[Foo, Foo#TID] {
//    override def get( f: Foo ): Foo#TID = f.id
//    override def set( f: Foo )( tid: Foo#TID ): Foo = SimpleFoo( id = tid, name = f.name )
//  }
//
//  private case class SimpleFoo( override val id: Foo#TID, override val name: String ) extends Foo
//}
