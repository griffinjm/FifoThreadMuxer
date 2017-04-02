package ie.jgriffin.fifothreadmuxer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by griffin on 01/04/2017.
 */
public class FifoThreadMuxerTest {

    private FifoThreadMuxer fifoThreadMuxer;

    @Before
    public void setup() {

    }

    @After
    public void teardown() throws InterruptedException {
        if (fifoThreadMuxer != null) {
            fifoThreadMuxer.stop();
            fifoThreadMuxer = null;
        }
    }

    @Test
    public void start_NoArgs_StartsWithDefaultNumberOfThreads() throws InterruptedException {
        fifoThreadMuxer = new FifoThreadMuxer();
        fifoThreadMuxer.start();

        assertTrue("Incorrect number of threads", FifoThreadMuxer.DEFAULT_NUM_THREADS ==
                fifoThreadMuxer.getNumberOfMuxers());
    }

    @Test
    public void start_CustomThreadNumber_StartsWithCustomNumberOfThreads() throws InterruptedException {
        int customThreadNumber = 8;
        fifoThreadMuxer = new FifoThreadMuxer(customThreadNumber);
        fifoThreadMuxer.start();

        assertTrue("Incorrect number of threads", customThreadNumber == fifoThreadMuxer.getNumberOfMuxers());
    }

    /**
     * This is more of a functional test as it actually creates amd submits a task to a real muxer, nothing is mocked.
     */
    @Test
    public void execute_SameFifoValue_TasksExecutedInOrder() throws InterruptedException {
        //ensure more than one thread
        fifoThreadMuxer = new FifoThreadMuxer(8);
        fifoThreadMuxer.start();

        final int numberOfTasks = 50;
        final long sleepTime = 100L;
        final CopyOnWriteArrayList<Integer> completionList = new CopyOnWriteArrayList<>();
        List<Runnable> taskList = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            final int taskSequence = i;
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(sleepTime);
                        completionList.add(taskSequence);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            };
            taskList.add(task);
        }

        for (int i = 0; i < numberOfTasks; i++) {
            fifoThreadMuxer.execute("userId-jgriffin", taskList.get(i));
        }

        // wait for the amount of time required to ensure all are processed plus one extra sleepTime to allow for queueing etc.
        Thread.sleep((sleepTime * numberOfTasks) + sleepTime);

        //assert all tasks were completed
        assertEquals("not all tasks were completed", numberOfTasks, completionList.size());
        //assert tasks were completed in order
        for (int i = 0; i < numberOfTasks; i++) {
            assertEquals("task is out of order", i, completionList.get(i).intValue());
        }

    }

}
