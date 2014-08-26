package peds.commons.module


trait ModuleLifecycle {
  def start( moduleContext: Map[Symbol, Any] = Map() ): Unit = { }
  def stop( moduleContext: Map[Symbol, Any] = Map() ): Unit = { }
}
