package peds.commons.repository


trait Criteria[E] {
  type Query
  type Predicate

  def filter: Option[Predicate]
  def sortBy[T}: Option[E => T]
  def pageStart: Int
  def pageSize: Int
}
