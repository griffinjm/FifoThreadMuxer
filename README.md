# FifoThreadMuxer
The FifoThreadMuxer provides a method of maintaining FIFO order for related events while using a shared ExecutorService processor. It hashes the provided fifoValue String and then mods the returned int with the number of available threads. The remainder is then used to determine which thread the submitted task will be executed on. This ensures that tasks submitted with the same fifoValue will be processed in the order that they were submitted. This typically has applications in event driven systems where the order of related events needs to be maintained.  

### FIFO
>FIFO is an acronym for first in, first out, a method for organizing and manipulating a data buffer, where the oldest (first) entry, or 'head' of the queue, is processed first. It is analogous to processing a queue with first-come, first-served (FCFS) behaviour: where the people leave the queue in the order in which they arrive. [Wikipedia](https://en.wikipedia.org/w/index.php?title=FIFO_(computing_and_electronics)&oldid=773338255)

### Thread
>A thread of execution is the smallest sequence of programmed instructions that can be managed independently by a scheduler, which is typically a part of the operating system. [Wikipedia](https://en.wikipedia.org/w/index.php?title=Thread_(computing)&oldid=769356190)

### Muxer
>A multiplexer (or mux) is a device that selects one of several analog or digital input signals and forwards the selected input into a single line. [Wikipedia](https://en.wikipedia.org/w/index.php?title=Multiplexer&oldid=771706389)
 



