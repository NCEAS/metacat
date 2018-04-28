Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network "forwarded_port", guest: 80, host: 8080
  config.vm.network "forwarded_port", guest: 8080, host: 8888
  config.vm.synced_folder "./", "/metacat"
  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    apt-get install -y apache2 \
                       openjdk-7-jdk \
                       postgresql \
                       postgresql-contrib \
                       python3-bcrypt \
                       tomcat7

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
    # TODO: Disabled for now as I debug issues with authentication
    # python3 /metacat/vagrant/apply_config.py /metacat/vagrant/app.properties /var/lib/tomcat7/webapps/metacat/WEB-INF/metacat.properties

    # Set up an admin user
    cd /var/lib/tomcat7/webapps/metacat/WEB-INF/scripts/bash
    PASS=`python3 -c "import bcrypt; print(bcrypt.hashpw('password', bcrypt.gensalt()))"`
    sudo bash ./authFileManager.sh useradd -h $PASS -dn "admin@localhost"

    # Link in the folder(s) we want
    # TODO: Set this up so it runs automatically
    # sudo ln -f -s /metacat/lib/style/skins/metacatui /var/lib/tomcat7/webapps/metacat/style/skins/metacatui
  SHELL
end
