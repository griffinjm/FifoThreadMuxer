

/**
 * Created by griffinjm on 29/10/2016.
 */
public class FifoThreadMuxer implements ThreadMuxer {

    private static final int DEFAULT_NUM_THREADS = 4;
    private final int numThreads;

    public FifoThreadMuxer() {
        this.numThreads = DEFAULT_NUM_THREADS;
    }

    public FifoThreadMuxer(int numThreads) {
        this.numThreads = numThreads;
    }

    public void start() {

    }

    public void stop() {

    }

    public void execute(Runnable task) {

    }

}
