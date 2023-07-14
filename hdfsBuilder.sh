#!/bin/sh

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
tar -xvf  ${PROJECT_DIR}hadoop-dist/target/hadoop-${VERSION}.tar.gz -C $HOME
echo  "Extracting new version completed.\n\n"

sudo rm -r /tmp/hadoop*
rm -r $HADOOP_HOME

# Prefer rsync than copy. We simply want to update the files in all the subdirectories
rsync --update -raz --progress $HOME/hadoop-${VERSION}/. $HADOOP_HOME
echo  "Copy new version to previous config completed\n\n"

# Remove temporary files
rm -r $HOME/hadoop-${VERSION}/
