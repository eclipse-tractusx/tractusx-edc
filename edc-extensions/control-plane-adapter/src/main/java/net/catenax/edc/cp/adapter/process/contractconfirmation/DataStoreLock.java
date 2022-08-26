package net.catenax.edc.cp.adapter.process.contractconfirmation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataStoreLock {
  private final Map<String, ReadWriteLock> lock = new HashMap<>();

  public void readLock(String id) {
    addLock(id);
    lock.get(id).readLock().lock();
  }

  public void readUnlock(String id) {
    addLock(id);
    lock.get(id).readLock().unlock();
  }

  public void writeLock(String id) {
    addLock(id);
    lock.get(id).writeLock().lock();
  }

  public void writeUnlock(String id) {
    addLock(id);
    lock.get(id).writeLock().unlock();
  }

  public void removeLock(String id) {
    addLock(id);
    lock.remove(id);
  }

  private void addLock(String id) {
    synchronized (this) {
      lock.putIfAbsent(id, new ReentrantReadWriteLock());
    }
  }
}
