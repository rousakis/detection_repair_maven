detection_repair_maven
======================

Maven Project with services which correspond on the Change Detection and Validation/Repair Modules 

Configurations Notes
=====================
Folder Config Files contains two sample configuration files
 - change detection parameters for multidimensional model (md_config.properties)
 - change detection parameters for ontological model (config.properties)

If someone plans to use a diferrent virtuoso triple store to store the diachron dataset versions, then the repository fields (i.e., Repository_IP, Repository_Username etc.) should change w.r.t. the Virtuoso setting. 



The current war file which contained in folder dist (DiachronForthServices.war) has been created w.r.t. the configuration file config.properties. If you want to deploy the existing war file in your own application server, note that the existing one was tested successfully on a Tomcat Server 7.0.54 (Windows 7) and the configuration was place in folder C:/. Alternatively, you can make changes in the source code in order to read the properties file from another directory and recreate your own war file.
