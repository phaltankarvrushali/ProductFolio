# webapp

# Assignment 9
# Description
This is web application using Java spring boot framework. This assignment consists of user and product management aspect of the application and comprises of RESTful APIs to CREATE, READ and UPDATE user details along with CREATE, READ, UPDATE, PATCH and DELETE product in mySQL database.

# Prerequisites for building and deploying your application locally
1. Clone repository https://github.com/CSYE6225-cloud-computing-neu/webapp.git
2. An IDE for building the application such as IntelliJ or Netbeans
3. Terminal to run git or use IDE pre built terminal
4. This project requires Spring Boot 2.0.0.RELEASE Java 8 or 9 and Spring Framework 5.0.4.RELEASE or above. Explicit build support is provided for Maven 3.2+ and Gradle 4
5. Servlet Containers-Tomcat 8.5; Servlet version 3.1

# Build and Deploy instructions for the web application
1. To build the project run mvn clean install to start the application.
2. Hit APIs from postman or any other tool for invoking APIs.
3. The application requires user authentication so only an authenticated user can perform these actions
4. APIs for using the application-
   POST: http://localhost:8088/v1/user/

   {

    "first_name": "Vrushali",
    "last_name": "Vrushali",
    "password": "Vrushali",
    "username": "vrushali@example.com"

}
   GET: http://localhost:8088/v1/user/0

   PUT: http://localhost:8088/v1/user/0
   {

    "first_name": "Vrushali",
    "last_name": "Phaltankar",
    "password": "Vrushali",
    "username": "vrushali@example.com"

}

PRODUCT:

POST:

http://localhost:8088/v1/product/

{
  "name": "Food",
  "description": "food",
  "sku": "food123",
  "manufacturer": "shreya@jaiswal.com",
  "quantity": "abcd"
}

PUT:
http://localhost:8088/v1/product/2

{
  "name": "Food",
  "description": "food",
  "sku": "food",
  "manufacturer": "VRUSHALI@jaiswal.com",
  "quantity": "20"
}

PATCH:
http://localhost:8088/v1/product/2

{
  "name": "Food",
  "description": "food",
  "sku": "food1"

}

# How to build and deploy the application in local
1. run mvn clean install
2. In IntelliJ, add a run configuration to run the application as a java application by selecting the main class
# Requirements

1. This application supports Token-Based authentication.
2. Create a new user
3. Create an account by providing the following information.
    Email Address
    Password
    First Name
    Last Name
3. Users cannot set values for account_created and account_updated.
4. Password will not be returned in the response payload.
5. User's email id will be used as username and it should be in the form of abc@xyz.com
6. Application returns 400 Bad Request HTTP response code when a user account with the email address already exists.
7. User will only be allowed to update the following fields and an attempt to update any other field will return 400 Bad Request HTTP response code.
    First Name
    Last Name
    Password
8. Only tha user who has an account can create a product.
9. Only the user who has created a product can update or delete a product.
10. account_updated field for the user will be updated when the user update is successful.

# Packer for AMI creation:

1. Install Packer: Download and install Packer on local machine or build server.
2. Choose a base AMI: Select the base AMI to use as a starting point for the build. Packer supports a wide range of base AMIs provided by AWS.
3. Create a Packer template: Write a Packer template in JSON format that specifies the configuration for building the AMI. The template includes the base AMI ID, instance type, provisioners, and other configuration details.
4. Configure Provisioners(here shell): Run the shell file on the instance and perform actions such as installing software, configuring settings, and copying files, etc.
5. Build the AMI: Use the "packer build" command to initiate the build process. Packer creates a new AMI based on the provided configuration.
6. Validate the AMI: After the build process completes, Packer automatically registers the new AMI with AWS. You can use the AWS Management Console, CLI, or API to validate the AMI.

# Packer variables

aws_region="us-east-1"
ami_users=["323454451498", "464155857672"]
instance_type="t2.micro"
source_ami="ami-0dfcb1ef8550277af"

Testdemo
