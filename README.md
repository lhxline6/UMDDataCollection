# UMDDataCollection

## Client
Modify the url in LoginActivity.java and DataCollectionActivity_v2.java files to your own server host. Then recompile the mobile and watch apk respectively.

## Server
set up a LAMP server on your server, and put the Login.php and SaveData.php in the ServerCode folder to /var/www/html/. You can install a phpmyadmin for convenience. You may need to align the mysql username and password in the PHP files to your local mysql database's username and password
If you use AWS EC2, refer to https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/install-LAMP.html
