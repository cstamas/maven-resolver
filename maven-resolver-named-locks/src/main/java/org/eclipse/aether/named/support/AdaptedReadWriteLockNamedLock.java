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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Named lock support implementation that is using {@link ReadWriteLock} instances.
 */
public class AdaptedReadWriteLockNamedLock
        extends NamedLockSupport
{
    /**
     * Wrapper for read-write-lock-like stuff, that do not share common ancestor.
     */
    public interface AdaptedReadWriteLock
    {
        AdaptedLock readLock();

        AdaptedLock writeLock();
    }

    /**
     * Wrapper for lock-like stuff, that do not share common ancestor.
     */
    public interface AdaptedLock
    {
        boolean tryLock( long time, TimeUnit unit ) throws InterruptedException;

        void unlock();
    }

    /**
     * Adapter for read-write-locks descending from Java {@link ReadWriteLock}.
     */
    public static final class JVMReadWriteLock implements AdaptedReadWriteLock
    {
        private final JVMLock readLock;

        private final JVMLock writeLock;

        public JVMReadWriteLock( final ReadWriteLock readWriteLock )
        {
            Objects.requireNonNull( readWriteLock );
            this.readLock = new JVMLock( readWriteLock.readLock() );
            this.writeLock = new JVMLock( readWriteLock.writeLock() );
        }

        @Override
        public AdaptedLock readLock()
        {
            return readLock;
        }

        @Override
        public AdaptedLock writeLock()
        {
            return writeLock;
        }
    }

    private static final class JVMLock implements AdaptedLock
    {
        private final Lock lock;

        private JVMLock( final Lock lock )
        {
            this.lock = lock;
        }

        @Override
        public boolean tryLock( final long time, final TimeUnit unit ) throws InterruptedException
        {
            return lock.tryLock( time, unit );
        }

        @Override
        public void unlock()
        {
            lock.unlock();
        }
    }

    private enum Step
    {
        SHARED, EXCLUSIVE, NOOP
    }

    private final ThreadLocal<Deque<Step>> threadSteps;

    private final AdaptedReadWriteLock readWriteLock;

    public AdaptedReadWriteLockNamedLock( final String name,
                                          final NamedLockFactorySupport factory,
                                          final AdaptedReadWriteLock readWriteLock )
    {
        super( name, factory );
        this.threadSteps = ThreadLocal.withInitial( ArrayDeque::new );
        this.readWriteLock = readWriteLock;
    }

    @Override
    public boolean lockShared( final long time, final TimeUnit unit ) throws InterruptedException
    {
        Deque<Step> steps = threadSteps.get();
        if ( !steps.isEmpty() )
        { // we already own shared or exclusive lock
            steps.push( Step.NOOP );
            return true;
        }
        if ( readWriteLock.readLock().tryLock( time, unit ) )
        {
            steps.push( Step.SHARED );
            return true;
        }
        return false;
    }

    @Override
    public boolean lockExclusively( final long time, final TimeUnit unit ) throws InterruptedException
    {
        Deque<Step> steps = threadSteps.get();
        if ( !steps.isEmpty() )
        { // we already own shared or exclusive lock
            if ( steps.contains( Step.EXCLUSIVE ) )
            {
                steps.push( Step.NOOP );
                return true;
            }
            else
            {
                return false; // Lock upgrade not supported
            }
        }
        if ( readWriteLock.writeLock().tryLock( time, unit ) )
        {
            steps.push( Step.EXCLUSIVE );
            return true;
        }
        return false;
    }

    @Override
    public void unlock()
    {
        Deque<Step> steps = threadSteps.get();
        if ( steps.isEmpty() )
        {
            throw new IllegalStateException( "Wrong API usage: unlock w/o lock" );
        }
        Step step = steps.pop();
        if ( Step.SHARED == step )
        {
            readWriteLock.readLock().unlock();
        }
        else if ( Step.EXCLUSIVE == step )
        {
            readWriteLock.writeLock().unlock();
        }
    }
}
