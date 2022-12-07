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
import java.util.HashSet;
import java.util.Map;

import org.eclipse.aether.MultiRuntimeException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.SignatureAlgorithmManager;
import org.eclipse.aether.spi.signature.SignatureAlgorithmFactory;
import org.eclipse.aether.spi.signature.SignatureAlgorithmSelector;
import org.eclipse.aether.spi.signature.SignatureSigner;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Default implementation of manager: it creates, keeps signers in session, and aggregates possibly multiple signers
 * into one and ensures they behave.
 */
@Singleton
@Named
public class DefaultSignatureAlgorithmManager
        implements SignatureAlgorithmManager
{
    private static final String INSTANCE_KEY = DefaultSignatureAlgorithmManager.class + ".instance";

    private final SignatureAlgorithmSelector signatureAlgorithmSelector;

    @Deprecated
    public DefaultSignatureAlgorithmManager()
    {
        this.signatureAlgorithmSelector = new DefaultSignatureAlgorithmSelector();
    }

    @Inject
    public DefaultSignatureAlgorithmManager( SignatureAlgorithmSelector signatureAlgorithmSelector )
    {
        this.signatureAlgorithmSelector = requireNonNull( signatureAlgorithmSelector );
    }

    @Override
    public SignatureSigner getSignatureSigners( RepositorySystemSession session )
    {
        final Object signer = session.getData().computeIfAbsent( INSTANCE_KEY, () ->
        {
            Map<String, SignatureSigner> signers = selectSigners( session );
            if ( signers.isEmpty() )
            {
                return NO_ACTIVE_SIGNER;
            }
            else
            {
                return new ManagedSigners( signers );
            }
        } );

        if ( signer == NO_ACTIVE_SIGNER )
        {
            return null;
        }

        return (SignatureSigner) signer;
    }

    private Map<String, SignatureSigner> selectSigners( RepositorySystemSession session )
    {
        requireNonNull( session, "session is null" );
        HashMap<String, SignatureSigner> signers = new HashMap<>();
        for ( SignatureAlgorithmFactory factory : signatureAlgorithmSelector.getSignatureAlgorithmFactories() )
        {
            SignatureSigner signer = factory.signer( session );
            if ( signer != null )
            {
                signers.put( factory.getName(), signer );
            }
        }
        return signers;
    }

    private static final Object NO_ACTIVE_SIGNER = new Object();

    /**
     * Managed signer just aggregates all the provided {@link SignatureSigner} instances as one, and ensures they do
     * not produce conflicting signature artifacts.
     */
    private static class ManagedSigners implements SignatureSigner
    {
        private final Map<String, SignatureSigner> signers;

        private ManagedSigners( Map<String, SignatureSigner> signers )
        {
            this.signers = signers;
        }

        @Override
        public Collection<Artifact> sign( Collection<Artifact> artifacts )
        {
            requireNonNull( artifacts );

            // all the IDs of passed in artifacts
            HashSet<String> toBeSignedIds = new HashSet<>();
            artifacts.forEach( a -> toBeSignedIds.add( ArtifactIdUtils.toId( a ) ) );

            // all the IDs of produced signature artifacts
            HashSet<String> signatureIds = new HashSet<>();

            // we must ensure that:
            // 1. there is no conflict among generated signature artifacts
            // 2. no signature artifact conflicts with "to be signed" artifact

            ArrayList<Exception> exceptions = new ArrayList<>( signers.size() );
            HashMap<String, Collection<Artifact>> signatures = new HashMap<>();
            for ( Map.Entry<String, SignatureSigner> entry : signers.entrySet() )
            {
                String name = entry.getKey();
                SignatureSigner signer = entry.getValue();

                Collection<Artifact> signerSignatures = signer.sign( artifacts );
                for ( Artifact signature : signerSignatures )
                {
                    String id = ArtifactIdUtils.toId( signature );
                    if ( toBeSignedIds.contains( id ) )
                    {
                        // conflict: signer provided same artifact that is among to be signed ones
                        exceptions.add( new IllegalStateException( "Signature algorithm " + name
                                + " produced signature artifact that conflicts with deployed artifacts: "
                                + id ) );
                    }
                    if ( !signatureIds.add( id ) )
                    {
                        // conflict: another provider already provided signature like this
                        exceptions.add( new IllegalStateException( "Signature algorithm " + name
                                + " produced signature artifact that conflicts with other signature artifact: "
                                + id ) );

                    }
                }
                signatures.put( name, signerSignatures );
            }
            MultiRuntimeException.mayThrow( "Problem signing artifacts", exceptions );

            return signatures.values().stream().flatMap( Collection::stream ).collect( toList() );
        }

        @Override
        public void close()
        {
            ArrayList<Exception> exceptions = new ArrayList<>( signers.size() );
            for ( SignatureSigner signer : signers.values() )
            {
                try
                {
                    signer.close();
                }
                catch ( Exception e )
                {
                    exceptions.add( e );
                }
            }
            MultiRuntimeException.mayThrow( "Problem closing signers", exceptions );
        }
    }
}
