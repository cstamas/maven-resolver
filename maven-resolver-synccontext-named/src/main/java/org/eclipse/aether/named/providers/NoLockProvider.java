package org.eclipse.aether.named.providers;

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

import org.eclipse.aether.named.NamedLock;
import org.eclipse.aether.named.NamedLockFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Provider of {@link NoLockNamedLockFactory}, that does not lock at all.
 */
@Singleton
@Named( "nolock" )
public final class NoLockProvider
        implements Provider<NamedLockFactory>
{
    @Override
    public NamedLockFactory get()
    {
        return new NoLockNamedLockFactory();
    }

    /**
     * A no-lock named lock factory that actually does not lock at all.
     */
    public static final class NoLockNamedLockFactory implements NamedLockFactory
    {
        private final NoLockNamedLock noLockNamedLock = new NoLockNamedLock();

        @Override
        public NamedLock getLock( String name )
        {
            return noLockNamedLock;
        }

        @Override
        public void shutdown()
        {
            // nop
        }
    }

    /**
     * A no-lock lock.
     */
    public static final class NoLockNamedLock implements NamedLock
    {
        @Override
        public String name()
        {
            return "nolock";
        }

        @Override
        public boolean lockShared( long time, TimeUnit unit )
        {
            return true;
        }

        @Override
        public boolean lockExclusively( long time, TimeUnit unit )
        {
            return true;
        }

        @Override
        public void unlock()
        {
            // nop
        }

        @Override
        public void close()
        {
            // nop
        }
    }
}
