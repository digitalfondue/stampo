   _____ _                              
  / ____| |                             
 | (___ | |_ __ _ _ __ ___  _ __   ___  
  \___ \| __/ _` | '_ ` _ \| '_ \ / _ \ 
  ____) | || (_| | | | | | | |_) | (_) |
 |_____/ \__\__,_|_| |_| |_| .__/ \___/ 
                           | |          
                           |_|          

 Version: ${project.version}
 
 https://github.com/digitalfondue/stampo
===========================================

This archive contain:

 - /bin/* : shell script for launching stampo.
 - /lib/stampo.jar : stampo library with dependencies
 - /LICENSE.txt: license file (APACHE 2)
 - /NOTICE.txt: third party licenses file
 

Install
------------
Add in your PATH the bin directory. Or, if you move the shell/bat script, 
adjust the paths to the lib/stampo.jar accordingly.
 
 
Use
-----------------------
In your stampo project, for processing the site:

> stampo

For running the embedded web server (it will listen to localhost:8080)

> stampo serve

For checking the correctness

> stampo check

For calling the help

> stampo help