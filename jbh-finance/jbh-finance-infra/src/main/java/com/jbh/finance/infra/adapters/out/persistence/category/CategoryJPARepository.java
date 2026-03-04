package com.jbh.finance.infra.adapters.out.persistence.category;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.List;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class CategoryJPARepository implements PanacheRepository<CategoryJPAEntity> {

  private static final String SOURCE_PARAM = "source";
  private static final String ALIAS_PARAM = "alias";

  public List<CategoryJPAEntity> findBySource(final String source) {
    return find("source = :source", Sort.by(ALIAS_PARAM), Parameters.with(SOURCE_PARAM, source))
        .list();
  }

  public List<CategoryJPAEntity> findAllSystem() {
    return find("system IS TRUE", Sort.by(ALIAS_PARAM)).list();
  }
}
