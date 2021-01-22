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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.util.ChecksumUtils;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discriminating {@link NameMapper}, that extends {@link GAVNameMapper} and adds "discriminator" that makes lock
 * names unique including local repository (localRepo + GAV). The discriminator may be passed in via
 * {@link RepositorySystemSession} or is automatically calculated based on local repository path.
 */
@Singleton
@Named( LGAVNameMapper.NAME )
public class LGAVNameMapper
    extends GAVNameMapper
{
  public static final String NAME = "lgav";

  /**
   * Configuration property to pass in discriminator.
   */
  private static final String CONFIG_PROP_DISCRIMINATOR = "aether.syncContext.named.discriminator";

  private static final String DEFAULT_DISCRIMINATOR_DIGEST = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

  private static final String DEFAULT_HOSTNAME = "localhost";

  private final Logger log = LoggerFactory.getLogger( getClass() );

  private final String hostname;

  @Inject
  public LGAVNameMapper()
  {
    this.hostname = getHostname();
  }

  @Override
  public Collection<String> nameLocks( final RepositorySystemSession session,
                                       final Collection<? extends Artifact> artifacts,
                                       final Collection<? extends Metadata> metadatas )
  {
    return toGAVNames( NAME_PREFIX + createDiscriminator( session ) + ":", artifacts, metadatas );
  }

  private String getHostname()
  {
    try
    {
      return InetAddress.getLocalHost().getHostName();
    }
    catch ( UnknownHostException e )
    {
      log.warn( "Failed to get hostname, using '{}'", DEFAULT_HOSTNAME, e );
      return DEFAULT_HOSTNAME;
    }
  }

  private String createDiscriminator( final RepositorySystemSession session )
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
        log.warn( "Failed to calculate discriminator digest, using '{}'", DEFAULT_DISCRIMINATOR_DIGEST, e );
        return DEFAULT_DISCRIMINATOR_DIGEST;
      }
    }
    return discriminator;
  }
}
