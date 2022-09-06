package net.catenax.edc.cp.adapter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class LockMap {
  private final Map<String, ReentrantLock> lock = new HashMap<>();

  public void lock(String id) {
    addLock(id);
    lock.get(id).lock();
  }

  public void unlock(String id) {
    addLock(id);
    lock.get(id).unlock();
  }

  public void removeLock(String id) {
    addLock(id);
    lock.remove(id);
  }

  private void addLock(String id) {
    synchronized (this) {
      lock.putIfAbsent(id, new ReentrantLock());
    }
  }
}
