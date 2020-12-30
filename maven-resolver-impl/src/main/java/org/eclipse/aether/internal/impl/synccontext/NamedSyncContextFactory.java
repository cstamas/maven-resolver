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
import org.eclipse.aether.named.providers.LocalReadWriteLockProvider;
import org.eclipse.aether.named.providers.LocalSemaphoreProvider;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Named {@link SyncContextFactoryDelegate} implementation that selects underlying {@link NamedLockFactory}
 * implementation at creation.
 */
@Singleton
@Named( NamedSyncContextFactory.NAME )
public final class NamedSyncContextFactory
        implements SyncContextFactoryDelegate
{
    public static final String NAME = "named";

    private static final String FACTORY_NAME = System.getProperty(
            "aether.syncContext.named.factory", LocalReadWriteLockProvider.NAME
    );

    private static final long TIME_OUT = Long.getLong(
            "aether.syncContext.named.timeOut", 30L
    );

    private static final TimeUnit TIME_UNIT = TimeUnit.valueOf( System.getProperty(
            "aether.syncContext.named.timeUnit", TimeUnit.SECONDS.name()
    ) );

    private final NamedLockFactoryAdapter namedLockFactoryAdapter;

    /**
     * Constructor used with DI, where factories are injected and selected based on key.
     */
    @Inject
    public NamedSyncContextFactory( final Map<String, Provider<NamedLockFactory>> factories )
    {
        this.namedLockFactoryAdapter = selectAndAdapt( factories );
    }

    /**
     * Default constructor.
     */
    public NamedSyncContextFactory()
    {
        HashMap<String, Provider<NamedLockFactory>> providers = new HashMap<>();
        providers.put( LocalReadWriteLockProvider.NAME, new LocalReadWriteLockProvider() );
        providers.put( LocalSemaphoreProvider.NAME, new LocalSemaphoreProvider() );
        this.namedLockFactoryAdapter = selectAndAdapt( providers );
    }

    private NamedLockFactoryAdapter selectAndAdapt( final Map<String, Provider<NamedLockFactory>> factories )
    {
        Provider<NamedLockFactory> provider = factories.get( FACTORY_NAME );
        if ( provider == null )
        {
            throw new IllegalArgumentException( "Unknown NamedLockFactory name: " + FACTORY_NAME );
        }
        return new NamedLockFactoryAdapter( provider.get(), TIME_OUT, TIME_UNIT );
    }

    @Override
    public SyncContext newInstance( final RepositorySystemSession session, final boolean shared )
    {
        return namedLockFactoryAdapter.newInstance( session, shared );
    }

    @PreDestroy
    public void shutdown()
    {
        namedLockFactoryAdapter.shutdown();
    }
}
