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
		options.addOption(Option.builder("f").longOpt("file").hasArg().required().desc("IP Address list file").build());
		options.addOption(Option.builder("u").longOpt("username").hasArg().required().desc("Username").build());
		options.addOption(Option.builder("p").longOpt("password").hasArg().desc("Password").build());
		options.addOption(Option.builder("c").longOpt("command").hasArg().required().desc("Command").build());
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

		if(password == null || password.trim().isEmpty()){

			if(System.console() != null){
				char[] pwd = System.console().readPassword("Password: ");
				password = new String(pwd);
			}else{
				System.out.print("Password: ");
				Scanner input = new Scanner(System.in);
				password = input.next();
			}
		}
		
		List<String> hosts = loadIPAddressFromFile(file);

		try {
			JSch jsch = new JSch();
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			
			for (String host : hosts) {
				Session session = jsch.getSession(username, host, 22);

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

				System.out.println("======"+host+"======");
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
	
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar GoSSH.jar -f -u [-p] -help", options );
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
