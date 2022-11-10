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
import static java.util.Objects.requireNonNull;

/**
 * Parameterized selector implementation that selects based on injected parameters.
 * <p>
 * It uses Sisu named ctor parameters to create one adapter and always returns that one instance.
 *
 * @since 1.9.1
 */
@Singleton
@Named
public final class ParameterizedNamedLockFactoryAdapterFactory
        extends NamedLockFactoryAdapterFactory
{
    private final NamedLockFactory namedLockFactory;

    private final NameMapper nameMapper;

    private final NamedLockFactoryAdapter adapter;

    /**
     * Default constructor for SL.
     */
    public ParameterizedNamedLockFactoryAdapterFactory()
    {
        super( FACTORIES, NAME_MAPPERS );
        this.namedLockFactory = selectNamedLockFactory( DEFAULT_FACTORY );
        this.nameMapper = selectNameMapper( DEFAULT_NAME_MAPPER );
        this.adapter = new NamedLockFactoryAdapter( this.nameMapper, this.namedLockFactory );
    }

    /**
     * Constructor that uses Eclipse Sisu parameter injection.
     */
    @SuppressWarnings( "checkstyle:LineLength" )
    @Inject
    public ParameterizedNamedLockFactoryAdapterFactory( final RepositorySystemLifecycle repositorySystemLifecycle,
                                                        final Map<String, NamedLockFactory> factories,
                                                        @Named( "${" + FACTORY_KEY + ":-" + DEFAULT_FACTORY + "}" ) final String selectedFactoryName,
                                                        final Map<String, NameMapper> nameMappers,
                                                        @Named( "${" + NAME_MAPPER_KEY + ":-" + DEFAULT_NAME_MAPPER + "}" ) final String selectedMapperName )
    {
        super( factories, nameMappers );
        registerLifecycle( repositorySystemLifecycle );
        this.namedLockFactory = selectNamedLockFactory( selectedFactoryName );
        this.nameMapper = selectNameMapper( selectedMapperName );
        this.adapter = new NamedLockFactoryAdapter( this.nameMapper, this.namedLockFactory );
    }

    @Override
    public NamedLockFactoryAdapter getAdapter( RepositorySystemSession session )
    {
        return adapter;
    }
}