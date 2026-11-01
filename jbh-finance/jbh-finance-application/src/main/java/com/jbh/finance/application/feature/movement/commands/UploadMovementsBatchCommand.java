package com.jbh.finance.application.feature.movement.commands;

import com.jbh.finance.domain.movement.vo.ProcessMovementOptionsVO;
import java.util.List;

/**
 * The movements of one upload and the options to process them.
 *
 * @param movements the movements to save
 * @param options how the product sync treats them
 */
public record UploadMovementsBatchCommand(
    List<AddMovementUploadedFileCommand> movements, ProcessMovementOptionsVO options) {

  /**
   * A batch from a user file: default options.
   *
   * @param movements the movements to save
   * @return the batch
   */
  public static UploadMovementsBatchCommand forFile(
      final List<AddMovementUploadedFileCommand> movements) {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * A batch from a bank statement: the funds check is skipped.
   *
   * @param movements the movements to save
   * @return the batch
   */
  public static UploadMovementsBatchCommand forStatement(
      final List<AddMovementUploadedFileCommand> movements) {
    throw new UnsupportedOperationException("not implemented");
  }
}
