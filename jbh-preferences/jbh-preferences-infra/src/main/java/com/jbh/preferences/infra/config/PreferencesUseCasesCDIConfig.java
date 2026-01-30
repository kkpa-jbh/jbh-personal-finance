package com.jbh.preferences.infra.config;

import com.jbh.preferences.application.core.ports.input.CreateUserPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.GetUserPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.UpdateUserPreferencesInputPort;
import com.jbh.preferences.application.core.ports.output.UserPreferencesRepository;
import com.jbh.preferences.application.core.services.PreferencesService;
import com.jbh.preferences.application.core.services.PreferencesServiceImpl;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(
    targets = {
      GetUserPreferencesInputPort.class,
      UpdateUserPreferencesInputPort.class,
      CreateUserPreferencesInputPort.class
    })
public class PreferencesUseCasesCDIConfig {

  @Inject
  UserPreferencesRepository preferencesRepository;

  @Produces
  @ApplicationScoped
  public PreferencesService preferencesService() {
    return new PreferencesServiceImpl(preferencesRepository);
  }

  @Produces
  @ApplicationScoped
  public GetUserPreferencesInputPort getPreferencesUseCase() {
    return new GetUserPreferencesInputPort(preferencesService());
  }

  @Produces
  @ApplicationScoped
  public UpdateUserPreferencesInputPort updatePreferencesUseCase() {
    return new UpdateUserPreferencesInputPort(preferencesService());
  }

  @Produces
  @ApplicationScoped
  public CreateUserPreferencesInputPort createPreferencesUseCase() {
    return new CreateUserPreferencesInputPort(preferencesService());
  }
}
