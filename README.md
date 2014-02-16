GoSSH
=====

###Overview

It's convenient to execute shell command against multiple servers specified by IP address list file via SSH.

Build it by JDK7 or later version.

###Instruction
    
Usage: java -jar GoSSH.jar options  
 -c,--command <arg>    Command  
 -f,--file <arg>       IP Address list file  
 -h,--host <arg>       Host  
 -help                 Print this help  
 -p,--password <arg>   Password  
 -u,--username <arg>   Username 
 
###IP Address File

One ip per line

###Build package

Build independent jar package

> mvn assembly:single  

get target/GoSSH-0.0.1-jar-with-dependencies.jar

Execute command

>java -jar target/GoSSH-0.0.1-jar-with-dependencies.jar [options]

We can rename GoSSH-0.0.1-jar-with-dependencies.jar to GoSSH.jar

###Dependencies

1. [JSch](http://www.jcraft.com/jsch/)  
2. [Commons CLI](http://commons.apache.org/proper/commons-cli/)