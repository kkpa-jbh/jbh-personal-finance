package com.jbh.preferences.infra.adapters.out.intermodule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.ports.input.GetTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.GetUserPreferencesInputPort;
import com.jbh.preferences.contracts.PreferencesLookupException;
import com.jbh.preferences.contracts.UserPreferencesData;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreferencesLookupAdapterTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PRODUCT_ID = UUID.randomUUID();
  @Mock private GetUserPreferencesInputPort getUserPreferencesInputPort;
  @Mock private GetTeamPreferencesInputPort getTeamPreferencesInputPort;
  private PreferencesLookupAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter =
        new PreferencesLookupAdapter(getUserPreferencesInputPort, getTeamPreferencesInputPort);
  }

  @Test
  void getPreferences_shouldReturnMappedData_whenPreferencesExist() throws Exception {
    final UserPreferencesDTO dto = createTestDto();
    when(getUserPreferencesInputPort.execute(USER_ID)).thenReturn(dto);

    final UserPreferencesData result = adapter.getPreferences(USER_ID);

    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo(USER_ID);
    assertThat(result.languageCode()).isEqualTo("en");
    assertThat(result.currencyCode()).isEqualTo("USD");
    assertThat(result.savingsGoal()).isEqualTo(new BigDecimal("1000.00"));
    assertThat(result.defaultAccountId()).isEqualTo(PRODUCT_ID);
  }

  private UserPreferencesDTO createTestDto() {
    return UserPreferencesDTO.builder()
        .userId(USER_ID)
        .defaultLang(Language.ENGLISH)
        .defaultCurrency(Currency.USD)
        .savingsGoal(new BigDecimal("1000.00"))
        .defaultProductId(PRODUCT_ID)
        .build();
  }

  @Test
  void getPreferences_shouldThrowPreferencesLookupException_whenBusinessExceptionOccurs()
      throws Exception {
    when(getUserPreferencesInputPort.execute(USER_ID))
        .thenThrow(
            new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_NOT_FOUND, USER_ID));

    assertThatThrownBy(() -> adapter.getPreferences(USER_ID))
        .isInstanceOf(PreferencesLookupException.class)
        .hasMessageContaining("Failed to retrieve preferences for user")
        .hasCauseInstanceOf(BusinessException.class);
  }

  @Test
  void getPreferences_exceptionShouldContainUserId_whenThrown() throws Exception {
    when(getUserPreferencesInputPort.execute(USER_ID))
        .thenThrow(
            new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_NOT_FOUND, USER_ID));

    try {
      adapter.getPreferences(USER_ID);
    } catch (final PreferencesLookupException e) {
      assertThat(e.getUserId()).isEqualTo(USER_ID);
      assertThat(e.hasUserId()).isTrue();
    }
  }

  @Test
  void findPreferences_shouldReturnOptionalWithData_whenPreferencesExist() throws Exception {
    final UserPreferencesDTO dto = createTestDto();
    when(getUserPreferencesInputPort.execute(USER_ID)).thenReturn(dto);

    final Optional<UserPreferencesData> result = adapter.findPreferences(USER_ID);

    assertThat(result).isPresent();
    assertThat(result.get().userId()).isEqualTo(USER_ID);
    assertThat(result.get().languageCode()).isEqualTo("en");
  }

  @Test
  void findPreferences_shouldReturnEmpty_whenBusinessExceptionOccurs() throws Exception {
    when(getUserPreferencesInputPort.execute(USER_ID))
        .thenThrow(
            new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_NOT_FOUND, USER_ID));

    final Optional<UserPreferencesData> result = adapter.findPreferences(USER_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void getLanguageCode_shouldReturnLanguage_whenPreferencesExist() throws Exception {
    final UserPreferencesDTO dto = createTestDto();
    when(getUserPreferencesInputPort.execute(USER_ID)).thenReturn(dto);

    final String result = adapter.getLanguageCode(USER_ID);

    assertThat(result).isEqualTo("en");
  }

  @Test
  void getLanguageCode_shouldReturnDefault_whenExceptionOccurs() throws Exception {
    when(getUserPreferencesInputPort.execute(USER_ID))
        .thenThrow(
            new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_NOT_FOUND, USER_ID));

    final String result = adapter.getLanguageCode(USER_ID);

    assertThat(result).isEqualTo("es");
  }
}
