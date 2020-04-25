
define jira::site (
  $envid                = "$title",
  $version,
  $xmlrpcplugin         = "trunk",
  $accountmanagerplugin = "",
  $allbasicauth         = false,
  $certauth             = false,
  $digestauth           = false,
  $base                 = $jira::base,
  $envtype              = "jira",
  $envinfo              = "",
  $envdefault           = false,
  $userOwner            = $jira::userOwner,
  $userGroup            = $jira::userGroup,) {
  $prefix = "$base/share/jira-$version"
  $envbase = "$base/var/$envid"
  $env = "$base/var/$envid/env"
  $conf = "$base/conf.d"

  include "jira"

  exec { "prepare $envbase":
    command => "mkdir -p $base/bin $base/conf.d $base/src $base/var $envbase",
    creates => "$envbase",
    require => Exec["prepare jira"],
    user    => "$userOwner",
  }

  file { "$envbase":
    ensure  => "directory",
    owner   => "www-data",
    group   => "$userGroup",
    require => Exec["prepare $envbase"],
  }

  file { "$envbase/svn":
    ensure  => "directory",
    owner   => "www-data",
    group   => "$userGroup",
    require => File["$envbase"],
  }

  exec { "svn create $envid":
    command => "/usr/bin/svnadmin create $envbase/svn",
    require => File["$envbase/svn"],
    creates => "$envbase/svn/format",
  }

  exec { "initenv $envid":
    command => "$base/bin/jiraadmin-$version $env initenv $envid sqlite:db/jira.db svn $envbase/svn",
    creates => "$env",
    user    => "www-data",
    require => Exec["svn create $envid"],
  }

  exec { "add admin permissions $envid":
    command     => "$base/bin/jiraadmin-$version $env permission add admin@mylyn.eclipse.org jira_ADMIN",
    user        => "www-data",
    environment => "PYTHON_EGG_CACHE=/tmp/eggs",
    require     => Exec["initenv $envid"],
    onlyif      => 
    "$base/bin/jiraadmin-$version $env permission list admin@mylyn.eclipse.org | (grep -qE 'admin.*jira_ADMIN'; test $? != 0)"
  }

  exec { "add tests permissions $envid":
    command     => 
    "$base/bin/jiraadmin-$version $env permission add tests@mylyn.eclipse.org TICKET_ADMIN TICKET_CREATE TICKET_MODIFY",
    user        => "www-data",
    environment => "PYTHON_EGG_CACHE=/tmp/eggs",
    require     => Exec["initenv $envid"],
    onlyif      => 
    "$base/bin/jiraadmin-$version $env permission list tests@mylyn.eclipse.org | (grep -qE 'tests.*TICKET_ADMIN'; test $? != 0)"
  }

  exec { "add user permissions $envid":
    command     => "$base/bin/jiraadmin-$version $env permission add user@mylyn.eclipse.org TICKET_CREATE TICKET_MODIFY",
    user        => "www-data",
    environment => "PYTHON_EGG_CACHE=/tmp/eggs",
    require     => Exec["initenv $envid"],
    onlyif      => 
    "$base/bin/jiraadmin-$version $env permission list user@mylyn.eclipse.org | (grep -qE 'user.*TICKET_CREATE'; test $? != 0)"
  }

  file { "$env/conf/jira.ini":
    content => template('jira/jira.ini.erb'),
    require => Exec["initenv $envid"],
    owner   => "www-data",
    group   => "$userGroup",
  }

  file { "$conf/$envid.conf":
    content => template('jira/jira.conf.erb'),
    require => Exec["prepare $envbase"],
    owner   => "$userOwner",
    group   => "$userGroup",
  }

  if $digestauth {
    file { "$envbase/htpasswd.digest":
      content => template('jira/htpasswd.digest.erb'),
      require => File["$envbase"],
      owner   => "$userOwner",
      group   => "$userGroup",
    }
  } else {
    file { "$envbase/htpasswd":
      content => template('jira/htpasswd.erb'),
      require => File["$envbase"],
      owner   => "$userOwner",
      group   => "$userGroup",
    }
  }

  file { "$envbase/jira-$version.fcgi":
    content => template('jira/jira.fcgi.erb'),
    mode    => 755,
    require => File["$envbase"],
    owner   => "$userOwner",
    group   => "$userGroup",
  }

  if $xmlrpcplugin {
    file { "$env/plugins/jiraXMLRPC.egg":
      source  => "$base/src/xmlrpcplugin-$xmlrpcplugin/src/dist/jiraXMLRPC.egg",
      require => Exec["initenv $envid"],
      owner   => "$userOwner",
      group   => "$userGroup",
    }

    exec { "add xmlrpc permissions $envid":
      command     => "$base/bin/jiraadmin-$version $env permission add tests@mylyn.eclipse.org XML_RPC",
      user        => "www-data",
      environment => "PYTHON_EGG_CACHE=/tmp/eggs",
      require     => File["$env/plugins/jiraXMLRPC.egg"],
      onlyif      => 
      "$base/bin/jiraadmin-$version $env permission list tests@mylyn.eclipse.org | (grep -qE 'tests.*XML_RPC'; test $? != 0)"
    }
  }

  if $accountmanagerplugin {
    file { "$env/plugins/jiraAccountManager.egg":
      source  => "$base/src/accountmanagerplugin-$accountmanagerplugin/src/dist/jiraAccountManager.egg",
      require => Exec["initenv $envid"],
      owner   => "$userOwner",
      group   => "$userGroup",
    }
  }

  exec { "add $envbase to /etc/apache2/conf.d/jira.conf":
    command => "echo 'Include $base/conf.d/[^.#]*\n' >> /etc/apache2/conf.d/jira.conf",
    require => File["$conf/$envid.conf"],
    notify  => Service["apache2"],
    onlyif  => "grep -qe '^Include $base/conf.d' /etc/apache2/conf.d/jira.conf; test $? != 0"
  }

  jira::service { "${envid}-xml-rpc":
    envid      => "$title",
    version    => "$version",
    envinfo    => "$envinfo",
    envdefault => $envdefault,
    envmode    => "XML-RPC",
    accessmode => "XML_RPC",
  }

  jira::service { "${envid}-web":
    envid      => "$title",
    version    => "$version",
    envinfo    => "$envinfo",
    envdefault => false,
    envmode    => "Web",
    accessmode => "jira_0_9",
  }

}
