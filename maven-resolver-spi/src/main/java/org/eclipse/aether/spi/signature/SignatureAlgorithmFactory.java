package org.eclipse.aether.spi.signature;

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
import org.eclipse.aether.artifact.Artifact;

/**
 * Signature algorithm performing artifact signing.
 *
 * @since 1.9.3
 */
public interface SignatureAlgorithmFactory
{
    /**
     * Returns the algorithm name, usually used as key, never {@code null} value. The name is a standard name of
     * algorithm (if applicable) or any other designator that is algorithm commonly referred with. Example: "PGP".
     */
    String getName();

    /**
     * Returns {@code true} if passed in artifact represents a signature artifact produced by this algorithm.
     */
    boolean isSignatureArtifact( Artifact artifact );

    /**
     * Returns new {@link SignatureSigner} instance, or {@code null} if this instance cannot provide signer instance
     * based on passed in session.
     */
    SignatureSigner signer( RepositorySystemSession session );
}
