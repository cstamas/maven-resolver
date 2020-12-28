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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.named.NamedLock;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.util.ChecksumUtils;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Adapter to adapt {@link NamedLockFactory} and {@link NamedLock} to {@link SyncContext}.
 */
final class SyncContextFactoryAdapter
{
    private static final String DEFAULT_DISCRIMINATOR_DIGEST = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    private static final String DEFAULT_HOSTNAME = "localhost";

    private static final Logger LOGGER = LoggerFactory.getLogger( SyncContextFactoryAdapter.class );

    private final NamedLockFactory namedLockFactory;

    private final long time;

    private final TimeUnit timeUnit;

    private final String hostname;

    SyncContextFactoryAdapter( final NamedLockFactory namedLockFactory,
                               final long time,
                               final TimeUnit timeUnit )
    {
        this.namedLockFactory = namedLockFactory;
        this.time = time;
        this.timeUnit = timeUnit;
        this.hostname = getHostname();
    }

    private String getHostname()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch ( UnknownHostException e )
        {
            LOGGER.warn( "Failed to get hostname, using '{}'", DEFAULT_HOSTNAME, e );
            return DEFAULT_HOSTNAME;
        }
    }

    public SyncContext newInstance( final RepositorySystemSession session, final boolean shared )
    {
        return new AdaptedLockSyncContext( session, hostname, namedLockFactory, time, timeUnit, shared );
    }

    public void shutdown()
    {
        namedLockFactory.shutdown();
    }

    private static class AdaptedLockSyncContext
            implements SyncContext
    {
        private static final String CONFIG_PROP_DISCRIMINATOR = "aether.syncContext.named.discriminator";

        private static final String KEY_PREFIX = "mvn:resolver:";

        private static final Logger LOGGER = LoggerFactory.getLogger( AdaptedLockSyncContext.class );

        private final RepositorySystemSession session;

        private final String hostname;

        private final NamedLockFactory namedLockFactory;

        private final long time;

        private final TimeUnit timeUnit;

        private final boolean shared;

        private final ArrayDeque<NamedLock> locks;

        private AdaptedLockSyncContext( final RepositorySystemSession session,
                                        final String hostname,
                                        final NamedLockFactory namedLockFactory,
                                        final long time,
                                        final TimeUnit timeUnit,
                                        final boolean shared )
        {
            this.session = session;
            this.hostname = hostname;
            this.namedLockFactory = namedLockFactory;
            this.time = time;
            this.timeUnit = timeUnit;
            this.shared = shared;
            this.locks = new ArrayDeque<>();
        }

        @Override
        public void acquire( Collection<? extends Artifact> artifacts,
                             Collection<? extends Metadata> metadatas )
        {
            // Deadlock prevention: https://stackoverflow.com/a/16780988/696632
            // We must acquire multiple locks always in the same order!
            Collection<String> keys = new TreeSet<>();
            if ( artifacts != null )
            {
                for ( Artifact artifact : artifacts )
                {
                    // TODO Should we include extension and classifier too?
                    String key = "artifact:" + artifact.getGroupId() + ":"
                            + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
                    keys.add( key );
                }
            }

            if ( metadatas != null )
            {
                for ( Metadata metadata : metadatas )
                {
                    StringBuilder key = new StringBuilder( "metadata:" );
                    if ( !metadata.getGroupId().isEmpty() )
                    {
                        key.append( metadata.getGroupId() );
                        if ( !metadata.getArtifactId().isEmpty() )
                        {
                            key.append( ':' ).append( metadata.getArtifactId() );
                            if ( !metadata.getVersion().isEmpty() )
                            {
                                key.append( ':' ).append( metadata.getVersion() );
                            }
                        }
                    }
                    keys.add( key.toString() );
                }
            }

            if ( keys.isEmpty() )
            {
                return;
            }

            String discriminator = createDiscriminator();
            LOGGER.trace( "Using key discriminator '{}' during this session", discriminator );

            LOGGER.trace( "Need {} {} lock(s) for {}", keys.size(), shared ? "read" : "write", keys );
            int acquiredLockCount = 0;
            for ( String key : keys )
            {
                NamedLock namedLock = namedLockFactory.getLock( KEY_PREFIX + discriminator + ":" + key );
                try
                {
                    boolean locked;
                    if ( shared )
                    {
                        locked = namedLock.lockShared( time, timeUnit );
                    }
                    else
                    {
                        locked = namedLock.lockExclusively( time, timeUnit );
                    }

                    if ( !locked )
                    {
                        namedLock.close();
                        throw new IllegalStateException( "Could not lock "
                                + namedLock.name() + " (shared=" + shared + ")" );
                    }

                    locks.push( namedLock );
                    acquiredLockCount++;
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException( e );
                }
            }
            LOGGER.trace( "Total new locks acquired: {}", acquiredLockCount );
        }

        private String createDiscriminator()
        {
            String discriminator = ConfigUtils.getString( session, null, CONFIG_PROP_DISCRIMINATOR );

            if ( discriminator == null || discriminator.isEmpty() )
            {
                File basedir = session.getLocalRepository().getBasedir();
                discriminator = hostname + ":" + basedir;
                try
                {
                    Map<String, Object> checksums = ChecksumUtils.calc(
                            discriminator.getBytes( StandardCharsets.UTF_8 ),
                            Collections.singletonList( "SHA-1" ) );
                    Object checksum = checksums.get( "SHA-1" );

                    if ( checksum instanceof Exception )
                    {
                        throw ( Exception ) checksum;
                    }

                    return String.valueOf( checksum );
                }
                catch ( Exception e )
                {
                    LOGGER.warn( "Failed to calculate discriminator digest, using '{}'",
                            DEFAULT_DISCRIMINATOR_DIGEST, e );
                    return DEFAULT_DISCRIMINATOR_DIGEST;
                }
            }

            return discriminator;
        }

        @Override
        public void close()
        {
            if ( locks.isEmpty() )
            {
                return;
            }

            // Release locks in reverse insertion order
            int released = 0;
            while ( !locks.isEmpty() )
            {
                try ( NamedLock namedLock = locks.pop() )
                {
                    namedLock.unlock();
                    released++;
                }
            }
            LOGGER.trace( "Total locks released: {}", released );
        }
    }
}
