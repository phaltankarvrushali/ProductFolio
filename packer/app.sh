#!/bin/bash
sleep 30
sudo yum update -y
sudo yum upgrade -y
sudo yum install -y unzip

sudo amazon-linux-extras install java-openjdk11 -y
java -version
sudo yum install tomcat -y
sudo alternatives --config java <<< 1
cd
sudo systemctl restart tomcat.service


sudo mv /tmp/webapp.service /etc/systemd/system/webapp.service
sudo systemctl enable webapp.service
sudo systemctl start webapp.service

sudo curl https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm -O
sudo rpm -U ./amazon-cloudwatch-agent.rpm

sudo systemctl enable amazon-cloudwatch-agent
sudo systemctl start amazon-cloudwatch-agent