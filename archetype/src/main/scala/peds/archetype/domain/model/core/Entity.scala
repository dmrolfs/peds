package peds.archetype.domain.model.core

import scala.reflect.ClassTag
import peds.commons.util._


trait Entity extends Identifiable {
  override type That <: Entity
  def name: String
  // def nameLens: Lens[this.type, String]
  // def nameLens: Lens[That, String]

  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}

// object Entity {
//   implicit def referenceEntity( e: Entity ): EntityRef = EntityRef( e )
// }
