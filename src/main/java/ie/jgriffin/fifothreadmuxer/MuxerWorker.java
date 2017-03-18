package ie.jgriffin.fifothreadmuxer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by griffinjm on 02/11/2016.
 */
public class MuxerWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuxerWorker.class);

    private static final String THREAD_NAME_PREFIX = "MuxerWorker-";
    private static final String MUXER_ID = "muxerId";
    private static final String QUEUE_SIZE = "queueSize";

    // used to govern the processing loop in the run method
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private final String muxerWorkerThreadName;
    private final int muxerId;
    private final LinkedBlockingQueue<Runnable> taskQueue;

    public MuxerWorker(int muxerId, LinkedBlockingQueue<Runnable> taskQueue) {
        this.muxerId = muxerId;
        this.taskQueue = taskQueue;
        this.muxerWorkerThreadName = THREAD_NAME_PREFIX + muxerId;
    }

    @Override
    public void run() {
        String methodName = "run";
        LOGGER.info(methodName);

        final String originalThreadName = Thread.currentThread().getName();
        LOGGER.info("renaming thread {} to {}", originalThreadName, muxerWorkerThreadName);
        Thread.currentThread().setName(muxerWorkerThreadName);

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            processNextTask();
        }

        LOGGER.info("MuxerWorker stopped {}:{}", QUEUE_SIZE, taskQueue.size());

        finished.set(true);
        Thread.currentThread().setName(originalThreadName);
    }

    /**
     * A blocking method which will continuously process any tasks in the queue until interrupted.
     */
    private void processNextTask() {
        final String methodName = "processNextTask";
        LOGGER.debug(methodName);

        LOGGER.trace("dequeueing next task");
        try {
            // take the next runnable and execute
            Runnable task = taskQueue.take();
            LOGGER.trace("executing next task in queue");
            task.run();
        } catch (InterruptedException e) {
            // set the interrupted flag again for higher level interrupt handlers
            Thread.currentThread().interrupt();

            if (running.get()) {
                //interrupted while still running
                LOGGER.warn("Processing thread was interrupted while processing a submitted task");
            }

            LOGGER.info("MuxerWorker Thread Interrupted, {}:{}", QUEUE_SIZE, taskQueue.size());
        }
    }

    public int getMuxerId() {
        return muxerId;
    }

    public void stop() {
        running.set(false);
    }
}
