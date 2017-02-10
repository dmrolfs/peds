package omnibus.archetype.domain.model.core

import scala.reflect._
import scalaz._, Scalaz._
import shapeless.Lens
import com.typesafe.scalalogging.LazyLogging
import omnibus.commons.TryV
import omnibus.commons.identifier._
import omnibus.commons.util._


trait Identifiable {
  type ID
  def id: TID

  type TID = TaggedID[ID]
  val evID: ClassTag[ID]
  val evTID: ClassTag[TID]
}


trait IdentifiableLensProvider[I <: Identifiable] {
  def idLens: Lens[I, I#TID]
}
