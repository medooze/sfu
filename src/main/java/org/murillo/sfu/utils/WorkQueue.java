/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.murillo.sfu.utils;

import java.util.LinkedList;

public class WorkQueue
{
    private final int nThreads;
    private final PoolWorker[] threads;
    private final LinkedList<Runnable> queue;

    public WorkQueue(int nThreads)
    {
        this.nThreads = nThreads;
        queue = new LinkedList();
        threads = new PoolWorker[nThreads];

        //Create each thread
        for (int i=0; i<nThreads; i++)
        {
            //Create new worker thread
            threads[i] = new PoolWorker();
            //Start it
            threads[i].start();
        }
    }

    public void execute(Runnable r)
    {
        synchronized(queue)
        {
            //Add to list
            queue.addLast(r);
            //Notify
            queue.notify();
        }
    }

    public void end()
    {
        synchronized(queue)
        {
            //For each trhead
            for (int i=0; i<nThreads; i++)
                //End it
                threads[i].end();
        }
    }

    private class PoolWorker extends Thread
    {
        private boolean running;

        public PoolWorker()
        {
            //We are running
            running = true;
        }

        public void end()
        {
            //We are not running
            running = false;
            //Interrupt our selves
            interrupt();
        }

        @Override
        public void run()
        {
            Runnable r;

            while (running)
            {
                synchronized(queue) 
                {
                    //Check if there is something on the queue
                    while (queue.isEmpty())
                    {
                        try
                        {
                            //Wait for something to
                            queue.wait();
                        } catch (InterruptedException ignored) {
                            //Check if already running
                            if (!running)
                                //Exit
                                return;
                        }
                    }
                    //Get task
                    r = (Runnable) queue.removeFirst();
                }

                try {
                    //Run work
                    r.run();
                } catch (RuntimeException e) {
                }
            }
        }
    }
}