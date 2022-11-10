package org.eclipse.aether.internal.impl.synccontext.named;

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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.synccontext.named.providers.DiscriminatingNameMapperProvider;
import org.eclipse.aether.internal.impl.synccontext.named.providers.FileGAVNameMapperProvider;
import org.eclipse.aether.internal.impl.synccontext.named.providers.FileHashingGAVNameMapperProvider;
import org.eclipse.aether.internal.impl.synccontext.named.providers.GAVNameMapperProvider;
import org.eclipse.aether.internal.impl.synccontext.named.providers.StaticNameMapperProvider;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.named.providers.FileLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalReadWriteLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalSemaphoreNamedLockFactory;
import org.eclipse.aether.named.providers.NoopNamedLockFactory;

import static java.util.Objects.requireNonNull;

/**
 * Support class for {@link NamedLockFactoryAdapter} factories.
 *
 * @since 1.9.1
 */
public abstract class NamedLockFactoryAdapterFactory
{
    protected static final Map<String, NamedLockFactory> FACTORIES;

    protected static final String DEFAULT_FACTORY = LocalReadWriteLockNamedLockFactory.NAME;

    protected static final Map<String, NameMapper> NAME_MAPPERS;

    protected static final String DEFAULT_NAME_MAPPER = GAVNameMapperProvider.NAME;

    static
    {
        HashMap<String, NamedLockFactory> factories = new HashMap<>();
        factories.put( NoopNamedLockFactory.NAME, new NoopNamedLockFactory() );
        factories.put( LocalReadWriteLockNamedLockFactory.NAME, new LocalReadWriteLockNamedLockFactory() );
        factories.put( LocalSemaphoreNamedLockFactory.NAME, new LocalSemaphoreNamedLockFactory() );
        factories.put( FileLockNamedLockFactory.NAME, new FileLockNamedLockFactory() );
        FACTORIES = factories;

        HashMap<String, NameMapper> mappers = new HashMap<>();
        mappers.put( StaticNameMapperProvider.NAME, new StaticNameMapperProvider().get() );
        mappers.put( GAVNameMapperProvider.NAME, new GAVNameMapperProvider().get() );
        mappers.put( DiscriminatingNameMapperProvider.NAME, new DiscriminatingNameMapperProvider().get() );
        mappers.put( FileGAVNameMapperProvider.NAME, new FileGAVNameMapperProvider().get() );
        mappers.put( FileHashingGAVNameMapperProvider.NAME, new FileHashingGAVNameMapperProvider().get() );
        NAME_MAPPERS = mappers;
    }

    protected static final String FACTORY_KEY = "aether.syncContext.named.factory";

    protected static final String NAME_MAPPER_KEY = "aether.syncContext.named.nameMapper";

    protected final Map<String, NamedLockFactory> factories;

    protected final Map<String, NameMapper> nameMappers;

    protected NamedLockFactoryAdapterFactory( Map<String, NamedLockFactory> factories,
                                              Map<String, NameMapper> nameMappers )
    {
        this.factories = requireNonNull( factories );
        this.nameMappers = requireNonNull( nameMappers );
    }

    protected NamedLockFactory selectNamedLockFactory( final String factoryName )
    {
        NamedLockFactory factory = factories.get( factoryName );
        if ( factory == null )
        {
            throw new IllegalArgumentException( "Unknown NamedLockFactory name: " + factoryName
                    + ", known ones: " + factories.keySet() );
        }
        return factory;
    }

    protected NameMapper selectNameMapper( final String nameMapperName )
    {
        NameMapper nameMapper = nameMappers.get( nameMapperName );
        if ( nameMapper == null )
        {
            throw new IllegalArgumentException( "Unknown NameMapper name: " + nameMapperName
                    + ", known ones: " + nameMappers.keySet() );
        }
        return nameMapper;
    }

    /**
     * Returns the {@link NamedLockFactoryAdapter}, never {@code null}.
     * <p>
     * It is up to implementation what will do here: create new instances over and over based on
     * passed in session, create (and cache) one instance eagerly, and keep returning that. One
     * thing for sure, implementation must ensure that all instances emitted by this method will be
     * properly cleaned up (see {@link NamedLockFactory#shutdown()} method).
     */
    public abstract NamedLockFactoryAdapter getAdapter( RepositorySystemSession session );
}