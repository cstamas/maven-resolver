package org.eclipse.aether.internal.named;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.concurrent.TimeUnit;

/**
 * A named lock, functionally similar to existing JVM and other implementations. Does support boxing, but no
 * lock upgrade is supported. Usual pattern to use this lock:
 * <pre>
 *   try (NamedLock lock = factory.getLock("resourceName")) {
 *     if (lock.lockExclusively(10L, Timeunit.SECONDS)) {
 *       try {
 *         ... exclusive access to "resourceName" resource gained here
 *       }
 *       finally {
 *         lock.unlock();
 *       }
 *     }
 *     else {
 *       ... failed to gain access within specified time, handle it
 *     }
 *   }
 * </pre>
 */
public interface NamedLock
    extends AutoCloseable
{
  /**
   * Returns this instance name, never null.
   */
  String name();

  /**
   * Tries to lock shared, may block for given time. If successful, returns {@code true}.
   */
  boolean lockShared( long time, TimeUnit unit ) throws InterruptedException;

  /**
   * Tries to lock exclusively, may block for given time. If successful, returns {@code true}.
   */
  boolean lockExclusively( long time, TimeUnit unit ) throws InterruptedException;

  /**
   * Unlocks the lock.
   */
  void unlock();

  /**
   * Closes the lock.
   */
  @Override
  void close();
}
