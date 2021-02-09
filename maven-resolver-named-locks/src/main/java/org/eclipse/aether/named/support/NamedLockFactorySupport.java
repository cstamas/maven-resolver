package org.eclipse.aether.named.support;

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.aether.named.NamedLockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for {@link NamedLockFactory} implementations providing reference counting.
 *
 * @param <L> the actual implementation, subclass of {@link NamedLockSupport}.
 */
public abstract class NamedLockFactorySupport<L extends NamedLockSupport>
    implements NamedLockFactory
{
  protected final Logger log = LoggerFactory.getLogger( getClass() );

  private final ConcurrentHashMap<String, L> locks;

  public NamedLockFactorySupport()
  {
    this.locks = new ConcurrentHashMap<>();
  }

  @Override
  public L getLock( final String name )
  {
    return locks.compute( name, ( k, v ) ->
    {
      if ( v == null )
      {
        v = createLock( k );
      }
      v.incRef();
      return v;
    } );
  }

  @Override
  public void shutdown()
  {
    // override if needed
  }

  public boolean closeLock( final L lock )
  {
    AtomicBoolean destroyed = new AtomicBoolean( false );
    locks.compute( lock.name(), ( k, v ) ->
    {
      if ( v != null && v.decRef() == 0 )
      {
        destroyLock( v );
        destroyed.set( true );
        return null;
      }
      return v;
    } );
    return destroyed.get();
  }

  protected abstract L createLock( final String name );

  protected void destroyLock( final L lock )
  {
    // override if needed
  }
}
