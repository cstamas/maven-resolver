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

import java.util.Collection;

import org.eclipse.aether.artifact.Artifact;

/**
 * Component performing selection of {@link SignatureAlgorithmFactory} based on known factory names.
 * Note: this component is NOT meant to be implemented or extended by client, is exposed ONLY to make clients
 * able to get {@link SignatureAlgorithmFactory} instances.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 1.9.3
 */
public interface SignatureAlgorithmSelector
{
    /**
     * Returns factory for given algorithm name, or throws if algorithm not supported.
     *
     * @throws IllegalArgumentException if asked algorithm name is not supported.
     */
    SignatureAlgorithmFactory select( String algorithmName );

    /**
     * Returns a collection of all supported algorithms. This set represents all the algorithms supported by Resolver,
     * and is NOT in any relation to current session configuration.
     */
    Collection<SignatureAlgorithmFactory> getSignatureAlgorithmFactories();

    /**
     * Returns {@code true} if passed in artifact represents a signature belonging to any known factory.
     */
    boolean isSignatureArtifact( Artifact artifact );
}
