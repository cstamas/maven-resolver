package org.eclipse.aether.named.support;

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
import java.util.concurrent.TimeUnit;

/**
 * Named locks implementation backed by {@link java.nio.channels.FileLock} and implements advisory locking, able to
 * coordinate with other processes using same advisory locking.
 */
public class FileLockNamedLock
        extends NamedLockSupport
{
    private final File file;

    public FileLockNamedLock( final File file, final FileLockNamedLockFactory factory )
    {
        super( file.getPath(), factory );
        this.file = file;
    }

    @Override
    public boolean lockShared( final long time, final TimeUnit unit ) throws InterruptedException
    {
        throw new RuntimeException( "not implemented" );
    }

    @Override
    public boolean lockExclusively( final long time, final TimeUnit unit ) throws InterruptedException
    {
        throw new RuntimeException( "not implemented" );
    }

    @Override
    public void unlock()
    {
        throw new RuntimeException( "not implemented" );
    }
}
