package cc.unmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

public class Arguments {
	public String file;
	public String username;
	public String password;
	public String command;
	public boolean multipleThreadMode;
	
	public List<String> hosts;
	public String currentHost;
	
	public static boolean isBlank(String src){
		return src == null || src.trim().isEmpty();
	}
	
	public static Arguments processArguments(String[] args){
		Options options = new Options();
		options.addOption(Option.builder("help").desc("Print this help information").build());
		options.addOption(Option.builder("f").longOpt("file").hasArg().argName("file").desc("Host(name or IP) list file, one host per line").build());
		options.addOption(Option.builder("h").longOpt("host").hasArg().argName("string").desc("Host name or IP address").build());
		options.addOption(Option.builder("u").longOpt("username").hasArg().argName("string").required().desc("Username for login server").build());
		options.addOption(Option.builder("p").longOpt("password").hasArg().argName("string").desc("Password for login server").build());
		options.addOption(Option.builder("m").longOpt("multi-thread").desc("Run under multiple thread mode").build());
		options.addOption(Option.builder("c").longOpt("command").hasArg().argName("string").desc("Shell command, with quote if contains space").build());

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			System.out.println(e1.getMessage());
			printHelp(options);
			System.exit(-1);
		}

		if (cmd.hasOption("help")) {
			printHelp(options);
			System.exit(0);
		}
		
		String file = cmd.getOptionValue("f");
		String username = cmd.getOptionValue("u");
		String password = cmd.getOptionValue("p");
		String command = cmd.getOptionValue("c");
		String host = cmd.getOptionValue("h");
		
		boolean multipleThreadMode = cmd.hasOption("m") ? true: false;

		if(isBlank(file)){
			if(isBlank(host)){
				System.out.println("Missing required option: f or h");
				printHelp(options);
				System.exit(-3);
			}
		}
		
		List<String> hosts = new ArrayList<String>();
		if (!isBlank(host)) {
			hosts.add(host.trim());
		} else {
			hosts = loadIPAddressFromFile(file);
		}
		
		password = inputIfNeeded(password, "Password: ", true);

		if (hosts.isEmpty()) {
			System.out.println("must specifiy host by -h or -f parameter");
			System.exit(-3);
		}

		command = inputIfNeeded(command, "Command: ", false);
		
		
		Arguments arguments = new Arguments();
		arguments.file = file;
		arguments.hosts = hosts;
		arguments.username = username;
		arguments.password = password;
		arguments.command = command;
		arguments.multipleThreadMode = multipleThreadMode;
		
		return arguments;
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
		formatter.setOptionComparator(new Comparator<Option>() {
			@Override
			public int compare(Option o1, Option o2) {
				return 0;
			}
		});
		formatter.printHelp("gossh options\nor\njava -jar GoSSH-x.x.x.jar options", options);
	}

	public static List<String> loadIPAddressFromFile(String ipFile) {
		List<String> ips = new ArrayList<>();
		try {
			ips = IOUtils.readLines(new FileInputStream(ipFile));
			Iterator<String> iterator = ips.iterator();
			while(iterator.hasNext()){
				String curr = iterator.next();
				if(curr.trim().isEmpty()) iterator.remove();
			}
		} catch (IOException e) {
			System.out.println("read file " + ipFile + " error.");
			System.exit(-2);
		}
		return ips;
	}
}
