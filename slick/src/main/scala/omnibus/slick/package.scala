package omnibus


package object slick extends CommonTypeMappers {
  case class OptimisticLockException( 
    message: String, 
    parent: Throwable = null 
  ) extends RuntimeException( message, parent ) with Serializable
}
