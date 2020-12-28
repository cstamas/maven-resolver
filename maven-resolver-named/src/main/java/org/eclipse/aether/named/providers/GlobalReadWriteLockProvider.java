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
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provider of {@link GlobalNamedLockFactory}. This uses one "global" {@link ReentrantReadWriteLock}, is really not a
 * "named" lock.
 */
@Singleton
@Named( GlobalReadWriteLockProvider.NAME )
public final class GlobalReadWriteLockProvider
        implements Provider<NamedLockFactory>
{
    public static final String NAME = "global";

    @Override
    public NamedLockFactory get()
    {
        return new GlobalNamedLockFactory();
    }

    /**
     * A global named lock factory that actually uses one singleton named lock instance for all names.
     */
    public static final class GlobalNamedLockFactory implements NamedLockFactory
    {
        private final GlobalNamedLock globalNamedLock = new GlobalNamedLock();

        @Override
        public NamedLock getLock( String name )
        {
            return globalNamedLock;
        }

        @Override
        public void shutdown()
        {
            // nop
        }
    }

    /**
     * A global named lock that uses one single reentrant read-write lock.
     */
    public static final class GlobalNamedLock implements NamedLock
    {
        private final ReentrantReadWriteLock global = new ReentrantReadWriteLock();

        private final ArrayDeque<Lock> steps = new ArrayDeque<>();

        @Override
        public String name()
        {
            return "global";
        }

        @Override
        public boolean lockShared( long time, TimeUnit unit ) throws InterruptedException
        {
            Lock lock = global.readLock();
            if ( lock.tryLock( time, unit ) )
            {
                steps.push( lock );
                return true;
            }
            return false;
        }

        @Override
        public boolean lockExclusively( long time, TimeUnit unit ) throws InterruptedException
        {
            Lock lock = global.writeLock();
            if ( lock.tryLock( time, unit ) )
            {
                steps.push( lock );
                return true;
            }
            return false;
        }

        @Override
        public void unlock()
        {
            try
            {
                steps.pop().unlock();
            }
            catch ( NoSuchElementException e )
            {
                throw new IllegalStateException( "Wrong API usage: unlock w/o lock" );
            }
        }

        @Override
        public void close()
        {
            // nop
        }
    }
}
