package peds.commons.module


trait AuditedModule {
  type AuditContext <: Serializable
}
