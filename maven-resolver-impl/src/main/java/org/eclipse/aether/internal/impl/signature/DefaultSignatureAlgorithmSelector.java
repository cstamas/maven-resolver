package org.eclipse.aether.internal.impl.signature;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.signature.pgp.PgpSignatureAlgorithm;
import org.eclipse.aether.spi.signature.SignatureAlgorithm;
import org.eclipse.aether.spi.signature.SignatureAlgorithmSelector;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Default implementation.
 *
 * @since 1.9.3
 */
@Singleton
@Named
public class DefaultSignatureAlgorithmSelector
        implements SignatureAlgorithmSelector
{
    private final Map<String, SignatureAlgorithm> algorithms;

    /**
     * Default ctor for SL.
     */
    @Deprecated
    public DefaultSignatureAlgorithmSelector()
    {
        this.algorithms = new HashMap<>();
        this.algorithms.put( PgpSignatureAlgorithm.NAME, new PgpSignatureAlgorithm() );
    }

    @Inject
    public DefaultSignatureAlgorithmSelector( Map<String, SignatureAlgorithm> algorithms )
    {
        this.algorithms = requireNonNull( algorithms );
    }

    @Override
    public SignatureAlgorithm select( String algorithmName )
    {
        requireNonNull( algorithmName, "algorithmMame must not be null" );
        SignatureAlgorithm factory = algorithms.get( algorithmName );
        if ( factory == null )
        {
            throw new IllegalArgumentException(
                    String.format( "Unsupported signature algorithm %s, supported ones are %s",
                            algorithmName,
                            getSignatureAlgorithmFactories().stream()
                                    .map( SignatureAlgorithm::getName )
                                    .collect( toList() )
                    )
            );
        }
        return factory;
    }

    @Override
    public List<SignatureAlgorithm> selectList( Collection<String> algorithmNames )
    {
        return algorithmNames.stream()
                .map( this::select )
                .collect( toList() );
    }

    @Override
    public List<SignatureAlgorithm> getSignatureAlgorithmFactories()
    {
        return new ArrayList<>( algorithms.values() );
    }

    @Override
    public boolean isSignatureArtifact( Artifact artifact )
    {
        for ( SignatureAlgorithm signatureAlgorithm : algorithms.values() )
        {
            if ( signatureAlgorithm.isSignatureArtifact( artifact ) )
            {
                return true;
            }
        }
        return false;
    }
}
