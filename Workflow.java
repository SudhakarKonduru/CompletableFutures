package sk;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Template code explaining how to use CompletableFuture features for
 * pipelining, composing, transformation and combining activities.
 * Activities will run task in its own thread of control
 * Actions are performed as part of the flow by intermediate threads - no new threads
 */
public class Workflow {
	static ExecutorService workerPool = Executors.newFixedThreadPool(10);
	
	/*
	 * Waiting for first CompletableFuture to complete
	 * Helps to build resilient system where combining both speed and reliability  
	 */
	static class ExtServiceEither {
		CompletableFuture<String> work(String p1) {
			System.out.println("ExtServiceEither - work submitted");
			CompletableFuture<String> fastTaskCompletable = CompletableFuture.supplyAsync(
					() -> fastTask(p1)
					, workerPool);
			CompletableFuture<String> predictableTaskCompletable = CompletableFuture.supplyAsync(
					() -> predictableTask(p1)
					, workerPool);
			return fastTaskCompletable.applyToEither(predictableTaskCompletable,
					result -> "ExtServiceEither::" + result);
		}
		
		String predictableTask(String p1) {
			System.out.println("ExtServiceEither.predictableTask - submitted");
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			return "ExtServiceEither.predictableTask";
		}
		
		String fastTask(String p1) {
			System.out.println("ExtServiceEither.fastTask - submitted");
			try {
				Thread.sleep(2000);
				// Randomly throw exception
				int random = new Random().nextInt(2);
				if (random != 0) {
					Thread.sleep(5000);
				}
			} catch (Exception e) {}
			return "ExtServiceEither.fastTask";
		}
	}
	
	/*
	 * Pipeline(future computations) activities
	 */
	static class ExtServiceCompose {
		CompletableFuture<String> work(String p1) {
			System.out.println("ExtServiceCompose - work submitted");
			CompletableFuture<String> task1Completable = CompletableFuture.supplyAsync(
					() -> task1(p1)
					, workerPool);
			CompletableFuture<String> task2Completable = task1Completable.thenCompose(
					result -> task2(result));
			CompletableFuture<String> finalResult = task2Completable.thenApply(
					result -> "ExtServiceCompose::" + result);
			// Post final results asynchronously to consumer
			finalResult.thenAcceptAsync(
					result -> consumeTask(result)
					, workerPool);
			return finalResult;
		}
		
		String task1(String p1) {
			System.out.println("ExtServiceCompose.task1 - submitted");
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			return "ExtServiceCompose.task1";
		}
		
		CompletableFuture<String> task2(String p1) {
			System.out.println("ExtServiceCompose.task2 - submitted");
			return CompletableFuture.supplyAsync(
					() -> {
						try {
							Thread.sleep(2000);
						} catch (Exception e) {}
						return p1 + "(" + "ExtServiceCompose.task2" + ")";
					}
					, workerPool);
		}
		
		void consumeTask(String p1) {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			System.out.print("ExtServiceCompose.consumeTask done: " + p1);
		}
	}
	
	/*
	 * Combining two activities together
	 */
	static class ExtServiceCombine {
		CompletableFuture<String> work(String p1) {
			System.out.println("ExtServiceCompose - work submitted");
			CompletableFuture<String> task1Completable = CompletableFuture.supplyAsync(
					() -> task1(p1)
					, workerPool);
			CompletableFuture<String> task2Completable = CompletableFuture.supplyAsync(
					() -> task2(p1)
					, workerPool);
			CompletableFuture<String> finalResult = task1Completable.thenCombine(task2Completable,
					(result1, result2) -> "ExtServiceCombine::" + result1 + "+" + result2);
			return finalResult;
		}
		
		String task1(String p1) {
			System.out.println("ExtServiceCombine.task1 - submitted");
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			return "ExtServiceCombine.task1";
		}
		
		String task2(String p1) {
			System.out.println("ExtServiceCombine.task2 - submitted");
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			return "ExtServiceCombine.task2";
		}
	}
	
	/*
	 * Utility method to type conversion as explained in:
	 * https://www.javacodegeeks.com/2013/05/java-8-completablefuture-in-action.html
	 */
	private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
		CompletableFuture<Void> allDoneFuture = CompletableFuture
				.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		return allDoneFuture
				.thenApply(v -> futures.stream().map(future -> future.join()).collect(Collectors.<T> toList()));
	}
	
	/*
	 * Main task
	 */
	public static String mainTask(String p1) throws InterruptedException, ExecutionException {
		ExtServiceEither extServiceEither = new ExtServiceEither();
		CompletableFuture<String> result1Future = extServiceEither.work(p1);
		ExtServiceCompose extServiceCompose = new ExtServiceCompose();
		CompletableFuture<String> result2Future = extServiceCompose.work(p1);
		ExtServiceCombine extServiceCombine = new ExtServiceCombine();
		CompletableFuture<String> result3Future = extServiceCombine.work(p1);
		List<CompletableFuture<String>> combineResults = Arrays.asList(result1Future, result2Future, result3Future);
		CompletableFuture<List<String>> finalCombineResults = sequence(combineResults);
		List<String> finalResults = finalCombineResults.get();
		
		return finalResults.toString();
	}
	
	public static void main(String[] args) {
		try {
			String finalResult = mainTask("Hello");
			System.out.println("Final result: " + finalResult);
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}
}
