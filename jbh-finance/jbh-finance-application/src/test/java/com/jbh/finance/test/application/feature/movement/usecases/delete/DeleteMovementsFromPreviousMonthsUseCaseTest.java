package com.jbh.finance.test.application.feature.movement.usecases.delete;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder.delayTests;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.GeneralCommandFixtureBuilder;
import com.jbh.finance.test.testfixtures.utils.MonthlyBalanceITUtils;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for {@link DeleteMovementUseCase} covering cross-month reversal scenarios
 * within the same calendar year.
 *
 * <h2>Business Intent Under Test</h2>
 *
 * <p>When a movement is deleted, the system must:
 *
 * <ol>
 *   <li>Reverse the movement's effect on the <strong>product</strong> balances immediately
 *       (synchronous).
 *   <li>Reverse the movement's effect on the <strong>monthly balance of the movement's own
 *       period</strong> immediately (synchronous).
 *   <li>Propagate the corrected closing balance as the opening balance of every <strong>subsequent
 *       month</strong> up to the current period (asynchronous cascade).
 * </ol>
 *
 * <h2>Base-Data Setup (Orders 0–8)</h2>
 *
 * <p>A savings account is created and 8 movements are spread across three months within the same
 * year (January, February, March). January and February are previous months; March is the current
 * month at run time.
 *
 * <pre>
 * Jan  1 : +$1,000.00  Initial Balance
 * Jan 10 : +$  500.00  Income (Deposit)
 * Jan 20 : -$  200.00  Personal Expense   ← deleted at Order 11 (oldest-month deletion)
 * Feb  5 : +$  300.00  Income (Deposit)
 * Feb 15 : -$  100.00  Personal Expense   ← deleted at Order 21 (second Feb deletion)
 * Feb 25 : +$  200.00  Income (Deposit)   ← deleted at Order 20 (first Feb deletion)
 * Mar  5 : +$  400.00  Income (Deposit)
 * Mar 15 : -$  150.00  Personal Expense   ← deleted at Order 10 (current-month deletion, simplest case)
 * </pre>
 *
 * <h2>Cumulative State After All Base Data</h2>
 *
 * <pre>
 * Product:
 *   movementBalance  = $1,950  (1000+500−200+300−100+200+400−150)
 *   currentBalance   = $1,950
 *   netProfitBalance = $0
 *
 * January MB (async-settled):
 *   openingBalance  = $0
 *   totalDebits     = $1,500  (1000+500)
 *   totalCredits    = $  200
 *   movementBalance = $1,300
 *   closingBalance  = $1,300
 *   totalMovements  = 3
 *
 * February MB (async-settled):
 *   openingBalance  = $1,300  ← Jan closing
 *   totalDebits     = $  500  (300+200)
 *   totalCredits    = $  100
 *   movementBalance = $  400
 *   closingBalance  = $1,700
 *   totalMovements  = 3
 *
 * March MB (async-settled):
 *   openingBalance  = $1,700  ← Feb closing
 *   totalDebits     = $  400
 *   totalCredits    = $  150
 *   movementBalance = $  250
 *   closingBalance  = $1,950
 *   totalMovements  = 2
 * </pre>
 *
 * <h2>Deletion Test Scenarios</h2>
 *
 * <ul>
 *   <li><strong>Order 10</strong> – Delete the <em>March</em> expense (−$150): only March MB
 *       changes. January and February are untouched.
 *   <li><strong>Order 11</strong> – Delete the <em>January</em> expense (−$200): January MB
 *       changes AND the cascade propagates the new January closing to February's opening, then to
 *       March's opening (two-level cascade).
 *   <li><strong>Order 20</strong> – Delete the <em>February</em> income (+$200): February MB
 *       changes AND March's opening is updated (one-level cascade). January is untouched.
 *   <li><strong>Order 21</strong> – Delete the <em>February</em> expense (−$100): a second
 *       deletion in February. February and March are both updated again. January still untouched.
 * </ul>
 *
 * <h2>Important Constraint: canBeRemoved()</h2>
 *
 * <p>{@link com.jbh.finance.application.feature.movement.dto.MovementDTO#canBeRemoved()} checks
 * {@code createdAt}, NOT {@code movementDate}. Because all movements in this test are recorded
 * in the <em>current run session</em> (createdAt = now), they are all eligible for deletion even
 * though their {@code movementDate} falls in January or February.
 *
 * <h2>Design Notes</h2>
 *
 * <ul>
 *   <li>Tests are ordered and stateful – each test builds on the previous one's in-memory state.
 *   <li>All shared state (captured DTOs, products, movements) is stored in static fields.
 *   <li>Extra {@code delayTests()} calls are used after cross-month deletions to allow the async
 *       cascade ({@code adjustCurrentAndNextMonthlyBalancesAsync}) to complete before asserting
 *       the opening balances of subsequent months.
 *   <li>No official monthly reports are registered; all monthly balances remain in calculated
 *       (non-locked) state throughout.
 * </ul>
 */
@TestMethodOrder(OrderAnnotation.class)
public class DeleteMovementsFromPreviousMonthsUseCaseTest {

  // ─── Period constants ──────────────────────────────────────────────────────
  // The test is designed to run any time during or after the third month of
  // a given year (Jan and Feb movements must be in the past).
  static final YearMonth CURRENT_PERIOD = YearMonth.of(2026, Month.MARCH);
  static final YearMonth JANUARY = YearMonth.of(CURRENT_PERIOD.getYear(), 1);
  static final YearMonth FEBRUARY = YearMonth.of(CURRENT_PERIOD.getYear(), 2);
  static final YearMonth MARCH = CURRENT_PERIOD; // = YearMonth.now() when running in March

  static final UUID userId = UUID.randomUUID();

  // ─── Shared state: built up across ordered tests ───────────────────────────
  private static ProductDTO productDTO;

  // Movements captured during setup so they can be deleted in later tests
  private static MovementDTO janExpenseToReverse; // Jan 20 : −$200
  private static MovementDTO febExpenseToReverse; // Feb 15 : −$100
  private static MovementDTO febIncomeToReverse; // Feb 25 : +$200
  private static MovementDTO marExpenseToReverse; // Mar 15 : −$150

  // State snapshots captured just before certain deletions
  private static MonthlyBalanceDTO janMBSnapshotBeforeDeletion;

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  // ─── Use-case / service handles (re-created in @BeforeEach) ────────────────
  private DeleteMovementUseCase deleteMovementUseCase;
  private CreateProductUseCase createProductUseCase;
  private AddMovementUseCase addMovementUseCase;
  private MonthlyBalanceLifecycleService monthlyBalanceLifecycleSrv;
  private ProductLifecycleService productLifecycleSrv;
  private MovementLifecycleService movementLifecycleSrv;

  // ═══════════════════════════════════════════════════════════════════════════
  // Lifecycle
  // ═══════════════════════════════════════════════════════════════════════════

  @BeforeAll
  static void beforeAll() {
    UseCaseFixtureBuilder.resetState();
  }

  @BeforeEach
  void setup() {
    deleteMovementUseCase = UseCaseFixtureBuilder.buildDeleteMovementUseCase();
    createProductUseCase = UseCaseFixtureBuilder.buildCreateProductUseCase();
    addMovementUseCase = UseCaseFixtureBuilder.buildAddMovementUseCase();
    monthlyBalanceLifecycleSrv = UseCaseFixtureBuilder.buildMonthlyBalanceLifecycleSrv();
    productLifecycleSrv = UseCaseFixtureBuilder.buildProductLifecycleSrv();
    movementLifecycleSrv = UseCaseFixtureBuilder.buildMovementLifeCycleSrv();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PHASE 1 – BASE DATA SETUP  (Orders 0 – 8)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Order 0 – Create the savings account.
   *
   * <p>Expected product state:
   *
   * <pre>
   * movementBalance  = $0
   * currentBalance   = $0
   * netProfitBalance = $0
   * </pre>
   */
  @Test
  @Order(0)
  void createSavingsAccount() throws BusinessException {
    productDTO =
        createProductUseCase.execute(GeneralCommandFixtureBuilder.createSavingAccountCommand(userId));

    assertNotNull(productDTO, "Product must be created");
    assertNotNull(productDTO.id(), "Product ID must be assigned");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        productDTO.movementBalance(),
        "New product movementBalance must be $0");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        productDTO.currentBalance(),
        "New product currentBalance must be $0");
  }

  /**
   * Order 1 – Initial balance entry on <strong>January 1st</strong>.
   *
   * <p>Movement: Jan 1 | DEPOSIT | +$1,000.00 | INCOME_INITIAL_BALANCE
   *
   * <p>Expected product state after:
   *
   * <pre>
   * movementBalance = $1,000
   * currentBalance  = $1,000
   * </pre>
   *
   * <p>Expected January MB (after async settle):
   *
   * <pre>
   * openingBalance  = $0
   * totalDebits     = $1,000
   * totalCredits    = $0
   * movementBalance = $1,000
   * closingBalance  = $1,000
   * totalMovements  = 1
   * </pre>
   */
  @Test
  @Order(1)
  void addInitialBalanceJanuary() throws BusinessException {
    addMovementUseCase.addMovement(
        userId,
        productDTO.id(),
        AddMovementCommandFixtureBuilder.createInitialBalance(
            JANUARY.atDay(1), new BigDecimal("1000.00")));

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("1000")),
        product.movementBalance(),
        "Product movementBalance after initial deposit");
    assertEquals(
        withJBHDecimals(new BigDecimal("1000")),
        product.currentBalance(),
        "Product currentBalance after initial deposit");
  }

  private ProductDTO getCurrentProductInfo() {
    return productLifecycleSrv.findProductById(productDTO.id()).get();
  }

  /**
   * Order 2 – Income on <strong>January 10th</strong>.
   *
   * <p>Movement: Jan 10 | DEPOSIT | +$500.00 | INCOME_DEPOSIT
   *
   * <p>Cumulative product state:
   *
   * <pre>
   * movementBalance = $1,500
   * currentBalance  = $1,500
   * </pre>
   *
   * <p>Cumulative January MB:
   *
   * <pre>
   * totalDebits     = $1,500  (1000+500)
   * totalCredits    = $0
   * movementBalance = $1,500
   * closingBalance  = $1,500
   * totalMovements  = 2
   * </pre>
   */
  @Test
  @Order(2)
  void addIncomeJanuary() throws BusinessException {
    addMovementUseCase.addMovement(
        userId,
        productDTO.id(),
        AddMovementCommandFixtureBuilder.createDepositIncome(
            JANUARY.atDay(10), new BigDecimal("500.00")));

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(withJBHDecimals(new BigDecimal("1500")), product.movementBalance());
    assertEquals(withJBHDecimals(new BigDecimal("1500")), product.currentBalance());
  }

  /**
   * Order 3 – Personal expense on <strong>January 20th</strong>.
   *
   * <p><em>This movement is the target for the cross-month deletion test at Order 11.</em>
   *
   * <p>Movement: Jan 20 | WITHDRAWAL | −$200.00 | PERSONAL
   *
   * <p>Cumulative product state:
   *
   * <pre>
   * movementBalance = $1,300  (was $1,500 − $200)
   * currentBalance  = $1,300
   * </pre>
   *
   * <p>Cumulative January MB:
   *
   * <pre>
   * totalDebits     = $1,500
   * totalCredits    = $  200
   * movementBalance = $1,300
   * closingBalance  = $1,300
   * totalMovements  = 3
   * </pre>
   */
  @Test
  @Order(3)
  void addJanuaryExpense_capturedForLaterReversal() throws BusinessException {
    final AddMovementResultDTO result =
        addMovementUseCase.addMovement(
            userId,
            productDTO.id(),
            AddMovementCommandFixtureBuilder.createPersonalExpense(
                JANUARY.atDay(20), new BigDecimal("200.00")));

    janExpenseToReverse = result.movement();
    assertNotNull(janExpenseToReverse, "January expense movement must be captured");

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("1300")),
        product.movementBalance(),
        "movementBalance: 1500 − 200 = 1300");
    assertEquals(withJBHDecimals(new BigDecimal("1300")), product.currentBalance());
  }

  /**
   * Order 4 – Income on <strong>February 5th</strong>.
   *
   * <p>Movement: Feb 5 | DEPOSIT | +$300.00 | INCOME_DEPOSIT
   *
   * <p>Cumulative product state:
   *
   * <pre>
   * movementBalance = $1,600
   * currentBalance  = $1,600
   * </pre>
   *
   * <p>Expected February MB (after async propagation from Jan):
   *
   * <pre>
   * openingBalance  = $1,300  ← Jan closing
   * totalDebits     = $  300
   * totalCredits    = $    0
   * movementBalance = $  300
   * closingBalance  = $1,600
   * totalMovements  = 1
   * </pre>
   */
  @Test
  @Order(4)
  void addFirstIncomeFebruary() throws BusinessException {
    addMovementUseCase.addMovement(
        userId,
        productDTO.id(),
        AddMovementCommandFixtureBuilder.createDepositIncome(
            FEBRUARY.atDay(5), new BigDecimal("300.00")));

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(withJBHDecimals(new BigDecimal("1600")), product.movementBalance());
    assertEquals(withJBHDecimals(new BigDecimal("1600")), product.currentBalance());
  }

  /**
   * Order 5 – Personal expense on <strong>February 15th</strong>.
   *
   * <p><em>This movement is the target for the second February deletion at Order 21.</em>
   *
   * <p>Movement: Feb 15 | WITHDRAWAL | −$100.00 | PERSONAL
   *
   * <p>Cumulative product state:
   *
   * <pre>
   * movementBalance = $1,500
   * currentBalance  = $1,500
   * </pre>
   *
   * <p>Cumulative February MB:
   *
   * <pre>
   * totalDebits     = $300
   * totalCredits    = $100
   * movementBalance = $200
   * closingBalance  = $1,500
   * totalMovements  = 2
   * </pre>
   */
  @Test
  @Order(5)
  void addFebruaryExpense_capturedForLaterReversal() throws BusinessException {
    final AddMovementResultDTO result =
        addMovementUseCase.addMovement(
            userId,
            productDTO.id(),
            AddMovementCommandFixtureBuilder.createPersonalExpense(
                FEBRUARY.atDay(15), new BigDecimal("100.00")));

    febExpenseToReverse = result.movement();
    assertNotNull(febExpenseToReverse, "February expense movement must be captured");

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(withJBHDecimals(new BigDecimal("1500")), product.movementBalance());
    assertEquals(withJBHDecimals(new BigDecimal("1500")), product.currentBalance());
  }

  /**
   * Order 6 – Second income on <strong>February 25th</strong>.
   *
   * <p><em>This movement is the target for the first February deletion at Order 20.</em>
   *
   * <p>Movement: Feb 25 | DEPOSIT | +$200.00 | INCOME_DEPOSIT
   *
   * <p>Cumulative product state:
   *
   * <pre>
   * movementBalance = $1,700
   * currentBalance  = $1,700
   * </pre>
   *
   * <p>Cumulative February MB:
   *
   * <pre>
   * totalDebits     = $  500  (300+200)
   * totalCredits    = $  100
   * movementBalance = $  400
   * closingBalance  = $1,700
   * totalMovements  = 3
   * </pre>
   */
  @Test
  @Order(6)
  void addSecondFebruaryIncome_capturedForLaterReversal() throws BusinessException {
    final AddMovementResultDTO result =
        addMovementUseCase.addMovement(
            userId,
            productDTO.id(),
            AddMovementCommandFixtureBuilder.createDepositIncome(
                FEBRUARY.atDay(25), new BigDecimal("200.00")));

    febIncomeToReverse = result.movement();
    assertNotNull(febIncomeToReverse, "February income movement must be captured");

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(withJBHDecimals(new BigDecimal("1700")), product.movementBalance());
    assertEquals(withJBHDecimals(new BigDecimal("1700")), product.currentBalance());
  }

  /**
   * Order 7 – Income on <strong>March 5th</strong>.
   *
   * <p>Movement: Mar 5 | DEPOSIT | +$400.00 | INCOME_DEPOSIT
   *
   * <p>Cumulative product state:
   *
   * <pre>
   * movementBalance = $2,100
   * currentBalance  = $2,100
   * </pre>
   *
   * <p>Expected March MB (after async propagation from Feb):
   *
   * <pre>
   * openingBalance  = $1,700  ← Feb closing
   * totalDebits     = $  400
   * totalCredits    = $    0
   * movementBalance = $  400
   * closingBalance  = $2,100
   * totalMovements  = 1
   * </pre>
   */
  @Test
  @Order(7)
  void addIncomeMarch() throws BusinessException {
    addMovementUseCase.addMovement(
        userId,
        productDTO.id(),
        AddMovementCommandFixtureBuilder.createDepositIncome(
            MARCH.atDay(5), new BigDecimal("400.00")));

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(withJBHDecimals(new BigDecimal("2100")), product.movementBalance());
    assertEquals(withJBHDecimals(new BigDecimal("2100")), product.currentBalance());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PHASE 1 FINAL VERIFICATION  (Order 9)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Order 8 – Personal expense on <strong>March 15th</strong>.
   *
   * <p><em>This movement is the first to be deleted (Order 10) – the simplest same-month case.</em>
   *
   * <p>Movement: Mar 15 | WITHDRAWAL | −$150.00 | PERSONAL
   *
   * <p>Final cumulative product state (end of setup):
   *
   * <pre>
   * movementBalance = $1,950
   * currentBalance  = $1,950
   * netProfitBalance = $0
   * </pre>
   *
   * <p>Final March MB:
   *
   * <pre>
   * totalDebits     = $  400
   * totalCredits    = $  150
   * movementBalance = $  250
   * closingBalance  = $1,950
   * totalMovements  = 2
   * </pre>
   */
  @Test
  @Order(8)
  void addMarchExpense_capturedForFirstDeletion() throws BusinessException {
    final AddMovementResultDTO result =
        addMovementUseCase.addMovement(
            userId,
            productDTO.id(),
            AddMovementCommandFixtureBuilder.createPersonalExpense(
                MARCH.atDay(15), new BigDecimal("150.00")));

    marExpenseToReverse = result.movement();
    assertNotNull(marExpenseToReverse, "March expense movement must be captured");

    delayTests();

    final ProductDTO product = getCurrentProductInfo();
    assertEquals(withJBHDecimals(new BigDecimal("1950")), product.movementBalance());
    assertEquals(withJBHDecimals(new BigDecimal("1950")), product.currentBalance());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PHASE 2 – DELETIONS (non-chronological order: Mar, Jan, Feb, Feb)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Order 9 – Assert the complete three-month base state before any deletions.
   *
   * <p>This is the baseline snapshot from which all deletion scenarios depart. If this test fails,
   * a base-data setup step likely has a bug.
   *
   * <h3>Expected product state</h3>
   *
   * <pre>
   * movementBalance  = $1,950  (1000+500−200+300−100+200+400−150)
   * currentBalance   = $1,950
   * netProfitBalance = $0
   * </pre>
   *
   * <h3>Expected January MB</h3>
   *
   * <pre>
   * openingBalance  = $    0
   * totalDebits     = $1,500
   * totalCredits    = $  200
   * movementBalance = $1,300
   * closingBalance  = $1,300
   * totalMovements  = 3
   * monthlyNetProfit = $0    (1300 − 0 − 1300 = 0)
   * </pre>
   *
   * <h3>Expected February MB</h3>
   *
   * <pre>
   * openingBalance  = $1,300  ← Jan closing (propagated async)
   * totalDebits     = $  500
   * totalCredits    = $  100
   * movementBalance = $  400
   * closingBalance  = $  400  ← only Feb movements (300−100+200), independent of opening
   * totalMovements  = 3
   * </pre>
   *
   * <h3>Expected March MB</h3>
   *
   * <pre>
   * openingBalance  = $  400  ← Feb closing (propagated async)
   * totalDebits     = $  400
   * totalCredits    = $  150
   * movementBalance = $  250
   * closingBalance  = $  250  ← only Mar movements (400−150), independent of opening
   * totalMovements  = 2
   * </pre>
   */
  @Test
  @Order(9)
  void verifyCompleteBaseStateBeforeAnyDeletions() {
    // Extra delay for the async opening-balance cascade to propagate Jan→Feb→Mar
    delayTests();
    delayTests();

    final List<MonthlyBalanceDTO> allBalances = getCurrentMonthlyBalances();
    assertFalse(allBalances.isEmpty(), "Monthly balances must have been created");

    final MonthlyBalanceDTO janMB = MonthlyBalanceITUtils.getBalanceForPeriod(allBalances, JANUARY);
    final MonthlyBalanceDTO febMB =
        MonthlyBalanceITUtils.getBalanceForPeriod(allBalances, FEBRUARY);
    final MonthlyBalanceDTO marMB = MonthlyBalanceITUtils.getBalanceForPeriod(allBalances, MARCH);

    // ── January ──────────────────────────────────────────────────────────────
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), janMB.openingBalance(), "Jan: openingBalance");
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")), janMB.totalDebits(), "Jan: totalDebits (1000+500)");
    assertEquals(
        withJBHDecimals(new BigDecimal("200")), janMB.totalCredits(), "Jan: totalCredits");
    assertEquals(
        withJBHDecimals(new BigDecimal("1300")), janMB.movementBalance(), "Jan: movementBalance");
    assertEquals(
        withJBHDecimals(new BigDecimal("1300")), janMB.closingBalance(), "Jan: closingBalance");
    assertEquals(3, janMB.totalMovements(), "Jan: totalMovements");

    // ── February ─────────────────────────────────────────────────────────────
    assertEquals(
        withJBHDecimals(new BigDecimal("1300")),
        febMB.openingBalance(),
        "Feb: openingBalance must equal Jan closing");
    assertEquals(
        withJBHDecimals(new BigDecimal("500")), febMB.totalDebits(), "Feb: totalDebits (300+200)");
    assertEquals(
        withJBHDecimals(new BigDecimal("100")), febMB.totalCredits(), "Feb: totalCredits");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), febMB.movementBalance(), "Feb: movementBalance");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), febMB.closingBalance(), "Feb: closingBalance (300−100+200, independent of opening)");
    assertEquals(3, febMB.totalMovements(), "Feb: totalMovements");

    // ── March ────────────────────────────────────────────────────────────────
    assertEquals(
        withJBHDecimals(new BigDecimal("400")),
        marMB.openingBalance(),
        "Mar: openingBalance must equal Feb closing ($400)");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMB.totalDebits(), "Mar: totalDebits");
    assertEquals(
        withJBHDecimals(new BigDecimal("150")), marMB.totalCredits(), "Mar: totalCredits");
    assertEquals(
        withJBHDecimals(new BigDecimal("250")), marMB.movementBalance(), "Mar: movementBalance");
    assertEquals(
        withJBHDecimals(new BigDecimal("250")), marMB.closingBalance(), "Mar: closingBalance (400−150, independent of opening)");
    assertEquals(2, marMB.totalMovements(), "Mar: totalMovements");

    // ── Product ───────────────────────────────────────────────────────────────
    final ProductDTO product = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("1950")), product.movementBalance(), "Product movementBalance");
    assertEquals(
        withJBHDecimals(new BigDecimal("1950")), product.currentBalance(), "Product currentBalance");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        product.netProfitBalance(),
        "Product netProfitBalance must be $0 (no unrealized gains)");
  }

  private List<MonthlyBalanceDTO> getCurrentMonthlyBalances() {
    delayTests();
    return monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());
  }

  /**
   * Order 10 – <strong>Delete the March expense (−$150): same-month deletion.</strong>
   *
   * <p>This is the simplest case. The deleted movement's period is the current month (March), so no
   * cross-month cascade is expected. January and February monthly balances must remain exactly as
   * they were.
   *
   * <h3>Reversal mechanics</h3>
   *
   * <p>{@code MonthlyBalanceDomain.syncBalancesByMovement(REMOVE, amount=−150)}:
   *
   * <pre>
   * totalCredits   −= |−150| → 150 − 150 = $0
   * closingBalance −= (−150) → 1,950 + 150 = $2,100  (subtracting a negative = adding)
   * </pre>
   *
   * <h3>Expected product state after deletion</h3>
   *
   * <pre>
   * movementBalance  = $1,950 − (−$150) = $2,100
   * currentBalance   = $2,100
   * netProfitBalance = $0
   * </pre>
   *
   * <h3>Expected March MB after deletion</h3>
   *
   * <pre>
   * openingBalance  = $  400  (unchanged – no cascade needed)
   * totalDebits     = $  400  (unchanged)
   * totalCredits    = $    0  (was $150, removed)
   * movementBalance = $  400  (was $250, now 400−0)
   * closingBalance  = $  400  (was $250, +$150; independent of opening)
   * totalMovements  = 1       (was 2, removed 1)
   * </pre>
   *
   * <h3>January and February MB: completely unchanged</h3>
   */
  @Test
  @Order(10)
  void deleteMarchExpense_onlyCurrentMonthBalanceChanges() throws BusinessException {
    // Capture Jan and Feb snapshots now – they must be identical after the deletion
    delayTests();
    final List<MonthlyBalanceDTO> allBefore = getCurrentMonthlyBalances();
    final MonthlyBalanceDTO janMBBefore = MonthlyBalanceITUtils.getBalanceForPeriod(allBefore, JANUARY);
    final MonthlyBalanceDTO febMBBefore = MonthlyBalanceITUtils.getBalanceForPeriod(allBefore, FEBRUARY);
    final ProductDTO productBefore = getCurrentProductInfo();

    log.info(
        "Before deleting March expense: product movementBalance={}, currentBalance={}",
        productBefore.movementBalance(),
        productBefore.currentBalance());

    // ── Execute deletion ──────────────────────────────────────────────────────
    deleteMovementUseCase.deleteMovement(userId, productDTO.id(), marExpenseToReverse.id().value());

    // ── Movement must be gone ─────────────────────────────────────────────────
    assertTrue(
        movementLifecycleSrv.findById(marExpenseToReverse.id().value()).isEmpty(),
        "March expense movement must be absent from repository");

    // ── Product assertions ────────────────────────────────────────────────────
    final ProductDTO productAfter = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("2100")),
        productAfter.movementBalance(),
        "Product movementBalance: 1950 − (−150) = 2100");
    assertEquals(
        withJBHDecimals(new BigDecimal("2100")),
        productAfter.currentBalance(),
        "Product currentBalance: 1950 + 150 = 2100");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        productAfter.netProfitBalance(),
        "Product netProfitBalance must remain $0");

    // ── March MB assertions ───────────────────────────────────────────────────
    final List<MonthlyBalanceDTO> allAfter = getCurrentMonthlyBalances();
    final MonthlyBalanceDTO marMBAfter = MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, MARCH);

    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.openingBalance(), "Mar: openingBalance unchanged ($400)");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.totalDebits(), "Mar: totalDebits unchanged");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), marMBAfter.totalCredits(), "Mar: totalCredits 150−150=0");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.movementBalance(), "Mar: movementBalance 400−0=400");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.closingBalance(), "Mar: closingBalance 250+150=400 (independent of opening)");
    assertEquals(1, marMBAfter.totalMovements(), "Mar: totalMovements 2−1=1");

    // ── January and February must be untouched ───────────────────────────────
    MonthlyBalanceITUtils.assertMonthlyBalance(
        janMBBefore, MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, JANUARY));
    MonthlyBalanceITUtils.assertMonthlyBalance(
        febMBBefore, MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, FEBRUARY));
  }

  /**
   * Order 11 – <strong>Delete the January expense (−$200): oldest-month cross-month cascade.</strong>
   *
   * <p>This is the most complex scenario. Deleting a movement from January triggers a two-level
   * cascade: January's new closing becomes February's opening, and February's new closing becomes
   * March's opening.
   *
   * <h3>State before this deletion (from Order 10)</h3>
   *
   * <pre>
   * Product:  movementBalance=$2,100  currentBalance=$2,100
   * Jan MB:   opening=$0      debits=$1,500  credits=$200  closing=$1,300
   * Feb MB:   opening=$1,300  debits=$500    credits=$100  closing=$  400
   * Mar MB:   opening=$  400  debits=$400    credits=$0    closing=$  400
   * </pre>
   *
   * <h3>Reversal mechanics – January MB</h3>
   *
   * <p>{@code MonthlyBalanceDomain.syncBalancesByMovement(REMOVE, amount=−200)}:
   *
   * <pre>
   * totalCredits   −= |−200| → 200 − 200 = $0
   * closingBalance −= (−200) → 1,300 + 200 = $1,500
   * movementBalance = 1,500 − 0 = $1,500
   * </pre>
   *
   * <h3>Async cascade – February MB (only opening is updated; closing is independent)</h3>
   *
   * <pre>
   * openingBalance  = $1,500   ← Jan new closing  (was $1,300)
   * closingBalance  = $  400   (unchanged – only Feb's own movements count)
   * movementBalance = $  400   (unchanged)
   * </pre>
   *
   * <h3>March MB – not touched by this cascade</h3>
   *
   * <pre>
   * openingBalance  = $  400   (= Feb closing = $400, unchanged)
   * closingBalance  = $  400   (unchanged – only Mar's own movements count)
   * movementBalance = $  400   (unchanged)
   * </pre>
   *
   * <h3>Expected product state after deletion</h3>
   *
   * <pre>
   * movementBalance  = $2,100 − (−$200) = $2,300
   * currentBalance   = $2,300
   * netProfitBalance = $0
   * </pre>
   *
   * <h3>Expected January MB after deletion</h3>
   *
   * <pre>
   * openingBalance  = $    0  (unchanged)
   * totalDebits     = $1,500  (unchanged)
   * totalCredits    = $    0  (was $200, removed)
   * movementBalance = $1,500  (was $1,300, now 1500−0)
   * closingBalance  = $1,500  (was $1,300, +$200)
   * totalMovements  = 2       (was 3, −1)
   * </pre>
   *
   * <h3>Expected February MB after cascade (async)</h3>
   *
   * <pre>
   * openingBalance  = $1,500  (was $1,300 – updated by cascade)
   * totalDebits     = $  500  (unchanged)
   * totalCredits    = $  100  (unchanged)
   * movementBalance = $  400  (unchanged)
   * closingBalance  = $  400  (unchanged – only Feb's own movements count: 300−100+200=400)
   * totalMovements  = 3       (unchanged)
   * </pre>
   *
   * <h3>Expected March MB (opening cascades from Feb closing; Mar's own closing is independent)</h3>
   *
   * <pre>
   * openingBalance  = $  400  (= Feb closing $400, unchanged)
   * totalDebits     = $  400  (unchanged)
   * totalCredits    = $    0  (already $0 from Order 10)
   * movementBalance = $  400  (unchanged)
   * closingBalance  = $  400  (unchanged – only Mar's own movements count: 400−0=400)
   * totalMovements  = 1       (unchanged from Order 10)
   * </pre>
   */
  @Test
  @Order(11)
  void deleteJanuaryExpense_cascadesOpeningBalancesToFebruaryAndMarch() throws BusinessException {
    log.info(
        "Deleting January expense. This should cascade: Jan closing → Feb opening → Mar opening");

    // ── Execute deletion ──────────────────────────────────────────────────────
    deleteMovementUseCase.deleteMovement(userId, productDTO.id(), janExpenseToReverse.id().value());

    // Extra delays for the two-level async cascade (Jan→Feb→Mar)
    delayTests();
    delayTests();

    // ── Movement must be gone ─────────────────────────────────────────────────
    assertTrue(
        movementLifecycleSrv.findById(janExpenseToReverse.id().value()).isEmpty(),
        "January expense movement must be absent from repository");

    // ── Product assertions ────────────────────────────────────────────────────
    final ProductDTO productAfter = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("2300")),
        productAfter.movementBalance(),
        "Product movementBalance: 2100 − (−200) = 2300");
    assertEquals(
        withJBHDecimals(new BigDecimal("2300")),
        productAfter.currentBalance(),
        "Product currentBalance: 2100 + 200 = 2300");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        productAfter.netProfitBalance(),
        "Product netProfitBalance must remain $0");

    final List<MonthlyBalanceDTO> allAfter = getCurrentMonthlyBalances();

    // ── January MB assertions ─────────────────────────────────────────────────
    final MonthlyBalanceDTO janMBAfter = MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, JANUARY);
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), janMBAfter.openingBalance(), "Jan: openingBalance stays $0");
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")), janMBAfter.totalDebits(), "Jan: totalDebits unchanged $1,500");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), janMBAfter.totalCredits(), "Jan: totalCredits 200−200=0");
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")), janMBAfter.movementBalance(), "Jan: movementBalance 1500−0=1500");
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")), janMBAfter.closingBalance(), "Jan: closingBalance 1300+200=1500");
    assertEquals(2, janMBAfter.totalMovements(), "Jan: totalMovements 3−1=2");

    // ── February MB assertions (async cascade: opening updated) ──────────────
    final MonthlyBalanceDTO febMBAfter =
        MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, FEBRUARY);
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")),
        febMBAfter.openingBalance(),
        "Feb: openingBalance must equal Jan new closing $1,500 (was $1,300)");
    assertEquals(
        withJBHDecimals(new BigDecimal("500")), febMBAfter.totalDebits(), "Feb: totalDebits unchanged");
    assertEquals(
        withJBHDecimals(new BigDecimal("100")), febMBAfter.totalCredits(), "Feb: totalCredits unchanged");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), febMBAfter.movementBalance(), "Feb: movementBalance unchanged 500−100=400");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")),
        febMBAfter.closingBalance(),
        "Feb: closingBalance unchanged $400 (only Feb own movements: 300−100+200=400)");
    assertEquals(3, febMBAfter.totalMovements(), "Feb: totalMovements unchanged at 3");

    // ── March MB assertions (opening = Feb closing; Mar's own closing is independent) ───────────
    final MonthlyBalanceDTO marMBAfter = MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, MARCH);
    assertEquals(
        withJBHDecimals(new BigDecimal("400")),
        marMBAfter.openingBalance(),
        "Mar: openingBalance must equal Feb closing $400 (unchanged)");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.totalDebits(), "Mar: totalDebits unchanged");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), marMBAfter.totalCredits(), "Mar: totalCredits still $0");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.movementBalance(), "Mar: movementBalance unchanged 400−0=400");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")),
        marMBAfter.closingBalance(),
        "Mar: closingBalance unchanged $400 (only Mar own movements: 400−0=400)");
    assertEquals(1, marMBAfter.totalMovements(), "Mar: totalMovements still 1");
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PHASE 2 FINAL VERIFICATION  (Order 30)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Order 20 – <strong>Delete the February income (+$200): middle-month cross-month cascade.</strong>
   *
   * <p>Deleting a movement from February should update February's own balance and cascade the new
   * February closing to March's opening. January must remain completely unchanged.
   *
   * <h3>State before this deletion (cumulative from Orders 10 + 11)</h3>
   *
   * <pre>
   * Product:  movementBalance=$2,300  currentBalance=$2,300
   * Jan MB:   opening=$0      debits=$1,500  credits=$0    closing=$1,500
   * Feb MB:   opening=$1,500  debits=$500    credits=$100  closing=$  400
   * Mar MB:   opening=$  400  debits=$400    credits=$0    closing=$  400
   * </pre>
   *
   * <h3>Reversal mechanics – February MB</h3>
   *
   * <p>{@code MonthlyBalanceDomain.syncBalancesByMovement(REMOVE, amount=+200)}:
   *
   * <pre>
   * totalDebits    −= 200 → 500 − 200 = $300
   * closingBalance −= 200 → 400 − 200 = $200  (independent of opening)
   * movementBalance = 300 − 100 = $200
   * </pre>
   *
   * <h3>Async cascade – March MB (only opening is updated; closing is independent)</h3>
   *
   * <pre>
   * openingBalance  = $  200   ← Feb new closing  (was $400)
   * closingBalance  = $  400   (unchanged – only Mar's own movements count: 400−0=400)
   * </pre>
   *
   * <h3>Expected product state after deletion</h3>
   *
   * <pre>
   * movementBalance  = $2,300 − $200 = $2,100
   * currentBalance   = $2,100
   * netProfitBalance = $0
   * </pre>
   *
   * <h3>Expected January MB: completely unchanged</h3>
   *
   * <h3>Expected February MB after deletion</h3>
   *
   * <pre>
   * openingBalance  = $1,500  (unchanged)
   * totalDebits     = $  300  (was $500, −$200)
   * totalCredits    = $  100  (unchanged)
   * movementBalance = $  200  (was $400, now 300−100)
   * closingBalance  = $  200  (was $400, −$200; independent of opening)
   * totalMovements  = 2       (was 3, −1)
   * </pre>
   *
   * <h3>Expected March MB after cascade (async)</h3>
   *
   * <pre>
   * openingBalance  = $  200  (was $400 – updated by cascade to Feb new closing)
   * totalDebits     = $  400  (unchanged)
   * totalCredits    = $    0  (unchanged)
   * movementBalance = $  400  (unchanged)
   * closingBalance  = $  400  (unchanged – only Mar's own movements count: 400−0=400)
   * totalMovements  = 1       (unchanged)
   * </pre>
   */
  @Test
  @Order(20)
  void deleteFebruaryIncome_cascadesToMarchOpeningBalance() throws BusinessException {
    // Snapshot January now – it must be untouched after this deletion
    delayTests();
    final List<MonthlyBalanceDTO> allBefore = getCurrentMonthlyBalances();
    janMBSnapshotBeforeDeletion = MonthlyBalanceITUtils.getBalanceForPeriod(allBefore, JANUARY);

    log.info("Deleting February income (+$200). Expected cascade: Feb closing → Mar opening");

    // ── Execute deletion ──────────────────────────────────────────────────────
    deleteMovementUseCase.deleteMovement(userId, productDTO.id(), febIncomeToReverse.id().value());

    // Extra delay for the one-level async cascade (Feb→Mar)
    delayTests();
    delayTests();

    // ── Movement must be gone ─────────────────────────────────────────────────
    assertTrue(
        movementLifecycleSrv.findById(febIncomeToReverse.id().value()).isEmpty(),
        "February income movement must be absent from repository");

    // ── Product assertions ────────────────────────────────────────────────────
    final ProductDTO productAfter = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("2100")),
        productAfter.movementBalance(),
        "Product movementBalance: 2300 − 200 = 2100");
    assertEquals(
        withJBHDecimals(new BigDecimal("2100")),
        productAfter.currentBalance(),
        "Product currentBalance: 2300 − 200 = 2100");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        productAfter.netProfitBalance(),
        "Product netProfitBalance must remain $0");

    final List<MonthlyBalanceDTO> allAfter = getCurrentMonthlyBalances();

    // ── January MB must be unchanged ──────────────────────────────────────────
    MonthlyBalanceITUtils.assertMonthlyBalance(
        janMBSnapshotBeforeDeletion, MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, JANUARY));

    // ── February MB assertions ─────────────────────────────────────────────────
    final MonthlyBalanceDTO febMBAfter =
        MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, FEBRUARY);
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")), febMBAfter.openingBalance(), "Feb: openingBalance unchanged $1,500");
    assertEquals(
        withJBHDecimals(new BigDecimal("300")), febMBAfter.totalDebits(), "Feb: totalDebits 500−200=300");
    assertEquals(
        withJBHDecimals(new BigDecimal("100")), febMBAfter.totalCredits(), "Feb: totalCredits unchanged $100");
    assertEquals(
        withJBHDecimals(new BigDecimal("200")), febMBAfter.movementBalance(), "Feb: movementBalance 300−100=200");
    assertEquals(
        withJBHDecimals(new BigDecimal("200")), febMBAfter.closingBalance(), "Feb: closingBalance 400−200=200 (independent of opening)");
    assertEquals(2, febMBAfter.totalMovements(), "Feb: totalMovements 3−1=2");

    // ── March MB assertions (cascade: opening = Feb new closing; Mar closing independent) ────────
    final MonthlyBalanceDTO marMBAfter = MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, MARCH);
    assertEquals(
        withJBHDecimals(new BigDecimal("200")),
        marMBAfter.openingBalance(),
        "Mar: openingBalance must equal Feb new closing $200 (was $400)");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.totalDebits(), "Mar: totalDebits unchanged");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), marMBAfter.totalCredits(), "Mar: totalCredits unchanged $0");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.movementBalance(), "Mar: movementBalance unchanged 400−0=400");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.closingBalance(), "Mar: closingBalance unchanged $400 (only Mar own movements: 400−0=400)");
    assertEquals(1, marMBAfter.totalMovements(), "Mar: totalMovements unchanged at 1");
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Order 21 – <strong>Delete the February expense (−$100): second deletion in the same month.</strong>
   *
   * <p>This test demonstrates that multiple deletions within the same past month accumulate
   * correctly. After Order 20 already removed one February movement, this test removes the second,
   * leaving February with only the Feb 5 income (+$300).
   *
   * <h3>State before this deletion (cumulative from Orders 10 + 11 + 20)</h3>
   *
   * <pre>
   * Product:  movementBalance=$2,100  currentBalance=$2,100
   * Jan MB:   opening=$0      debits=$1,500  credits=$0    closing=$1,500
   * Feb MB:   opening=$1,500  debits=$300    credits=$100  closing=$  200
   * Mar MB:   opening=$  200  debits=$400    credits=$0    closing=$  400
   * </pre>
   *
   * <h3>Reversal mechanics – February MB</h3>
   *
   * <p>{@code MonthlyBalanceDomain.syncBalancesByMovement(REMOVE, amount=−100)}:
   *
   * <pre>
   * totalCredits   −= |−100| → 100 − 100 = $0
   * closingBalance −= (−100) → 200 + 100 = $300  (subtracting a negative = adding; independent of opening)
   * movementBalance = 300 − 0 = $300
   * </pre>
   *
   * <h3>Async cascade – March MB (only opening is updated; closing is independent)</h3>
   *
   * <pre>
   * openingBalance  = $  300   ← Feb new closing  (was $200)
   * closingBalance  = $  400   (unchanged – only Mar's own movements count: 400−0=400)
   * </pre>
   *
   * <h3>Expected product state after deletion</h3>
   *
   * <pre>
   * movementBalance  = $2,100 − (−$100) = $2,200
   * currentBalance   = $2,200
   * netProfitBalance = $0
   * </pre>
   *
   * <h3>Expected January MB: completely unchanged</h3>
   *
   * <h3>Expected February MB after deletion (only Feb 5 income remains)</h3>
   *
   * <pre>
   * openingBalance  = $1,500  (unchanged from Order 20 cascade)
   * totalDebits     = $  300  (unchanged from Order 20)
   * totalCredits    = $    0  (was $100, −$100)
   * movementBalance = $  300  (was $200, now 300−0)
   * closingBalance  = $  300  (was $200, +$100; independent of opening)
   * totalMovements  = 1       (was 2, −1; only Feb 5 income remains)
   * </pre>
   *
   * <h3>Expected March MB after cascade (async)</h3>
   *
   * <pre>
   * openingBalance  = $  300  (was $200 – updated by cascade to Feb new closing)
   * totalDebits     = $  400  (unchanged)
   * totalCredits    = $    0  (unchanged)
   * movementBalance = $  400  (unchanged)
   * closingBalance  = $  400  (unchanged – only Mar's own movements count: 400−0=400)
   * totalMovements  = 1       (unchanged)
   * </pre>
   */
  @Test
  @Order(21)
  void deleteFebruaryExpense_accumulatesWithPreviousFebruaryDeletion() throws BusinessException {
    // Snapshot January now – it must be untouched after this deletion
    delayTests();
    final List<MonthlyBalanceDTO> allBefore = getCurrentMonthlyBalances();
    janMBSnapshotBeforeDeletion = MonthlyBalanceITUtils.getBalanceForPeriod(allBefore, JANUARY);

    log.info(
        "Deleting February expense (−$100). Expected cascade: Feb new closing ($300) → Mar opening");

    // ── Execute deletion ──────────────────────────────────────────────────────
    deleteMovementUseCase.deleteMovement(userId, productDTO.id(), febExpenseToReverse.id().value());

    // Extra delay for the one-level async cascade (Feb→Mar)
    delayTests();
    delayTests();

    // ── Movement must be gone ─────────────────────────────────────────────────
    assertTrue(
        movementLifecycleSrv.findById(febExpenseToReverse.id().value()).isEmpty(),
        "February expense movement must be absent from repository");

    // ── Product assertions ────────────────────────────────────────────────────
    final ProductDTO productAfter = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("2200")),
        productAfter.movementBalance(),
        "Product movementBalance: 2100 − (−100) = 2200");
    assertEquals(
        withJBHDecimals(new BigDecimal("2200")),
        productAfter.currentBalance(),
        "Product currentBalance: 2100 + 100 = 2200");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        productAfter.netProfitBalance(),
        "Product netProfitBalance must remain $0");

    final List<MonthlyBalanceDTO> allAfter = getCurrentMonthlyBalances();

    // ── January MB must be unchanged ──────────────────────────────────────────
    MonthlyBalanceITUtils.assertMonthlyBalance(
        janMBSnapshotBeforeDeletion, MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, JANUARY));

    // ── February MB assertions ─────────────────────────────────────────────────
    final MonthlyBalanceDTO febMBAfter =
        MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, FEBRUARY);
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")), febMBAfter.openingBalance(), "Feb: openingBalance unchanged $1,500");
    assertEquals(
        withJBHDecimals(new BigDecimal("300")), febMBAfter.totalDebits(), "Feb: totalDebits unchanged $300 from Order 20");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), febMBAfter.totalCredits(), "Feb: totalCredits 100−100=0");
    assertEquals(
        withJBHDecimals(new BigDecimal("300")), febMBAfter.movementBalance(), "Feb: movementBalance 300−0=300");
    assertEquals(
        withJBHDecimals(new BigDecimal("300")), febMBAfter.closingBalance(), "Feb: closingBalance 200+100=300 (independent of opening)");
    assertEquals(1, febMBAfter.totalMovements(), "Feb: totalMovements 2−1=1 (only Feb 5 income remains)");

    // ── March MB assertions (cascade: opening = Feb new closing; Mar closing independent) ────────
    final MonthlyBalanceDTO marMBAfter = MonthlyBalanceITUtils.getBalanceForPeriod(allAfter, MARCH);
    assertEquals(
        withJBHDecimals(new BigDecimal("300")),
        marMBAfter.openingBalance(),
        "Mar: openingBalance must equal Feb new closing $300 (was $200)");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.totalDebits(), "Mar: totalDebits unchanged $400");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO), marMBAfter.totalCredits(), "Mar: totalCredits unchanged $0");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.movementBalance(), "Mar: movementBalance 400−0=400");
    assertEquals(
        withJBHDecimals(new BigDecimal("400")), marMBAfter.closingBalance(), "Mar: closingBalance unchanged $400 (only Mar own movements: 400−0=400)");
    assertEquals(1, marMBAfter.totalMovements(), "Mar: totalMovements unchanged at 1");
  }

  /**
   * Order 30 – Assert the final system state after all four deletions.
   *
   * <p>Only 4 movements remain out of the original 8:
   *
   * <pre>
   * Jan  1 : +$1,000  (initial balance)
   * Jan 10 : +$  500  (income)
   * Feb  5 : +$  300  (income)
   * Mar  5 : +$  400  (income)
   * Sum    =  $2,200
   * </pre>
   *
   * <h3>Expected final product state</h3>
   *
   * <pre>
   * movementBalance  = $2,200
   * currentBalance   = $2,200
   * netProfitBalance = $0
   * </pre>
   *
   * <h3>Expected final January MB</h3>
   *
   * <pre>
   * openingBalance  = $    0
   * totalDebits     = $1,500
   * totalCredits    = $    0
   * movementBalance = $1,500
   * closingBalance  = $1,500
   * totalMovements  = 2
   * </pre>
   *
   * <h3>Expected final February MB</h3>
   *
   * <pre>
   * openingBalance  = $1,500  ← Jan closing (propagated)
   * totalDebits     = $  300
   * totalCredits    = $    0
   * movementBalance = $  300
   * closingBalance  = $  300  ← only Feb movements: +300 (independent of opening)
   * totalMovements  = 1
   * </pre>
   *
   * <h3>Expected final March MB</h3>
   *
   * <pre>
   * openingBalance  = $  300  ← Feb closing (propagated)
   * totalDebits     = $  400
   * totalCredits    = $    0
   * movementBalance = $  400
   * closingBalance  = $  400  ← only Mar movements: +400 (independent of opening)
   * totalMovements  = 1
   * </pre>
   */
  @Test
  @Order(30)
  void verifyFinalStateAfterAllFourDeletions() {
    // Allow any remaining async tasks to finish
    delayTests();
    delayTests();

    // ── Product ───────────────────────────────────────────────────────────────
    final ProductDTO product = getCurrentProductInfo();
    assertEquals(
        withJBHDecimals(new BigDecimal("2200")),
        product.movementBalance(),
        "Final product movementBalance: 1000+500+300+400 = $2,200");
    assertEquals(
        withJBHDecimals(new BigDecimal("2200")),
        product.currentBalance(),
        "Final product currentBalance: $2,200");
    assertEquals(
        withJBHDecimals(BigDecimal.ZERO),
        product.netProfitBalance(),
        "Final product netProfitBalance: $0");

    final List<MonthlyBalanceDTO> allBalances = getCurrentMonthlyBalances();

    // ── Final January MB ──────────────────────────────────────────────────────
    final MonthlyBalanceDTO janFinal = MonthlyBalanceITUtils.getBalanceForPeriod(allBalances, JANUARY);
    assertEquals(withJBHDecimals(BigDecimal.ZERO), janFinal.openingBalance(), "Jan final: openingBalance");
    assertEquals(withJBHDecimals(new BigDecimal("1500")), janFinal.totalDebits(), "Jan final: totalDebits");
    assertEquals(withJBHDecimals(BigDecimal.ZERO), janFinal.totalCredits(), "Jan final: totalCredits");
    assertEquals(withJBHDecimals(new BigDecimal("1500")), janFinal.movementBalance(), "Jan final: movementBalance");
    assertEquals(withJBHDecimals(new BigDecimal("1500")), janFinal.closingBalance(), "Jan final: closingBalance");
    assertEquals(2, janFinal.totalMovements(), "Jan final: totalMovements");

    // ── Final February MB ─────────────────────────────────────────────────────
    final MonthlyBalanceDTO febFinal =
        MonthlyBalanceITUtils.getBalanceForPeriod(allBalances, FEBRUARY);
    assertEquals(
        withJBHDecimals(new BigDecimal("1500")),
        febFinal.openingBalance(),
        "Feb final: openingBalance must equal Jan closing $1,500");
    assertEquals(withJBHDecimals(new BigDecimal("300")), febFinal.totalDebits(), "Feb final: totalDebits");
    assertEquals(withJBHDecimals(BigDecimal.ZERO), febFinal.totalCredits(), "Feb final: totalCredits");
    assertEquals(withJBHDecimals(new BigDecimal("300")), febFinal.movementBalance(), "Feb final: movementBalance");
    assertEquals(withJBHDecimals(new BigDecimal("300")), febFinal.closingBalance(), "Feb final: closingBalance (only Feb movements: +300, independent of opening)");
    assertEquals(1, febFinal.totalMovements(), "Feb final: totalMovements");

    // ── Final March MB ────────────────────────────────────────────────────────
    final MonthlyBalanceDTO marFinal = MonthlyBalanceITUtils.getBalanceForPeriod(allBalances, MARCH);
    assertEquals(
        withJBHDecimals(new BigDecimal("300")),
        marFinal.openingBalance(),
        "Mar final: openingBalance must equal Feb closing $300");
    assertEquals(withJBHDecimals(new BigDecimal("400")), marFinal.totalDebits(), "Mar final: totalDebits");
    assertEquals(withJBHDecimals(BigDecimal.ZERO), marFinal.totalCredits(), "Mar final: totalCredits");
    assertEquals(withJBHDecimals(new BigDecimal("400")), marFinal.movementBalance(), "Mar final: movementBalance");
    assertEquals(withJBHDecimals(new BigDecimal("400")), marFinal.closingBalance(), "Mar final: closingBalance (only Mar movements: +400, independent of opening)");
    assertEquals(1, marFinal.totalMovements(), "Mar final: totalMovements");
  }
}
