package ie.jgriffin.fifothreadmuxer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by griffinjm on 29/10/2016.
 */
public class FifoThreadMuxer implements ThreadMuxer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FifoThreadMuxer.class);

    public static final long DEFAULT_AWAIT_TERMINATION_SECONDS = 30L;
    public static final int DEFAULT_NUM_THREADS = 4;

    // used to prevent multiple threads calling the same method (start/stop) resulting in undefined behaviour
    private final ReentrantLock stateLock = new ReentrantLock(true);
    // represents the current state of the ThreadMuxer
    private final AtomicBoolean running = new AtomicBoolean(false);
    //the number of threads in the ExecutorService
    private final int numThreads;
    // a map of task queues mapped to integers
    private final Map<Integer, LinkedBlockingQueue<Runnable>> workerTaskQueues;
    // a list of all MuxerWorker runnables
    private final List<MuxerWorker> workers;

    // The ExecutorService used to process the submitted tasks
    private ExecutorService executorService;

    public FifoThreadMuxer() {
        this(DEFAULT_NUM_THREADS);
    }

    public FifoThreadMuxer(int numThreads) {
        //Fast power of 2 algo from
        //https://en.wikipedia.org/wiki/Power_of_two#Fast_algorithm_to_check_if_a_positive_number_is_a_power_of_two
        if (numThreads < 2 || (numThreads & (numThreads - 1)) != 0) {
            throw new IllegalArgumentException("invalid argument, must be a power of 2");
        }

        this.numThreads = numThreads;
        //size the collections appropriately
        this.workers = Collections.synchronizedList(new ArrayList<MuxerWorker>(numThreads));
        this.workerTaskQueues = new ConcurrentHashMap<>(numThreads);
    }

    @Override
    public void start() throws InterruptedException {
        final String methodName = "start";
        LOGGER.info(methodName);

        stateLock.lockInterruptibly();

        try {
            if (running.get()) {
                LOGGER.info("already started");
                return;
            }
            running.set(true);
            initExecutorService();
            startWorkers();
        } catch (Exception e) {
            LOGGER.error("exception thrown while attempting start", e);
            throw new IllegalStateException("exception thrown while attempting start", e);
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public void stop() throws InterruptedException {
        final String methodName = "stop";
        LOGGER.info(methodName);

        stateLock.lockInterruptibly();

        try {
            if (!running.get()) {
                LOGGER.info("already stopped");
                return;
            }
            running.set(false);
            stopWorkers();
            shutdownExecutorService();
        } catch (Exception e) {
            LOGGER.error("exception thrown while attempting stop", e);
            throw new IllegalStateException("exception thrown while attempting stop", e);
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Place the passed task on the appropriate muxers queue for execution.
     *
     * @param fifoValue The String value to use for maintaining fifo order.
     * @param task      The task to execute.
     */
    @Override
    public void execute(String fifoValue, Runnable task) {
        final String methodName = "execute";
        LOGGER.info(methodName);

        if (!running.get()) {
            throw new IllegalStateException("cannot execute task, muxer not running");
        }

        if (fifoValue == null || fifoValue.isEmpty()) {
            throw new IllegalArgumentException("fifoValue is invalid");
        }

        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }

        int muxerId = getMuxerId(fifoValue);
        LOGGER.info("adding task to muxer: {}", muxerId);

        workerTaskQueues.get(muxerId).add(task);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getNumberOfMuxers() {
        return numThreads;
    }

    /**
     * For a given String this will always return the same int value. Used to determine which muxer should execute the
     * given task.
     *
     * @param fifoValue The String value to use for maintaining fifo order.
     * @return An int identifying which muxer should execute this rask.
     */
    private int getMuxerId(String fifoValue) {
        return getIndex(smearHash(fifoValue.hashCode()), numThreads);
    }

    /**
     * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions.
     * <p>
     * This function ensures that hashCodes that differ only by constant multiples at each bit position have a bounded
     * number of collisions (approximately 8 at default load factor).
     * <p>
     * This method was written by Doug Lea with assistance from members of JCP JSR-166 Expert Group and released to the
     * public domain, as explained at http://creativecommons.org/licenses/publicdomain
     * <p>
     * As of 2010/06/11, this method is identical to the (package private) hash method in OpenJDK 7's
     * java.util.HashMap class.
     *
     * @param hashCode the original hashcode
     * @return the "smeared" hashcode
     */
    private int smearHash(int hashCode) {
        hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
        return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
    }

    /**
     * Similar to the approach in java's HashMap here:
     * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/java/util/HashMap.java#HashMap.indexFor%28int%2Cint%29
     * <p>
     * Supposedly faster than the classical modulo approach, at least when used with a power of 2, the smearHash
     * function in this class defends against poor hash functions.
     * As the size value (numberThreads) will always be positive then the returned value will also always be positive
     * due to the bitwise operation.
     *
     * @param hash the hash to be used to get the index
     * @param size the size of the index table
     * @return the id of the muxer to use
     */
    private int getIndex(int hash, int size) {
        return hash & (size - 1);
    }

    private void initExecutorService() {
        final String methodName = "initExecutorService";
        LOGGER.info(methodName);

        // shut down the existing executorService if one exists
        if (executorService != null && !executorService.isShutdown()) {
            LOGGER.warn("Old executorService still exists, shutting down now");
            // shutdownNow() will interrupt all threads in the executorService
            executorService.shutdownNow();
        }

        LOGGER.info("Initialising executorService");
        executorService = Executors.newFixedThreadPool(numThreads);
        LOGGER.info("Initialised executorService: {}", executorService);
    }

    private void startWorkers() {
        final String methodName = "startWorkers";
        LOGGER.info(methodName);

        for (int i = 0; i < numThreads; i++) {
            startWorker(i);
        }
    }

    // link each LinkedBlockingQueue with a muxerId
    // create the MuxerWorker, passing its taskQueue and muxerId
    // add a reference to the taskQueue in the taskQueue map
    // add a reference to the MuxerWorker in the workers list
    private void startWorker(int muxerId) {
        final String methodName = "startWorker";
        LOGGER.info(methodName);
        LOGGER.info("muxerId: {}", muxerId);

        LinkedBlockingQueue<Runnable> workerQueue = new LinkedBlockingQueue<>();
        workerTaskQueues.put(muxerId, workerQueue);

        MuxerWorker worker = new MuxerWorker(muxerId, workerQueue);
        workers.add(worker);
        executorService.submit(worker);
    }

    private void stopWorkers() {
        final String methodName = "stopWorkers";
        LOGGER.info(methodName);

        synchronized (workers) {
            for (MuxerWorker worker : workers) {
                LOGGER.info("stopping worker: {}", worker.getMuxerId());
                worker.stop();
            }
        }
    }

    private void shutdownExecutorService() {
        final String methodName = "shutdownExecutorService";
        LOGGER.info(methodName);

        if (executorService != null) {
            LOGGER.info("stopping executorService...");

            executorService.shutdownNow();
            try {
                boolean terminated = executorService
                        .awaitTermination(DEFAULT_AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS);
                LOGGER.info("executorService terminated within {}s timeout: {}", DEFAULT_AWAIT_TERMINATION_SECONDS,
                            terminated);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOGGER.error("InterruptedException thrown while awaiting executorService termination", ex);
            }
            executorService = null;
        }
    }

}
