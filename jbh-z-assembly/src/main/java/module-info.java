module jbh.z.assembly {
  requires org.jboss.logging;

  requires jakarta.cdi;   // ✅ If assembly has any CDI annotations
  requires jakarta.inject;   // ✅ Basic injection (@Inject)
  requires quarkus.core; // ✅ Quarkus runtime + CDI container
  
}