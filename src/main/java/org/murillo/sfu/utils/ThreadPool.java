/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.murillo.sfu.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Sergio
 */
public class ThreadPool {

   private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

   public static void Execute(Runnable task) {
        //Execute
        threadPool.execute(task);
    }
}
