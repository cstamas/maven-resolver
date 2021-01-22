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

import java.util.Collection;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

/**
 * Component mapping lock names to passed in artifacts and metadata as required.
 */
public interface NameMapper
{
  /**
   * The name prefix recommended to be used.
   */
  String NAME_PREFIX = "maven:resolver:";

  /**
   * Creates names for passed in artifacts and metadata. Returned collection has max size of sum of the passed in
   * artifacts and metadata collections, or less. Never returns {@code null}.
   */
  Collection<String> nameLocks( RepositorySystemSession session,
                                Collection<? extends Artifact> artifacts,
                                Collection<? extends Metadata> metadatas );
}
