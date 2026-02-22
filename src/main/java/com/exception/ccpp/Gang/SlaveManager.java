package com.exception.ccpp.Gang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class SlaveManager {

    // THE GOAT, without this UX would be a pain in the ass
    // thats why it has its own package and class
    public final static SlaveDispatcher slaveWorkers = new SlaveDispatcher();
    public final static ArmyDispatcher romanArmy = new ArmyDispatcher();
//    public final static ExecutorService slaveWorkers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//    public final static ForkJoinPool romanArmy = ForkJoinPool.commonPool();

    static public abstract class ServiceDispatcher {
        protected ConcurrentHashMap<String,ConcurrentLinkedDeque<Future<?>>> runningThreads = new ConcurrentHashMap<>();

        protected void addThread(String purpose,Future<?> thread) {
            if (runningThreads.containsKey(purpose)) {
                runningThreads.get(purpose).add(thread);
            } else {
                ConcurrentLinkedDeque q = new ConcurrentLinkedDeque<>();
                q.add(thread);
                runningThreads.put(purpose, q);
            }
        }
        protected <T> void addThreadList(String purpose, List<Future<T>> threads) {
            if (runningThreads.containsKey(purpose)) {
                runningThreads.get(purpose).addAll(threads);
            } else {
                runningThreads.put(purpose, new ConcurrentLinkedDeque<>(threads));
            }
        }
        public void killAll(String purpose, boolean mayInterrupt) {
            if (runningThreads.containsKey(purpose)) {
                for (Future<?> t : runningThreads.get(purpose))
                {
                    t.cancel(mayInterrupt);
                }
                runningThreads.clear();
            }
        }

        public abstract <T> Future<T> submit(String purpose, Callable<T> task);
        public abstract Future<?> submit(String purpose, Runnable task);
        public abstract <T> List<Future<T>> invokeAll(String purpose,Collection<? extends Callable<T>> tasks) throws InterruptedException;
        public abstract void shutdown();
        public abstract void shutdownNow();
    }

    static public class ArmyDispatcher extends ServiceDispatcher {
        private ForkJoinPool romanArmy = ForkJoinPool.commonPool();

        @Override
        public <T> ForkJoinTask<T> submit(String purpose, Callable<T> task) {
            ForkJoinTask<T> f = romanArmy.submit(task);
            romanArmy.submit(()->{});
            addThread(purpose,f);
            return f;
        }

        @Override
        public ForkJoinTask<?> submit(String purpose, Runnable task) {
            ForkJoinTask<?> f = romanArmy.submit(task);
            addThread(purpose,f);
            return f;
        }

        @Override
        public <T> List<Future<T>> invokeAll(String purpose, Collection<? extends Callable<T>> tasks) throws InterruptedException {
            List<Future<T>> futures = romanArmy.invokeAll(tasks);
            addThreadList(purpose, futures);
            return futures;
        }

        @Override
        public void shutdown() {
            romanArmy.shutdown();
        }

        @Override
        public void shutdownNow() {
            romanArmy.shutdownNow();
        }

        public <T>  T invoke(ForkJoinTask<T> task) {
            return romanArmy.invoke(task);
        }
    }

    static public class SlaveDispatcher extends ServiceDispatcher {
        private ExecutorService slaveWorkers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        @Override
        public <T> Future<T> submit(String purpose, Callable<T> task) {
            Future<T> f = slaveWorkers.submit(task);
            addThread(purpose,f);
            return f;
        }

        @Override
        public Future<?> submit(String purpose, Runnable task) {
            Future<?> f = slaveWorkers.submit(task);
            addThread(purpose,f);
            return f;
        }

        @Override
        public <T> List<Future<T>> invokeAll(String purpose, Collection<? extends Callable<T>> tasks) throws InterruptedException {
            List<Future<T>> futures = slaveWorkers.invokeAll(tasks);
            addThreadList(purpose, futures);
            return futures;
        }

        @Override
        public void shutdown() {
            slaveWorkers.shutdown();
        }

        @Override
        public void shutdownNow() {
            slaveWorkers.shutdownNow();
        }
    }

}

