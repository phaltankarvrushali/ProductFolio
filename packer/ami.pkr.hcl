packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}


variable "aws_region" {
  type = string
  default = "us-east-1"

}
variable "ami_users" {
  type = list(string)
  default = ["323454451498", "464155857672"]
}

variable "instance_type" {
  type = string
  default = "t2.micro"
}

variable "source_ami" {
  type = string
  default = "ami-0dfcb1ef8550277af"
}



locals {
  timestamp = regex_replace(timestamp(), "[- TZ:]", "")
}

source "amazon-ebs" "my-ami" {
  ami_name = "csye6225_${formatdate("YYYY_MM_DD_hh_mm_ss" , timestamp())}"
  ami_description = "AMI for CSYE 6225"

  source_ami = var.source_ami
  ami_users = var.ami_users
  region = var.aws_region

  instance_type = var.instance_type
  ssh_username = "ec2-user"

}

build {
  sources = [
    "source.amazon-ebs.my-ami"
  ]

  provisioner "file" {
    source = "../app_artifact/neu.cloud.assignment-1.0-SNAPSHOT.jar"
    destination = "/home/ec2-user/neu.cloud.assignment-1.0-SNAPSHOT.jar"
  }

  provisioner "file" {
    source = "../app_artifact/cloudwatch-config.json"
    destination = "/home/ec2-user/cloudwatch-config.json"
  }

  provisioner "file" {
    source = "./webapp.service"
    destination = "/tmp/webapp.service"
  }

  post-processor "manifest" {
        output = "manifest.json"
        strip_path = true
        custom_data = {
          my_custom_data = "example"
        }
  }

  provisioner "shell" {
    script = "./app.sh"
  }
}