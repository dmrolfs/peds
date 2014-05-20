package peds.archetype.domain.model.core

// import shapeless.Lens


trait Identifiable {
  type That <: Identifiable

  type ID
  def idClass: Class[_]

  def id: ID
  // def idLens: Lens[this.type, ID]
  // def idLens: Lens[That, ID]
}
