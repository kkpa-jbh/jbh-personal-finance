package adapters;

import com.jbh.account.infra.adapters.in.service.ExcelMovementReaderService;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExcelMovementReaderServiceTest {

  private final ExcelMovementReaderService excelMovementReaderService =
      new ExcelMovementReaderService();

  @BeforeEach
  public void setup() {}

  @Test
  public void shouldTransformAmountProperly() {
    final String amount = "10139234.00";

    final AtomicReference<BigDecimal> result = new AtomicReference<>();
    Assertions.assertDoesNotThrow(() -> result.set(excelMovementReaderService.parseAmount(amount)));
    ;

    Assertions.assertEquals(new BigDecimal(amount), result.get());
  }
}
