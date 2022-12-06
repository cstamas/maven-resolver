package org.eclipse.aether.internal.impl.signature.pgp;

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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.spi.signature.SignatureAlgorithm;

/**
 * PGP implementation.
 *
 * @since 1.9.3
 */
@Singleton
@Named( PgpSignatureAlgorithm.NAME )
public class PgpSignatureAlgorithm
        implements SignatureAlgorithm
{
    public static final String NAME = "PGP";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isSignatureArtifact( Artifact artifact )
    {
        return artifact.getExtension().endsWith( ".asc" );
    }

    @Override
    public List<Artifact> sign( Artifact artifact )
    {
        return Collections.emptyList();
    }
}
