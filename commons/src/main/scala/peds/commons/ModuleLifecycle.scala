package peds.commons.util


trait ModuleLifecycle {
  def start( ctx: AnyRef ): Unit = { }
  def stop( ctx: AnyRef ): Unit = { }
}
