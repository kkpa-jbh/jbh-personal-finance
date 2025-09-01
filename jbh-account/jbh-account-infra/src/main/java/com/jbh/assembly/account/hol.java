package com.jbh.assembly.account;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.annotations.Pos;

@ApplicationScoped
public class hol {

  @PostConstruct
  void init() {
    System.out.println("hol for jbh-account module initialized.");
  }

}
