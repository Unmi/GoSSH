package cc.unmi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class Main {

	public static void main(String[] args) {
		final Arguments arguments = Arguments.processArguments(args);

		if(arguments.multipleThreadMode){
			executeInMultipleThreadMode(arguments);
		}else{
			executeInSingleThreadMode(arguments);
		}
	}

	private static void executeInMultipleThreadMode(final Arguments arguments) {
		ExecutorService threadPool = Executors.newFixedThreadPool(5);
		List<Callable<String>> callables = new ArrayList<Callable<String>>();

		for (final String currentHost : arguments.hosts) {
			callables.add(new Callable<String>() {
				public String call() throws Exception {
					return executeCommand(currentHost, arguments.username, arguments.password, arguments.command);
				}
			});
		}

		List<Future<String>> futures;
		try {
			futures = threadPool.invokeAll(callables);
			for (Future<String> future : futures) {
//				System.out.println(future.get());
				future.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			System.err.println(e.getMessage());
			System.exit(-3);
		}

		threadPool.shutdown();
	}
	
	//think about newSingleThreadExecutor
	private static void executeInSingleThreadMode(final Arguments arguments) {

		for (final String currentHost : arguments.hosts) {
			System.out.println(executeCommand(currentHost, arguments.username, arguments.password, arguments.command));
		}
	}
	
	private static String executeCommand(String currentHost, String username, String password, String command){
		String output = "\n[" + currentHost + "]---------------------------------------------------------------\n";
		try {
			JSch jsch = new JSch();
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");

			Session session = jsch.getSession(username, currentHost, 22);

			session.setPassword(password);
			session.setConfig(config);

			session.connect();

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// X Forwarding
			// channel.setXForwarding(true);

			// channel.setInputStream(System.in);
			channel.setInputStream(null);

			// channel.setOutputStream(System.out);

			// FileOutputStream fos=new
			// FileOutputStream("/tmp/stderr");
			// ((ChannelExec)channel).setErrStream(fos);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();

			channel.connect();

			output += "\n" + IOUtils.toString(in);
			try {
				Thread.sleep(500);
			} catch (Exception ee) {
			}
			output += "\nexit [" + currentHost + "], status: " + channel.getExitStatus() + "\n";

			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			output += "\nError: " + e.getMessage();
		}
//		return output;
		System.out.println(output);
		return "";
	}

}
