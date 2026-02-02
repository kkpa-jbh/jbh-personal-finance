package com.jbh.notification.infra.adapters.out.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.preferences.contracts.PreferencesLookupException;
import com.jbh.preferences.contracts.PreferencesLookupPort;
import com.jbh.preferences.contracts.UserPreferencesData;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectPreferencesAdapterTest {

  @Mock
  private PreferencesLookupPort preferencesLookupPort;

  private DirectPreferencesAdapter adapter;

  private static final UUID USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    adapter = new DirectPreferencesAdapter(preferencesLookupPort);
  }

  @Test
  void getUserPreferences_shouldDelegateToPreferencesLookupPort() throws Exception {
    final UserPreferencesData expectedData = createTestData();
    when(preferencesLookupPort.getPreferences(USER_ID)).thenReturn(expectedData);

    final UserPreferencesData result = adapter.getUserPreferences(USER_ID);

    assertThat(result).isEqualTo(expectedData);
    verify(preferencesLookupPort).getPreferences(USER_ID);
  }

  @Test
  void getUserPreferences_shouldPropagateException_whenLookupFails() throws Exception {
    when(preferencesLookupPort.getPreferences(USER_ID))
        .thenThrow(new PreferencesLookupException("Lookup failed", USER_ID));

    assertThatThrownBy(() -> adapter.getUserPreferences(USER_ID))
        .isInstanceOf(PreferencesLookupException.class)
        .hasMessageContaining("Lookup failed");
  }

  @Test
  void getUserLanguage_shouldDelegateToPreferencesLookupPort() {
    when(preferencesLookupPort.getLanguageCode(USER_ID)).thenReturn("en");

    final String result = adapter.getUserLanguage(USER_ID);

    assertThat(result).isEqualTo("en");
    verify(preferencesLookupPort).getLanguageCode(USER_ID);
  }

  @Test
  void getUserLanguage_shouldReturnLanguageFromPort() {
    when(preferencesLookupPort.getLanguageCode(USER_ID)).thenReturn("es");

    final String result = adapter.getUserLanguage(USER_ID);

    assertThat(result).isEqualTo("es");
  }

  private UserPreferencesData createTestData() {
    return new UserPreferencesData(
        USER_ID,
        "en",
        "USD",
        new BigDecimal("500.00"),
        UUID.randomUUID());
  }
}
