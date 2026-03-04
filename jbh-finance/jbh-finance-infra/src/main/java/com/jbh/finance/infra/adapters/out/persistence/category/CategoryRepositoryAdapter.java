package com.jbh.finance.infra.adapters.out.persistence.category;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class CategoryRepositoryAdapter implements CategoryQueryRepo {

  private final CategoryJPARepository jpaRepository;

  @Inject
  public CategoryRepositoryAdapter(final CategoryJPARepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  /**
   * INCLUDES THE INITIAL BALANCE USED INTERNALLY (INACTIVE)
   * @return
   */
  @Override
  public List<CategoryDTO> findAllSystemCategories() {
    return jpaRepository.findAllSystem().stream().map(CategoryJPAEntity::toDTO).toList();
  }
}
