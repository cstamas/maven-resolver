package org.eclipse.aether.internal.impl.synccontext;

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
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Named {@link SyncContextFactory} implementation that selects underlying {@link NamedLockFactory} implementation
 * at creation.
 */
@Named
@Singleton
public final class NamedSyncContextFactory
        implements SyncContextFactory
{
    private static final long TIME = Long.getLong(
            NamedSyncContextFactory.class.getName() + ".time", 10L
    );

    private static final TimeUnit TIME_UNIT = TimeUnit.valueOf( System.getProperty(
            NamedSyncContextFactory.class.getName() + ".timeunit", TimeUnit.SECONDS.name()
    ) );

    private final SyncContextFactoryAdapter syncContextFactoryAdapter;

    /**
     * Constructor used with SISU, where factories are injected and selected based on key.
     */
    @Inject
    public NamedSyncContextFactory( final Map<String, Provider<NamedLockFactory>> factories )
    {
        String name = System.getProperty( "synccontext.named.factory", "rwlock-local" );
        Provider<NamedLockFactory> provider = factories.get( name );
        if ( provider == null )
        {
            throw new IllegalArgumentException( "Unknown NamedLockFactory name: " + name );
        }
        this.syncContextFactoryAdapter = new SyncContextFactoryAdapter( provider.get(), TIME, TIME_UNIT );
    }

    /**
     * Constructor when factory already exists, or to be used in tests.
     */
    public NamedSyncContextFactory( final NamedLockFactory namedLockFactory, long time, TimeUnit timeUnit )
    {
        Objects.requireNonNull( namedLockFactory );
        this.syncContextFactoryAdapter = new SyncContextFactoryAdapter( namedLockFactory, time, timeUnit );
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
