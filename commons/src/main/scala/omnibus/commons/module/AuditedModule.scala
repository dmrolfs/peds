package omnibus.commons.module


trait AuditedModule {
  type AuditContext <: Serializable
}
