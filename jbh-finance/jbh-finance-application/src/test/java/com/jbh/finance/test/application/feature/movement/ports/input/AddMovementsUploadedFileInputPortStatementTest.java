package com.jbh.finance.test.application.feature.movement.ports.input;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.movement.commands.UploadMovementsBatchCommand;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.movement.vo.MovementMetadataKey;
import com.jbh.finance.domain.movement.vo.MovementReviewVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.movement.vo.ProcessMovementOptionsVO;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.test.testfixtures.CategoryServiceMock;
import com.jbh.finance.test.testfixtures.builders.ProductEntityBuilder;
import com.jbh.finance.test.testfixtures.builders.StatementFixtures;
import com.jbh.finance.test.testfixtures.fakes.acid.RecordingUnitOfWork;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("unit")
class AddMovementsUploadedFileInputPortStatementTest {

  private static final Long GROCERY_CATEGORY_ID = 77L;
  private static final String GROCERY = "EXP_GROCERY";
  private static final String OTHER_INCOME = "INC_OTHER";
  private static final String UNKNOWN_EXPENSE = "UNK";
  private static final String DESCRIPTION = "TIENDA";

  private final UUID userId = UUID.randomUUID();
  private final ProductId productId = ProductId.generate();
  private final UUID importId = UUID.randomUUID();

  private ProductLifecycleService productService;
  private MovementWriterRepository movementRepo;
  private MonthlyBalanceSyncForUploadedMovements balanceSyncer;
  private AddMovementsUploadedFileInputPort inputPort;

  @BeforeEach
  void setUp() throws BusinessException {
    final ProductDTO product =
        ProductMapper.toDTO(
            ProductEntityBuilder.withBasicMovementForExisting(
                productId, userId, JBH_ZERO, JBH_ZERO));
    productService = mock(ProductLifecycleService.class);
    when(productService.findOrThrowByUserAndProductId(userId, productId)).thenReturn(product);
    when(productService.syncByUploadedMovements(any(), any(), any())).thenReturn(product);
    movementRepo = mock(MovementWriterRepository.class);
    balanceSyncer = mock(MonthlyBalanceSyncForUploadedMovements.class);
    inputPort =
        new AddMovementsUploadedFileInputPort(
            productService,
            movementRepo,
            new RecordingUnitOfWork(),
            balanceSyncer,
            new CategoryServiceMock());
  }

  @Test
  void shouldSaveCategoryDescriptionImportIdAndReviewWhenCommandCarriesThem()
      throws BusinessException {
    // Arrange
    final CategoryDTO grocery =
        StatementFixtures.category(CategorySourceVO.EXPENSE, GROCERY, GROCERY_CATEGORY_ID, "Market");
    final MovementReviewVO review = new MovementReviewVO(new BigDecimal("0.9"), false);
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(
            LocalDate.now(),
            MovementType.WITHDRAWAL,
            new BigDecimal("-50.00"),
            null,
            grocery,
            DESCRIPTION,
            importId,
            review);

    // Act
    inputPort.uploadMovementsFromFile(
        userId, productId, UploadMovementsBatchCommand.forStatement(List.of(command)));

    // Assert
    final MovementDTO saved = savedMovements().get(0);
    assertEquals(GROCERY, saved.category().getAlias());
    assertEquals(GROCERY_CATEGORY_ID, saved.category().getCategoryId());
    assertEquals(DESCRIPTION, saved.description());
    assertEquals(importId, saved.statementImportId());
    assertEquals(
        new BigDecimal("0.9"), saved.metadata().get(MovementMetadataKey.STATEMENT_CONFIDENCE));
    assertEquals(false, saved.metadata().get(MovementMetadataKey.STATEMENT_NEEDS_REVIEW));
    assertEquals(true, saved.metadata().get(MovementMetadataKey.FILE_IMPORT_TAG));
  }

  @Test
  void shouldUseOtherIncomeWhenDepositHasNoCategory() throws BusinessException {
    // Arrange
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(
            LocalDate.now(), MovementType.DEPOSIT, new BigDecimal("10.00"), null);

    // Act
    inputPort.uploadMovementsFromFile(
        userId, productId, UploadMovementsBatchCommand.forStatement(List.of(command)));

    // Assert
    final MovementDTO saved = savedMovements().get(0);
    assertEquals(OTHER_INCOME, saved.category().getAlias());
    assertNull(saved.description());
    assertNull(saved.statementImportId());
  }

  @Test
  void shouldUseUnknownExpenseWhenWithdrawalHasNoCategory() throws BusinessException {
    // Arrange
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(
            LocalDate.now(), MovementType.WITHDRAWAL, new BigDecimal("-10.00"), null);

    // Act
    inputPort.uploadMovementsFromFile(
        userId, productId, UploadMovementsBatchCommand.forStatement(List.of(command)));

    // Assert
    final MovementDTO saved = savedMovements().get(0);
    assertEquals(UNKNOWN_EXPENSE, saved.category().getAlias());
    assertNull(saved.description());
    assertNull(saved.statementImportId());
  }

  @Test
  void shouldSkipFundsCheckWhenBatchIsForStatement() throws BusinessException {
    // Arrange
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(
            LocalDate.now(), MovementType.DEPOSIT, new BigDecimal("10.00"), null);

    // Act
    inputPort.uploadMovementsFromFile(
        userId, productId, UploadMovementsBatchCommand.forStatement(List.of(command)));

    // Assert
    assertTrue(syncOptions().skipFundsCheck());
  }

  @Test
  void shouldKeepFundsCheckWhenUsingTheListMethod() throws BusinessException {
    // Arrange
    final List<AddMovementUploadedFileCommand> commands =
        List.of(
            new AddMovementUploadedFileCommand(
                LocalDate.now(), MovementType.DEPOSIT, new BigDecimal("10.00"), null));

    // Act
    inputPort.uploadMovementsFromFile(userId, productId, commands);

    // Assert
    assertFalse(syncOptions().skipFundsCheck());
  }

  private List<MovementDTO> savedMovements() {
    @SuppressWarnings("unchecked")
    final ArgumentCaptor<List<MovementDTO>> captor = ArgumentCaptor.forClass(List.class);
    verify(movementRepo).save(captor.capture());
    return captor.getValue();
  }

  private ProcessMovementOptionsVO syncOptions() throws BusinessException {
    final ArgumentCaptor<ProcessMovementOptionsVO> captor =
        ArgumentCaptor.forClass(ProcessMovementOptionsVO.class);
    verify(productService).syncByUploadedMovements(any(), any(), captor.capture());
    return captor.getValue();
  }
}
