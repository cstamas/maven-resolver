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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RepositorySystemLifecycle;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.util.ConfigUtils;

import static java.util.Objects.requireNonNull;

/**
 * Session based implementation that selects based on session config.
 *
 * @since 1.9.1
 */
@Singleton
@Named
public final class SessionNamedLockFactoryAdapterFactory
        extends NamedLockFactoryAdapterFactory
{
    private static final String INSTANCE_KEY = SessionNamedLockFactoryAdapterFactory.class.getName() + ".instance";

    /**
     * Default constructor for non Eclipse Sisu uses.
     */
    public SessionNamedLockFactoryAdapterFactory()
    {
        super( FACTORIES, NAME_MAPPERS );
    }

    /**
     * Constructor that uses Eclipse Sisu parameter injection.
     */
    @Inject
    public SessionNamedLockFactoryAdapterFactory( final RepositorySystemLifecycle repositorySystemLifecycle,
                                                  final Map<String, NamedLockFactory> factories,
                                                  final Map<String, NameMapper> nameMappers )
    {
        super( factories, nameMappers );
        registerLifecycle( repositorySystemLifecycle );
    }

    @Override
    public NamedLockFactoryAdapter getAdapter( RepositorySystemSession session )
    {
        return (NamedLockFactoryAdapter) session.getData().computeIfAbsent( INSTANCE_KEY, () ->
        {
            NameMapper nameMapper =
                    selectNameMapper( ConfigUtils.getString( session, DEFAULT_NAME_MAPPER, NAME_MAPPER_KEY ) );
            NamedLockFactory namedLockFactory =
                    selectNamedLockFactory( ConfigUtils.getString( session, DEFAULT_FACTORY, FACTORY_KEY ) );
            return new NamedLockFactoryAdapter( nameMapper, namedLockFactory );
        } );
    }
}