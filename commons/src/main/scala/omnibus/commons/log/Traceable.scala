package omnibus.commons.log


//@deprecated( "use Journal Logger.debug", "0.63" )
trait Traceable[L] {
  /** Determine whether trace logging is enabled.
   */
  def isEnabled( logger: L ): Boolean

  /** Issue a trace logging message, with an exception. 
   * @param msg the message object. toString() is called to convert it to a loggable string.
   * @param t the exception to include with the logged message. 
   */
  def trace( logger: L, msg: ⇒ Any, t: ⇒ Throwable ): Unit 

  /** Issue a trace logging message. 
   * @param msg the message object. toString() is called to convert it to a loggable string.
   */   
  def trace( logger: L, msg: ⇒ Any ): Unit
}