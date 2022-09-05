package net.catenax.edc.cp.adapter.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Exception e) {
    super(message, e);
  }
}
