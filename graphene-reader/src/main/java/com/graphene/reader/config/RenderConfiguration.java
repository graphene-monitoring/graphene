package com.graphene.reader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Andrei Ivanov
 */
@ConfigurationProperties("graphene.reader.render")
public class RenderConfiguration {
  private int requestTimeout = 30;
  private boolean humanReadableNumbers = false;

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public boolean isHumanReadableNumbers() {
    return humanReadableNumbers;
  }

  public void setHumanReadableNumbers(boolean humanReadableNumbers) {
    this.humanReadableNumbers = humanReadableNumbers;
  }

  @Override
  public String toString() {
    return "ReaderConfiguration{"
      + "requestTimeout="
      + requestTimeout
      + ", humanReadableNumbers="
      + humanReadableNumbers
      + '}';
  }
}
