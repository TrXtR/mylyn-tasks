define jira::defaultsites ($base = $jira::base, $userOwner = $jira::userOwner, $userGroup = $jira::userGroup,) {
  include "jira"

  /* Defaults */

  jira::jira {
    base      => $base,
    userOwner => $userOwner,
    userGroup => $userGroup,
  }

  jira::Plugin {
    base      => $base,
    userOwner => $userOwner,
    userGroup => $userGroup,
  }

  jira::Site {
    base      => $base,
    version   => "1.0",
    require   => jira["1.0"],
    userOwner => $userOwner,
    userGroup => $userGroup,
  }

  /* Instances */

  jira::jira { "1.0":
  }

  jira::jira { "1.0.1":
  }

  jira::jira { "trunk":
  }

  /* Plugins */

  jira::plugin { "accountmanagerplugin-0.11":
    url => "http://jira-hacks.org/svn/accountmanagerplugin/0.11",
    egg => "jiraAccountManager",
  }

  jira::plugin { "masterticketsplugin-trunk":
    url => "http://jira-hacks.org/svn/masterticketsplugin/trunk",
    egg => "jiraMasterTickets",
  }

  jira::plugin { "xmlrpcplugin-trunk":
    url => "http://jira-hacks.org/svn/xmlrpcplugin/trunk",
    egg => "jiraXMLRPC",
  }

  /* Sites */

/* Disabling all Sites per bug 448427

  jira::site { "jira-1.0":
    version => "1.0",
    require => jira["1.0"]
  }

  jira::site { "jira-1.0.1":
    version => "1.0.1",
    require => jira["1.0.1"],
    envdefault => true,
  }

  jira::site { "jira-1.0-allbasic":
    allbasicauth => true,
    envinfo      => "AllBasicAuth",
  }

  jira::site { "jira-1.0-cert":
    certauth => true,
    envinfo  => "CertAuth",
  }

  jira::site { "jira-1.0-digest":
    digestauth => true,
    envinfo    => "DigestAuth",
  }

  jira::site { "jira-1.0-form-auth":
    accountmanagerplugin => "0.11",
    envinfo              => "FormAuth",
  }

  jira::site { "jira-trunk":
    version => "trunk",
    require => jira["trunk"],
  }

  jira::site { "jira-test":
    envinfo    => "Test",
  }
*/

}