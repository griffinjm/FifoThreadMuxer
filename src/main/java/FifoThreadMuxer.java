import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final Map<Integer, LinkedBlockingQueue<Runnable>> workerTaskQueues;

    // a list of all MuxerWorker runnables
    private final List<MuxerWorker> workers;

    public FifoThreadMuxer() {
        this(DEFAULT_NUM_THREADS);
    }

    public FifoThreadMuxer(int numThreads) {
        this.numThreads = numThreads;
        //size the collections appropriately
        this.workers = new ArrayList<>(numThreads);
        this.workerTaskQueues = new ConcurrentHashMap<>(numThreads);
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

}
