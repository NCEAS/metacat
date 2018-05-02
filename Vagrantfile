Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"

  config.vm.provider "virtualbox" do |v|
    v.memory = 1024
    v.cpus = 2
  end

  config.vm.network "forwarded_port", guest: 80, host: 8080
  
  config.vm.synced_folder "./", "/metacat"
  config.vm.synced_folder "../metacatui/src", "/var/www/metacatui"

  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    apt-get install -y apache2 \
                       libapache2-mod-jk \
                       openjdk-7-jdk \
                       postgresql \
                       postgresql-contrib \
                       python3-bcrypt \
                       tomcat7

    # Add sunec.jar to work around an issue with verifying the CN's cert
    # This makes it so we can use the DataONE API against Metacat
    sudo cp /metacat/vagrant/sunec.jar /usr/lib/jvm/java-7-openjdk-amd64/jre/lib/ext/
    sudo service tomcat7 restart

    # Set up Apache2
    sudo a2enmod proxy
    sudo a2enmod proxy_http
    sudo cp /metacat/vagrant/metacat.conf /etc/apache2/sites-available/
    sudo a2dissite 000-default
    sudo a2ensite metacat
    sudo service apache2 restart

    # PostgreSQL
    # Note: Vagrant has issues running as non-root so I'm not sudo -u'ing here
    sudo echo "host     metacat        metacat         127.0.0.1/24    password" >> /etc/postgresql/9.3/main/pg_hba.conf
    sudo -u postgres createdb metacat
    sudo -u postgres psql metacat -c "CREATE USER metacat WITH UNENCRYPTED PASSWORD 'password';"    
    sudo service postgresql restart

    # Make/chown metacat's home in /var
    sudo mkdir /var/metacat
    sudo chown -R tomcat7 /var/metacat

    # Temporarily install Metacat from the a release, then swap in our codebase
    # once that's all running later down in the provision script
    wget https://knb.ecoinformatics.org/software/dist/metacat-bin-2.8.7.tar.gz
    tar xzvf metacat-bin-2.8.7.tar.gz
    sudo cp metacat.war metacat-index.war /var/lib/tomcat7/webapps/
    rm -rf ./*
    
    # Set up Metacat all the way
    # Wait for metacat.properties to be created
    echo "Waiting for Tomcat to unpack the metacat.war"
    while [ ! -f "/var/lib/tomcat7/webapps/metacat/WEB-INF/metacat.properties" ]; do echo $(ls /var/lib/tomcat7/webapps/); sleep 5; done
    # TODO: Finish this so setup is fully automatic
    # python3 /metacat/vagrant/apply_config.py /metacat/vagrant/app.properties /var/lib/tomcat7/webapps/metacat/WEB-INF/metacat.properties

    # Set up an admin user
    cd /var/lib/tomcat7/webapps/metacat/WEB-INF/scripts/bash
    PASS=`python3 -c "import bcrypt; print(bcrypt.hashpw('password', bcrypt.gensalt()))"`
    sudo bash ./authFileManager.sh useradd -h $PASS -dn "admin@localhost"

    # Copy in AJP setup
    sudo cp /metacat/vagrant/workers.properties /etc/apache2/
    sudo cp /metacat/vagrant/jk.conf /etc/apache2/mods-available/
    sudo service apache2 restart
    sudo cp /metacat/vagrant/server.xml /var/lib/tomcat7/conf/server.xml
    sudo service tomcat7 restart
  SHELL
end
