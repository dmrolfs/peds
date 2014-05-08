package peds.commons.module


trait ModuleLifecycle {
  def start( ctx: Option[AnyRef] = None ): Unit = { }
  def stop( ctx: Option[AnyRef] = None ): Unit = { }
}
