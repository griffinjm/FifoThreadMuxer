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

    private static final Logger logger = LoggerFactory.getLogger(FifoThreadMuxer.class);

    private static final long DEFAULT_AWAIT_TERMINATION_SECONDS = 30L;
    private static final int DEFAULT_NUM_THREADS = 4;

    // used to prevent multiple threads calling the same method (start/stop) resulting in undefined behaviour
    private final ReentrantLock stateLock = new ReentrantLock(true);
    // represents the current state of the ThreadMuxer
    private final AtomicBoolean running = new AtomicBoolean(false);

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
        this.numThreads = numThreads;
        //size the collections appropriately
        this.workers = Collections.synchronizedList(new ArrayList<MuxerWorker>(numThreads));
        this.workerTaskQueues = new ConcurrentHashMap<>(numThreads);
    }

    @Override
    public void start() throws InterruptedException {
        final String methodName = "start";
        logger.info(methodName);

        if (running.get()) {
            logger.info(methodName, "already started");
            return;
        }

        stateLock.lockInterruptibly();
        try {
            initExecutorService();
            startWorkers();
            running.set(true);
        } catch (Exception e) {
            logger.error("exception thrown while attempting start", e);
            throw new IllegalStateException("exception thrown while attempting start", e);
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public void stop() throws InterruptedException {
        final String methodName = "stop";
        logger.info(methodName);

        if (!running.get()) {
            logger.info(methodName, "already stopped");
            return;
        }

        stateLock.lockInterruptibly();
        try {
            shutdownExecutorService();
            stopWorkers();
            running.set(false);
        } catch (Exception e) {
            logger.error("exception thrown while attempting stop", e);
            throw new IllegalStateException("exception thrown while attempting stop", e);
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Place the passed task on the appropriate muxer's queue for execution.
     *
     * @param fifoValue The String value to use for maintaining fifo order.
     * @param task      The task to execute.
     */
    @Override
    public void execute(String fifoValue, Runnable task) {
        final String methodName = "execute";
        logger.info(methodName);

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
        logger.info(methodName, "adding task to muxer: {}", muxerId);

        workerTaskQueues.get(muxerId).add(task);
    }

    /**
     * For a given String this will always return the same int value. Used to determine which muxer should execute the
     * given task.
     *
     * @param fifoValue The String value to use for maintaining fifo order.
     * @return An int identifying which muxer should execute this rask.
     */
    private int getMuxerId(String fifoValue) {
        return fifoValue.hashCode() % numThreads;
    }

    private void initExecutorService() {
        final String methodName = "initExecutorService";
        logger.info(methodName);

        // shut down the existing executorService if one exists
        if (executorService != null && !executorService.isShutdown()) {
            logger.error(methodName, "Old executorService still exists, shutting down now");
            // shutdownNow() will interrupt all threads in the executorService
            executorService.shutdownNow();
        }

        logger.info(methodName, "Initialising executorService");
        executorService = Executors.newFixedThreadPool(numThreads);
        logger.info(methodName, "Initialised executorService: {}", executorService);
    }

    private void startWorkers() {
        final String methodName = "startWorkers";
        logger.info(methodName);

        for (int i = 0; i < numThreads; i++) {
            startWorker(i);
        }
    }

    // link each LinkedBlockingQueue with a muxerId
    // create the MuxerWorker, passing the its taskQueue and muxerId
    // add a reference to the taskQueue in the taskQueue map
    // add a reference to the MuxerWorker in the workers list
    private void startWorker(int muxerId) {
        final String methodName = "startWorker";
        logger.info(methodName);

        LinkedBlockingQueue<Runnable> workerQueue = new LinkedBlockingQueue<>();
        workerTaskQueues.put(muxerId, workerQueue);

        MuxerWorker worker = new MuxerWorker(muxerId, workerQueue);
        workers.set(muxerId, worker);
        executorService.submit(worker);
    }

    private void stopWorkers() {
        final String methodName = "stopWorkers";
        logger.info(methodName);

        synchronized (workers) {
            for (MuxerWorker worker : workers) {
                logger.info(methodName, "Stopping worker: {}", worker.getMuxerId());
                worker.stop();
            }
        }
    }

    private void shutdownExecutorService() {
        final String methodName = "shutdownExecutorService";
        logger.info(methodName);

        if (executorService != null) {
            logger.info(methodName, "Stopping executorService...");

            executorService.shutdownNow();
            try {
                boolean terminated = executorService
                        .awaitTermination(DEFAULT_AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS);
                logger.info(methodName, "executorService terminated within {} timeout: {}", 30,
                            terminated);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.error(methodName, ex, "InterruptedException thrown while awaiting executorService termination");
            }
            executorService = null;
        }
    }

}
