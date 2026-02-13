package com.jbh.finance.domain.movement.vo;

import com.jbh.finance.domain.product.ProductDomain;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public final class MovementMetadata {

  private final Map<MovementMetadataKey, Object> data;

  private MovementMetadata(final Map<MovementMetadataKey, Object> data) {
    this.data =
        (data != null && !data.isEmpty())
            ? new EnumMap<>(data)
            : new EnumMap<>(MovementMetadataKey.class);
  }

  public static MovementMetadata createEmpty() {
    return new MovementMetadata(new EnumMap<>(MovementMetadataKey.class));
  }

  public static MovementMetadata of(final Map<MovementMetadataKey, Object> data) {
    return new MovementMetadata(data);
  }

  public Map<MovementMetadataKey, Object> asMap() {
    return data;
  }

  public boolean hasKey(final MovementMetadataKey key) {
    return data.containsKey(key);
  }

  public Object get(final MovementMetadataKey key) {
    return data.get(key);
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public void putTargetInternalAccount(final ProductDomain accountDomain) {
    put(MovementMetadataKey.TARGET_INTERNAL_ACCOUNT_ID, accountDomain.getId());
    put(MovementMetadataKey.TARGET_INTERNAL_ACCOUNT_NAME, accountDomain.getName());
  }

  private void put(final MovementMetadataKey key, final Object value) {
    data.put(key, value);
  }

  public void putInvestmentIncomeAccount(final ProductDomain accountDomain) {
    put(MovementMetadataKey.INVESTMENT_INCOME_ACCOUNT, accountDomain.getName());
  }

  public void putFileImportedAt(final LocalDateTime importedAt) {
    put(MovementMetadataKey.FILE_IMPORTED_AT_TAG, importedAt);
    put(MovementMetadataKey.FILE_IMPORT_TAG, true);
  }
}
