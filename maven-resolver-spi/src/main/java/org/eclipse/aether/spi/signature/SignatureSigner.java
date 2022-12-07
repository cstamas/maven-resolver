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

import java.io.Closeable;
import java.util.Collection;

import org.eclipse.aether.artifact.Artifact;

/**
 * Signature signer performing artifact signing. Instances are created ONCE per session and reused, hence they MUST
 * be thread-safe. Once session ends, the {@link #close()} method is invoked on signer.
 *
 * @since 1.9.3
 */
public interface SignatureSigner extends Closeable
{
    /**
     * Signs the provided artifacts and returns list of signature artifacts, that must have different coordinates
     * than any passed in artifact. In other words, the returned artifacts MUST COMPLEMENT the passed in collection
     * of artifacts. All returned artifacts must have existing content/file associated as well. Must never return
     * {@code null}.
     * <p>
     * How artifacts are grouped, depends on how installing/deploying are configured. For example, these may come
     * per-module, or at end in several collections (but still per module) or all
     */
    Collection<Artifact> sign( Collection<Artifact> artifacts );

    /**
     * Always invoked once the session this instance was created for ends.
     */
    void close();
}
