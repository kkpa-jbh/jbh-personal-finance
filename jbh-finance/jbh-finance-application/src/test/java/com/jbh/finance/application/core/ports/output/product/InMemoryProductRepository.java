package com.jbh.finance.application.core.ports.output.product;

import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryProductRepository implements ProductRepository {

  private static final Logger log = LoggerFactory.getLogger(InMemoryProductRepository.class);
  private final Map<UUID, ProductDTO> storage = new HashMap<>();

  @Override
  public Optional<ProductDTO> findByUserAndProductId(final UUID userId, final ProductId accountId) {
    final ProductDTO account = storage.get(accountId.value());
    if (account != null && account.userId().equals(userId)) {
      return Optional.of(account);
    }
    /*
    productDTO =
        AccountDTO.defaultBuilder(userId, productId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE)
            .currentBalance(JBH_ZERO)
            .movementBalance(JBH_ZERO)
            .build();
    storage.put(productId.value(), productDTO);

     */
    return Optional.empty();
  }

  @Override
  public Optional<ProductDTO> findByProductId(final ProductId accountId) {
    return Optional.ofNullable(storage.get(accountId.value()));
  }

  @Override
  public List<ProductDTO> findActiveByUserId(final UUID userId) {
    return storage.values().stream()
        .filter(product -> product.userId().equals(userId) && product.isActive())
        .toList();
  }

  @Override
  public ProductDTO save(final ProductDTO account) {
    storage.put(account.id().value(), account);
    log.warn("Updated Account " + account);
    return account;
  }

  @Override
  public void deleteById(final ProductId productId) {
    storage.remove(productId.value());
    log.warn("Deleted product with ID: " + productId);
  }

  public void clearStorage() {
    storage.clear();
  }

  public int size() {
    return storage.size();
  }
}
