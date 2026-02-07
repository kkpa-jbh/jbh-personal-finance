package com.jbh.products.domain.movement.vo;

import com.jbh.products.domain.product.ProductDomain;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public final class AccountMovementMetadata {

  private final Map<AccountMovementMetadataKey, Object> data;

  private AccountMovementMetadata(final Map<AccountMovementMetadataKey, Object> data) {
    this.data =
        (data != null) ? new EnumMap<>(data) : new EnumMap<>(AccountMovementMetadataKey.class);
  }

  public static AccountMovementMetadata createEmpty() {
    return new AccountMovementMetadata(new EnumMap<>(AccountMovementMetadataKey.class));
  }

  public static AccountMovementMetadata of(final Map<AccountMovementMetadataKey, Object> data) {
    return new AccountMovementMetadata(data);
  }

  public Map<AccountMovementMetadataKey, Object> asMap() {
    return data;
  }

  public boolean hasKey(final AccountMovementMetadataKey key) {
    return data.containsKey(key);
  }

  public Object get(final AccountMovementMetadataKey key) {
    return data.get(key);
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public void putTargetInternalAccount(final ProductDomain accountDomain) {
    put(AccountMovementMetadataKey.TARGET_INTERNAL_ACCOUNT_ID, accountDomain.getId());
    put(AccountMovementMetadataKey.TARGET_INTERNAL_ACCOUNT_NAME, accountDomain.getName());
  }

  private void put(final AccountMovementMetadataKey key, final Object value) {
    data.put(key, value);
  }

  public void putInvestmentIncomeAccount(final ProductDomain accountDomain) {
    put(AccountMovementMetadataKey.INVESTMENT_INCOME_ACCOUNT, accountDomain.getName());
  }

  public void putFileImportedAt(final LocalDateTime importedAt) {
    put(AccountMovementMetadataKey.FILE_IMPORTED_AT_TAG, importedAt);
    put(AccountMovementMetadataKey.FILE_IMPORT_TAG, true);
  }
}
