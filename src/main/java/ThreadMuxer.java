/**
 * Created by griffinjm on 29/10/2016.
 */
public interface ThreadMuxer {

    void start();

    void stop();

    void execute(String fifoValue, Runnable task);

}
