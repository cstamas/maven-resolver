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

import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

/**
 * A factory to create synchronization contexts. This default implementation does not provide any real
 * synchronization but merely completes the repository system.
 */
@Singleton
@Named( NoLockSyncContextFactory.NAME )
public class NoLockSyncContextFactory
    implements SyncContextFactoryDelegate
{
    public static final String NAME = "nolock";

    @Override
    public SyncContext newInstance( final RepositorySystemSession session, final boolean shared )
    {
        return new DefaultSyncContext();
    }

    private static class DefaultSyncContext
        implements SyncContext
    {
        @Override
        public void acquire( final Collection<? extends Artifact> artifact,
                             final Collection<? extends Metadata> metadata )
        {
        }

        @Override
        public void close()
        {
        }
    }
}