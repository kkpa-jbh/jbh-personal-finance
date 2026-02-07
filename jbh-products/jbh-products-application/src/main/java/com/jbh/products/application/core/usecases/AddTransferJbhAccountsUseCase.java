package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.vo.commands.AddTransferCommand;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

/**
 * Adds a transfer between two existing products (accounts) owned by the user within the JBH
 * system.
 *
 * <p><strong>User Explanation:</strong> "Move money between your accounts. The system
 * automatically records both the withdrawal from the source account and the deposit to the
 * destination account."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Both source and destination accounts must exist in the system
 *   <li>If transferring to an external/unknown account, use AddMovementUseCase instead
 *   <li>Source account must have sufficient net flow to cover the transfer
 *   <li>Special validation for loan accounts: transfer amount cannot exceed payoff amount
 *   <li>Creates two movements: withdrawal (expense) from source, deposit (income) to destination
 * </ul>
 */
public interface AddTransferJbhAccountsUseCase {

  /**
   * Adds a transfer between two existing accounts, creating movements in both accounts.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command fields validation (via command.validate())
   *   <li>Both FROM and TO accounts must exist
   *   <li>Source account must have sufficient net flow for the transfer amount
   *   <li>If destination is a loan account, transfer amount cannot exceed payoff amount
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: Withdrawal movement (TRANSFER category) in source account
   *   <li>INSERT: Deposit movement (TRANSFER category) in destination account
   *   <li>UPDATE: Both accounts' current balance and net flow
   *   <li>UPDATE: Monthly balance summaries for both accounts (asynchronous)
   * </ul>
   *
   * @param fromAccount the source product primary key (userId and productId)
   * @param transferCommand contains transfer date, amount, and destination account
   * @throws BusinessException if validation fails, insufficient funds, or accounts not found
   */
  void addTransfer(ProductPK fromAccount, AddTransferCommand transferCommand)
      throws BusinessException;
}
