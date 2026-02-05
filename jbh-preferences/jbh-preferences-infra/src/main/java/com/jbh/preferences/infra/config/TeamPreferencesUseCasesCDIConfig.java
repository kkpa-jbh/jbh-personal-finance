package com.jbh.preferences.infra.config;

import com.jbh.preferences.application.core.ports.input.CreateTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.GetTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.UpdateTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.output.TeamPreferencesRepository;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.application.core.services.TeamPreferencesServiceImpl;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection(
    targets = {
      GetTeamPreferencesInputPort.class,
      UpdateTeamPreferencesInputPort.class,
      CreateTeamPreferencesInputPort.class
    })
public class TeamPreferencesUseCasesCDIConfig {

  @Inject TeamPreferencesRepository preferencesRepository;

  @Produces
  @ApplicationScoped
  public TeamPreferencesService teamPreferencesService() {
    return new TeamPreferencesServiceImpl(preferencesRepository);
  }

  @Produces
  @ApplicationScoped
  public GetTeamPreferencesInputPort getTeamPreferencesUseCase() {
    return new GetTeamPreferencesInputPort(teamPreferencesService());
  }

  @Produces
  @ApplicationScoped
  public UpdateTeamPreferencesInputPort updateTeamPreferencesUseCase() {
    return new UpdateTeamPreferencesInputPort(teamPreferencesService());
  }

  @Produces
  @ApplicationScoped
  public CreateTeamPreferencesInputPort createTeamPreferencesUseCase() {
    return new CreateTeamPreferencesInputPort(teamPreferencesService());
  }
}
