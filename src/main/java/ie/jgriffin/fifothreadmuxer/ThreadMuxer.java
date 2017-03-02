package ie.jgriffin.fifothreadmuxer;

/**
 * Created by griffinjm on 29/10/2016.
 */
public interface ThreadMuxer {

    void start() throws InterruptedException;

    void stop() throws InterruptedException;

    void execute(String fifoValue, Runnable task);

}
