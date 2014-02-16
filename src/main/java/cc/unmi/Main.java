package cc.unmi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class Main {

	public static void main(String[] args) {
		
		Options options = new Options();
		options.addOption(Option.builder("f").longOpt("file").hasArg().desc("IP Address list file").build());
		options.addOption(Option.builder("u").longOpt("username").hasArg().required().desc("Username").build());
		options.addOption(Option.builder("p").longOpt("password").hasArg().desc("Password").build());
		options.addOption(Option.builder("c").longOpt("command").hasArg().desc("Command").build());
		options.addOption(Option.builder("h").longOpt("host").hasArg().desc("Host").build());
		options.addOption(Option.builder("help").desc("Print this help").build());
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e1) {
			printHelp(options);
			System.exit(-1);
		}
		
		if(cmd.hasOption("help")){
			printHelp(options);
			System.exit(0);
		}
		
		String file = cmd.getOptionValue("f");
		String username = cmd.getOptionValue("u");
		String password = cmd.getOptionValue("p");
		String command = cmd.getOptionValue("c");
		String host = cmd.getOptionValue("h");

		password = inputIfNeeded(password, "Password: ", true);
		
		List<String> hosts = new ArrayList<String>();
		
		if(host != null && !host.trim().isEmpty()){
			hosts.add(host.trim());
		}else{
			hosts = loadIPAddressFromFile(file);
		}
		
		if(hosts.isEmpty()){
			System.out.println("must specifiy host by -h or -f parameter");
			System.exit(-3);
		}
		
		command = inputIfNeeded(command, "Command: ", false);
		
		try {
			JSch jsch = new JSch();
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			
			for (String currentHost : hosts) {
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

				// FileOutputStream fos=new FileOutputStream("/tmp/stderr");
				// ((ChannelExec)channel).setErrStream(fos);
				((ChannelExec) channel).setErrStream(System.err);

				InputStream in = channel.getInputStream();

				channel.connect();

				System.out.println("["+currentHost+"]---------------------------------------------------------------");
				byte[] tmp = new byte[1024];
				while (true) {
					while (in.available() > 0) {
						int i = in.read(tmp, 0, 1024);
						if (i < 0)
							break;
						System.out.print(new String(tmp, 0, i));
					}
					if (channel.isClosed()) {
						System.out.println("exit: " + channel.getExitStatus());
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (Exception ee) {
					}
				}
				channel.disconnect();
				session.disconnect();
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	private static String inputIfNeeded(String parameter, String prompt, boolean isPassword) {
		if (parameter == null || parameter.trim().isEmpty()) {
			if (isPassword && System.console() != null) {
				char[] pwd = System.console().readPassword(prompt);
				parameter = new String(pwd);
			} else {
				System.out.print(prompt);
				try (Scanner input = new Scanner(System.in)) {
					parameter = input.nextLine();
				}
			}
		}
		
		if (parameter.trim().isEmpty()) {
			return inputIfNeeded(parameter, prompt, isPassword);
		}
		
		return parameter.trim();
	}
	
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar GoSSH.jar options", options );
	}

	public static List<String> loadIPAddressFromFile(String ipFile){
		File file = new File(ipFile);
		List<String> ips = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line = null;
			while((line=br.readLine()) != null){
				if(!line.trim().isEmpty()){
					ips.add(line.trim());
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println(ipFile + " does not exist");
			System.exit(-1);
		} catch (IOException e){
			System.out.println("read " + ipFile + " error.");
			System.exit(-2);
		} finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ips;
	}
}
