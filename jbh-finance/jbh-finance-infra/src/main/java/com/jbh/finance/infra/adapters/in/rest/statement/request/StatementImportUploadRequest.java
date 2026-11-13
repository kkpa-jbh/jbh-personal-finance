package com.jbh.finance.infra.adapters.in.rest.statement.request;

import jakarta.ws.rs.FormParam;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * Multipart form of a statement upload. The password is never logged or stored; its {@code
 * toString} is not generated for that reason.
 */
public class StatementImportUploadRequest {

  @FormParam("file")
  @Parameter(description = "Bank statement PDF", required = true)
  public FileUpload file;

  @FormParam("password")
  @Parameter(description = "PDF password, only for locked PDFs. Never saved.")
  public String password;
}
