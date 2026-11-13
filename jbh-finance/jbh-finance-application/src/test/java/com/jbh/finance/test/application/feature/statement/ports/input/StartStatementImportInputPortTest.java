package com.jbh.finance.test.application.feature.statement.ports.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.statement.commands.StartStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSettings;
import com.jbh.finance.application.feature.statement.ports.input.StartStatementImportInputPort;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.testfixtures.builders.StatementFixtures;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@Tag("unit")
class StartStatementImportInputPortTest {

  private static final Duration STALE_AFTER = Duration.ofMinutes(10);

  private final UUID userId = UUID.randomUUID();
  private final ProductId productId = ProductId.generate();
  private final PdfSha256 sha = StatementFixtures.SHA;

  private ProductLifecycleService productService;
  private StatementImportRepository repository;
  private StartStatementImportInputPort inputPort;

  @BeforeEach
  void setUp() {
    productService = mock(ProductLifecycleService.class);
    repository = mock(StatementImportRepository.class);
    inputPort =
        new StartStatementImportInputPort(
            productService, repository, new StatementImportSettings(STALE_AFTER));
  }

  @ParameterizedTest
  @EnumSource(
      value = ProductType.class,
      names = {"SAVINGS", "CREDIT_CARD"})
  void shouldReturnImportIdWhenProductTypeAllowsStatementsAndInsertWins(final ProductType type)
      throws BusinessException {
    // Arrange
    stubProduct(type);
    when(repository.insertProcessing(any())).thenReturn(true);

    // Act
    final UUID importId = inputPort.start(command());

    // Assert
    final ArgumentCaptor<NewStatementImportDTO> captor =
        ArgumentCaptor.forClass(NewStatementImportDTO.class);
    final InOrder inOrder = inOrder(productService, repository);
    inOrder.verify(productService).findOrThrowByUserAndProductId(userId, productId);
    inOrder.verify(repository).markStaleAsFailed(productId, sha, STALE_AFTER);
    inOrder.verify(repository).insertProcessing(captor.capture());
    assertNotNull(importId);
    assertEquals(importId, captor.getValue().id());
    assertEquals(userId, captor.getValue().userId());
    assertEquals(productId, captor.getValue().productId());
    assertEquals(sha, captor.getValue().pdfSha256());
  }

  @ParameterizedTest
  @EnumSource(
      value = ProductType.class,
      names = {"CDT", "LOAN", "INVESTMENT", "REAL_ESTATE_INVESTMENT"})
  void shouldThrowTypeNotAllowedWhenProductTypeCannotImportStatements(final ProductType type)
      throws BusinessException {
    // Arrange
    stubProduct(type);

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.start(command()));

    // Assert
    assertEquals(
        BusinessApplicationExceptionType.STATEMENT_PRODUCT_TYPE_NOT_ALLOWED,
        exception.getBusinessExceptionType());
    verifyNoInteractions(repository);
  }

  @Test
  void shouldThrowAlreadyImportedWhenInsertFindsDuplicate() throws BusinessException {
    // Arrange
    stubProduct(ProductType.SAVINGS);
    when(repository.insertProcessing(any())).thenReturn(false);

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.start(command()));

    // Assert
    assertEquals(
        BusinessApplicationExceptionType.STATEMENT_ALREADY_IMPORTED,
        exception.getBusinessExceptionType());
    verify(repository).insertProcessing(any());
  }

  @Test
  void shouldRethrowWhenProductIsNotFoundForUser() throws BusinessException {
    // Arrange
    final BusinessException notFound =
        new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND);
    when(productService.findOrThrowByUserAndProductId(userId, productId)).thenThrow(notFound);

    // Act
    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.start(command()));

    // Assert
    assertSame(notFound, exception);
    verifyNoInteractions(repository);
  }

  @Test
  void shouldThrowWhenCommandIsNull() {
    // Act & Assert
    assertThrows(GenericSpecificationException.class, () -> inputPort.start(null));
  }

  private StartStatementImportCommand command() {
    return new StartStatementImportCommand(userId, productId, sha);
  }

  private void stubProduct(final ProductType type) throws BusinessException {
    final ProductDTO product = ProductDTO.defaultBuilder(userId, productId, "Product", type).build();
    when(productService.findOrThrowByUserAndProductId(userId, productId)).thenReturn(product);
  }
}
