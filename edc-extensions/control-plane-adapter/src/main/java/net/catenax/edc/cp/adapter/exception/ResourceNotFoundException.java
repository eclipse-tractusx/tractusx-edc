package net.catenax.edc.cp.adapter.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(final String id) {
    super(String.format("Resource not found for id %s", id));
  }
}
