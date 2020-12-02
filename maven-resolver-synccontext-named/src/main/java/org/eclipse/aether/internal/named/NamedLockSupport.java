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

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for {@link NamedLock} implementations providing reference counting.
 */
public abstract class NamedLockSupport
    implements NamedLock
{
  protected final Logger log = LoggerFactory.getLogger( getClass() );

  private final String name;

  private final NamedLockFactorySupport factory;

  private final AtomicInteger refCount;

  public NamedLockSupport( final String name, final NamedLockFactorySupport factory )
  {
    this.name = name;
    this.factory = factory;
    this.refCount = new AtomicInteger( 0 );
  }

  public int incRef()
  {
    return refCount.incrementAndGet();
  }

  public int decRef()
  {
    return refCount.decrementAndGet();
  }

  @Override
  public String name()
  {
    return name;
  }

  @Override
  public void close()
  {
    factory.closeLock( this );
  }

  @Override
  protected void finalize() throws Throwable
  {
    try
    {
      if ( refCount.get() != 0 )
      {
        // report leak
        log.warn( "NamedLock leak: {} references={}", name, refCount.get() );
      }
    }
    finally
    {
      super.finalize();
    }
  }
}
