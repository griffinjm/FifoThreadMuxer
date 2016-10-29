import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by griffinjm on 29/10/2016.
 */
public interface ThreadMuxer {

    void start();

    void stop();

    void execute(Runnable task);

}
