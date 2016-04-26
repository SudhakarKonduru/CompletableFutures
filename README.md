# CompletableFutures
Java 8 Completable Future for Asynchronous Programming: Helps to build responsive applications elegantly

Following is the definitive guide explaining the capabilities
http://www.nurkiewicz.com/2013/05/java-8-definitive-guide-to.html

I have put together sample workflow which explains how to use and composes various features such as pipelining, composing, transforming and combining activities. Activities will run in its own thread of control, Actions are performed as part of the flow by intermediate threads(no new threads, see below)

In a nutshell, roles of threads:
 Main thread which initiates workflow, submit tasks and wait for the work to be done
 Worker threads which perform individual long running activities
 Intermediate work which involves applying transformation actions and glueing intermediate results - this also coordinate and carried out by worker threads as in step2

Prior to Completable Future, task 3 was performed by main thread in blocking way where it has to wait and carryout intermediate results and complete delegation was not possible.
