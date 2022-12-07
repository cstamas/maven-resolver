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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.signature.SignatureSigner;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.TransferListener;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A signature handling connector that may sign and/or verify transfers using selected signature algorithms and
 * delegates to another connector.
 */
public final class SignatureRepositoryConnector
        implements RepositoryConnector
{
    private final RepositoryConnector delegate;

    private final SignatureSigner signatureSigner;

    public SignatureRepositoryConnector( RepositoryConnector delegate,
                                         SignatureSigner signatureSigner )
    {
        this.delegate = requireNonNull( delegate );
        this.signatureSigner = requireNonNull( signatureSigner );
    }

    @Override
    public void close()
    {
        delegate.close();
    }

    @Override
    public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                     Collection<? extends MetadataDownload> metadataDownloads )
    {
        delegate.get( artifactDownloads, metadataDownloads );
    }

    @Override
    public void put( Collection<? extends ArtifactUpload> artifactUploads,
                     Collection<? extends MetadataUpload> metadataUploads )
    {
        List<ArtifactUpload> signedArtifactUploads = null;
        List<ArtifactUpload> signatureUploads = null;
        if ( artifactUploads != null && !artifactUploads.isEmpty() )
        {
            signedArtifactUploads = new ArrayList<>( artifactUploads );
            Collection<Artifact> signatures = signatureSigner.sign( artifactUploads.stream()
                    .map( ArtifactUpload::getArtifact ).collect( toList() ) );

            if ( !signatures.isEmpty() )
            {
                ArtifactUpload first = signedArtifactUploads.get( 0 );
                signatureUploads = new ArrayList<>( signatures.size() );
                signatureUploads.addAll( signatureUploads( signatures, first.getTrace(), first.getListener() ) );
                signedArtifactUploads.addAll( signatureUploads );
            }
        }

        delegate.put( signedArtifactUploads, metadataUploads );

        if ( signatureUploads != null )
        {
            ArtifactTransferException anyFailure = signatureUploads.stream()
                    .map( ArtifactUpload::getException )
                    .findAny().orElse( null );
            if ( anyFailure != null )
            {
                for ( ArtifactUpload upload : artifactUploads )
                {
                    upload.setException( anyFailure );
                }
            }
        }
    }

    private List<ArtifactUpload> signatureUploads( Collection<Artifact> signatures,
                                                   RequestTrace trace,
                                                   TransferListener listener )
    {
        ArrayList<ArtifactUpload> signatureUploads = new ArrayList<>( signatures.size() );
        for ( Artifact signature : signatures )
        {
            ArtifactUpload artifactUpload = new ArtifactUpload( signature, signature.getFile() );
            artifactUpload.setTrace( trace );
            artifactUpload.setListener( listener );
            signatureUploads.add( artifactUpload );
        }
        return signatureUploads;
    }

    @Override
    public String toString()
    {
        return "signature(" + delegate.toString() + ")";
    }
}
