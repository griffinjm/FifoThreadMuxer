import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by griffinjm on 29/10/2016.
 */
public class FifoThreadMuxer implements ThreadMuxer {

    private static final Logger logger = LoggerFactory.getLogger(FifoThreadMuxer.class);

    private static final int DEFAULT_NUM_THREADS = 4;
    private final int numThreads;

    // The ExecutorService used to process the submitted tasks
    private ExecutorService executorService;

    // a map of task queues mapped to integers
    private final Map<Integer, LinkedBlockingQueue<Runnable>> workerQueues;

    // an list of all MuxerWorker runnables
    private final List<MuxerWorker> workers;

    public FifoThreadMuxer() {
        this(DEFAULT_NUM_THREADS);
    }

    public FifoThreadMuxer(int numThreads) {
        this.numThreads = numThreads;
        //size the collections appropriately
        this.workers = new ArrayList<>(numThreads);
        this.workerQueues = new ConcurrentHashMap<>(numThreads);
    }

    @Override
    public void start() {
        startup();
    }

    @Override
    public void stop() {

    }

    @Override
    public void execute(Object fifoValue, Runnable task) {

    }

    private void startup() {
        String methodName = "startup";
        logger.info(methodName);

        initExecutorService();
        startWorkers();
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

        // init each LinkedBlockingQueue
        // link each LinkedBlockingQueue with a thread number
        // start the worker
        for (int i = 0; i < numThreads; i++) {
            LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
            workerQueues.put(i, queue);
            startWorker(i);
        }
    }

    // get the blocking queue
    // create the WorkerRunnable, passing the its queue and threadNumber
    // add a reference to the Worker in the workers list
    // add a reference to the Future returned by submitting the WorkerRunnable to the executorService
    private void startWorker(int threadNumber) {
        final String methodName = "startWorker";
        logger.info(methodName);

        LinkedBlockingQueue<Runnable> workerQueue = workerQueues.get(threadNumber);
        MuxerWorker worker = new MuxerWorker(workerQueue);
        workers.set(threadNumber, worker);
        executorService.submit(worker);
    }

    public static class MuxerWorker implements Runnable {

        private final LinkedBlockingQueue<Runnable> taskQueue;

        public MuxerWorker(LinkedBlockingQueue<Runnable> taskQueue) {
            this.taskQueue = taskQueue;
        }

        @Override
        public void run() {

        }
    }

}
