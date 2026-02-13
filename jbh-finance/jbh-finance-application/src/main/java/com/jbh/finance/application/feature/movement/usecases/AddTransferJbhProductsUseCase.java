package com.jbh.finance.application.feature.movement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.commands.AddTransferCommand;
import com.jbh.finance.domain.product.vo.ProductPK;

/**
 * Adds a transfer between two existing products (products) owned by the user within the JBH system.
 *
 * <p><strong>User Explanation:</strong> "Move money between your products. The system automatically
 * records both the withdrawal from the source productDTO and the deposit to the destination productDTO."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Both source and destination products must exist in the system
 *   <li>If transferring to an external/unknown productDTO, use AddMovementUseCase instead
 *   <li>Source productDTO must have sufficient net flow to cover the transfer
 *   <li>Special validation for loan products: transfer amount cannot exceed payoff amount
 *   <li>Creates two movements: withdrawal (expense) from source, deposit (income) to destination
 * </ul>
 */
public interface AddTransferJbhProductsUseCase {

  /**
   * Adds a transfer between two existing products, creating movements in both products.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command fields validation (via command.validate())
   *   <li>Both FROM and TO products must exist
   *   <li>Source productDTO must have sufficient net flow for the transfer amount
   *   <li>If destination is a loan productDTO, transfer amount cannot exceed payoff amount
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: Withdrawal movement (TRANSFER category) in source productDTO
   *   <li>INSERT: Deposit movement (TRANSFER category) in destination productDTO
   *   <li>UPDATE: Both products' current balance and net flow
   *   <li>UPDATE: Monthly balance summaries for both products (asynchronous)
   * </ul>
   *
   * @param fromAccount the source product primary key (userId and productId)
   * @param transferCommand contains transfer date, amount, and destination productDTO
   * @throws BusinessException if validation fails, insufficient funds, or products not found
   */
  void addTransfer(ProductPK fromAccount, AddTransferCommand transferCommand)
      throws BusinessException;
}
