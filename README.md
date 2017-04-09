# FifoThreadMuxer
The FifoThreadMuxer provides a method of maintaining FIFO order for related tasks while using a shared ExecutorService processor. Using an approach similar to the standard Java HashMap Collection, it uses a bucketing approach to queue related tasks. This typically has applications in event driven systems where the order of related tasks needs to be maintained.

[Link to post on personal website](https://g.gravizo.com/svg?%20@startuml;%20%28*%29%20--%3E%20%22execute%28String%20fifoValue,%20Runnable%20task%29%22;%20--%3E%22getMuxerId%28String%20fifoValue%29%22;%20if%20%22%22;%20--%3E[muxerId%20==%201]%20%22Queue%201%22;%20--%3E%22Thread%201%22;%20else;%20--%3E[muxerId%20==%202]%20%22Queue%202%22;%20--%3E%22Thread%202%22;%20else;%20--%3E[muxerId%20==%20n]%20%22Queue%20n%22;%20--%3E%22Thread%20n%22;%20endif;%20@enduml%20%27%3E)


 
### Methodology
1. Hashes the provided fifoValue String
2. "Smears" that first hash with a function provided by the JCP JSR-166 Expert Group (to protect against poor hashes)
3. Bitwise ANDs the returned int with the number of available threads
4. The remainder is then used to determine which thread the submitted task will be executed on
5. Each thread in the ExecutorService is running a MuxerWorker task which continuously performs a blocking take on a single LinkedBlockingQueue of Runnables which ensures that thread will only execute tasks submitted to that queue.

This ensures that tasks submitted with the same fifoValue will be processed in the order that they were submitted while also allowing for 'muxing' of other unrelated events on the same thread.  

### FIFO
>FIFO is an acronym for first in, first out, a method for organizing and manipulating a data buffer, where the oldest (first) entry, or 'head' of the queue, is processed first. It is analogous to processing a queue with first-come, first-served (FCFS) behaviour: where the people leave the queue in the order in which they arrive. [Wikipedia](https://en.wikipedia.org/w/index.php?title=FIFO_(computing_and_electronics)&oldid=773338255)

### Thread
>A thread of execution is the smallest sequence of programmed instructions that can be managed independently by a scheduler, which is typically a part of the operating system. [Wikipedia](https://en.wikipedia.org/w/index.php?title=Thread_(computing)&oldid=769356190)

### Muxer
>A multiplexer (or mux) is a device that selects one of several analog or digital input signals and forwards the selected input into a single line. [Wikipedia](https://en.wikipedia.org/w/index.php?title=Multiplexer&oldid=771706389)
 



