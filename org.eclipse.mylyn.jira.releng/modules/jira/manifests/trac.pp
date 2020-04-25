define jira::jira ( #
  $version   = "$title",
  $base      = $jira::base,
  $userOwner = $jira::userOwner,
  $userGroup = $jira::userGroup,) {
  $binbase = "$base/bin"
  $srcbase = "$base/src/jira-$version"
  $prefix = "$base/share/jira-$version"

  include "jira"

  exec { "prepare $version":
    command => "mkdir -p $binbase $srcbase $prefix",
    creates => ["$binbase", "$srcbase", "$prefix"],
    require => Exec["prepare jira"],
    user    => "$userOwner",
  }

  file { "$srcbase":
    ensure  => "directory",
    require => Exec["prepare $version"],
    owner   => "$userOwner",
    group   => "$userGroup",
  }

  if $version == "trunk" {
    exec { "exjirat jira $version":
      command => "svn checkout http://svn.edgewall.org/repos/jira/trunk jira-trunk",
      cwd     => "$srcbase",
      creates => "$srcbase/jira-$version",
      require => File["$srcbase"],
      user    => "$userOwner",
    }
  } else {
    exec { "download jira $version":
      command => "wget -O $srcbase/jira-$version.tar.gz http://download.edgewall.org/jira/jira-$version.tar.gz",
      creates => "$srcbase/jira-$version.tar.gz",
      require => File["$srcbase"],
      user    => "$userOwner",
    }

    exec { "exjirat jira $version":
      command => "tar -C $srcbase -xzvf $srcbase/jira-$version.tar.gz",
      require => Exec["download jira $version"],
      creates => "$srcbase/jira-$version",
      user    => "$userOwner",
    }
  }

  file { "$srcbase/install.sh":
    source => "puppet:///modules/jira/install.sh",
    mode   => 755,
    owner  => "$userOwner",
    group  => "$userGroup",
  }

  exec { "install $version":
    command   => "$srcbase/install.sh $srcbase/jira-$version $prefix $version",
    path      => ".",
    logoutput => false,
    require   => Exec["exjirat jira $version"],
    creates   => "$prefix/lib/.provisioned",
    user      => "$userOwner",
  }

  file { "$binbase/jira-$version.cgi":
    content => template('jira/jira.cgi.erb'),
    require => Exec["prepare $version"],
    mode    => 755,
    owner   => "$userOwner",
    group   => "$userGroup",
  }

  file { "$binbase/jiraadmin-$version":
    content => template('jira/jiraadmin.erb'),
    mode    => 755,
    require => Exec["prepare $version"],
    owner   => "$userOwner",
    group   => "$userGroup",
  }

}