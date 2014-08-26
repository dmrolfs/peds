package peds.commons.module


trait ModuleLifecycle {
  def start( moduleContext: Map[Symbol, Any] ): Unit = { }
  def stop( moduleContext: Map[Symbol, Any] ): Unit = { }
}
