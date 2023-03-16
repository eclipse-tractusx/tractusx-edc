package org.eclipse.tractusx.edc.tests.data;

public class NullDataAddress implements DataAddress {

  private static final NullDataAddress _instance = new NullDataAddress();

  public static DataAddress INSTANCE = new NullDataAddress();

  private NullDataAddress() {}
}
