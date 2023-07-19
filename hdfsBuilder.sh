#!/bin/sh


# DIRECTIONS FOR CLUSTER
# 1) Build Locally using this script
# 2) Perform remote to the cluster using this command copy scp -P 2222 hadoop-dist/target/hadoop-3.2.2.tar.gz  skalogerakis@139.91.183.36:/media/localdisk/skalogerakis/
# 3) Before building make sure to delete all existing hadoop tmp files rm -r /tmp/hadoop*
# 4) Untar file in the cluster 	tar -xvf ${HADOOP_VERSION}.tar.gz --directory ${MY_HOME} --one-top-level=hadoop --strip-components 1
# 5) Use hdfs-standalone script (with -d) parameter to update the config files in all nodes
# 6) Again make sure to delete all existing hadoop tmp files rm -r /tmp/hadoop*
# 7) Format only in the NN

# The default behavior is to ask for the directory where the project exists
PROJECT_DIR="/home/skalogerakis/Documents/Workspace/hadoop/" #Kalogerakis LocalPC
VERSION="3.2.2"

echo "Starting maven compilation\n"
cd $PROJECT_DIR
cd hadoop-maven-plugins/
sudo mvn clean install
cd ..
sudo mvn package -Pdist -DskipTests -Dtar -Dmaven.javadoc.skip=true
cd ..
echo -e "Hadoop "$VERSION" compiled\n"


# Untar compressed file into a temporary location
# THIS IS THE COMPRESSED FILE WE WANT
tar -xvf  ${PROJECT_DIR}hadoop-dist/target/hadoop-${VERSION}.tar.gz -C $HOME
echo  "Extracting new version completed.\n\n"

sudo rm -r /tmp/hadoop*
rm -r $HADOOP_HOME

# Prefer rsync than copy. We simply want to update the files in all the subdirectories
rsync --update -raz --progress $HOME/hadoop-${VERSION}/. $HADOOP_HOME
echo  "Copy new version to previous config completed\n\n"

# Remove temporary files
rm -r $HOME/hadoop-${VERSION}/

