#!/bin/sh

# The default behavior is to ask for the directory where the project exists
# projectDir=$1
projectDir="/home/skalogerakis/Projects" #Kalogerakis LocalPC

# Untar compressed file into a temporary location
tar -xvf  ${projectDir}/hadoop/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-3.2.2.tar.gz -C $HOME 

echo  "Extracting new version completed.\n\n"

# Prefer rsync than copy. We simply want to update the files in all the subdirectories
rsync --update -raz --progress $HOME/hadoop-hdfs-3.2.2/. $HADOOP_HOME

echo  "Copy new version to previous config completed\n\n"

# Assume that etc folder with configs exists in the directory of the script
#'cp' -rf etc/ $HADOOP_HOME
rsync --update -raz --progress etc/ $HADOOP_HOME

echo  "Completed"

# Remove temporary files
rm -r $HOME/hadoop-hdfs-3.2.2/
