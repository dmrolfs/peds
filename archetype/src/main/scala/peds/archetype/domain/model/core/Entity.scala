package peds.archetype.domain.model.core

import scala.reflect.ClassTag
import shapeless.Lens
import peds.commons.util._


trait Entity extends Identifiable {
  def name: String
  def slug: String
  def isActive: Boolean

  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}


trait EntityCompanion[E <: Entity] extends IdentifiableCompanion[E] {
  def nameLens: Lens[E, String]
  def slugLens: Lens[E, String]
  def isActiveLens: Lens[E, Boolean]
}