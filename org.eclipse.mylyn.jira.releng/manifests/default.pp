Exec {
  path => ["/bin/", "/sbin/", "/usr/bin/", "/usr/sbin/"] }

user { "tools":
  ensure     => present,
  membership => minimum,
  shell      => "/bin/bash",
  managehome => true,
}

include "jira"

exec { "disable all":
  command => "find $jira::base -name \"service*.json\" | xargs -i mv {} {}.disabled",
  onlyif  => "test -e $jira::base",
}

jira::defaultsites { "jira":
}