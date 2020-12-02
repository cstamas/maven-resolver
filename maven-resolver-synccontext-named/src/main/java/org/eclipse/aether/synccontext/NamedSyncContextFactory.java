package org.eclipse.aether.synccontext;

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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.internal.named.NamedLockFactory;
import org.eclipse.aether.internal.named.SyncContextFactoryAdapter;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Named {@link SyncContextFactory} implementation that selects underlying {@link NamedLockFactory} implementation
 * at creation. Known factory names are:
 * <ul>
 *   <li><pre>global</pre> - uses one JVM ReentrantReadWriteLock for all locking, suitable for MT builds (but not
 *   recommended)</li>
 *   <li><pre>rwlock-local</pre> - uses multiple JVM ReentrantReadWriteLock instances, suitable for MT builds</li>
 *   <li><pre>rwlock-redisson</pre> - uses multiple Redis-backed ReentrantReadWriteLock instances, suitable for MT and
 *   MP builds</li>
 *   <li><pre>semaphore-local</pre> - uses multiple JVM Semaphore instances, suitable for MT builds</li>
 *   <li><pre>semaphore-hazelcast</pre> - uses multiple Hazelcast (that would form cluster with other discovered
 *   instances) semaphores, suitable for MT and MP builds</li>
 *   <li><pre>semaphore-redisson</pre> - uses multiple Redis-backed RSemaphore instance, suitable for MT and MP
 *   builds</li>
 *   <li><pre>semaphore-hazelcast-client</pre> - uses Hazelcast Client (that would connect to some existing cluster)
 *   semaphores, suitable for MT and MP builds</li>
 *   <li><pre>semaphore-cp-hazelcast-client</pre> - uses Hazelcast Client (that would connect to some existing cluster),
 *   semaphores, suitable for MT and MP builds. Uses CP subsystem to get semaphore and does NOT destroy them.</li>
 * </ul>
 */
@Named
@Priority( Integer.MAX_VALUE )
@Singleton
public final class NamedSyncContextFactory
        implements SyncContextFactory
{
    private final SyncContextFactoryAdapter syncContextFactoryAdapter;

    @Inject
    public NamedSyncContextFactory( final Map<String, Provider<NamedLockFactory>> factories )
    {
        String name = System.getProperty( "synccontext.named.factory", "local" );
        Provider<NamedLockFactory> provider = factories.get( name );
        if ( provider == null )
        {
            throw new IllegalArgumentException( "Unknown NamedLockFactory name: " + name );
        }
        this.syncContextFactoryAdapter = new SyncContextFactoryAdapter( provider.get() );
    }

    @Override
    public SyncContext newInstance( final RepositorySystemSession session, final boolean shared )
    {
        return syncContextFactoryAdapter.newInstance( session, shared );
    }

    @PreDestroy
    public void shutdown()
    {
        syncContextFactoryAdapter.shutdown();
    }
}
