define jira::service (
  $envid      = "$title",
  $version,
  $envtype    = "jira",
  $envinfo    = "",
  $envdefault = false,
  $envmode    = "XML-RPC",
  $accessmode = "XML_RPC",
  $base       = $jira::base,
  $userOwner  = $jira::userOwner,
  $userGroup  = $jira::userGroup,) {
  $envbase = "$base/var/$envid"

  file { "$envbase/service-$title.json":
    content => template('jira/service.json.erb'),
    require => File["$envbase"],
    owner   => "$userOwner",
    group   => "$userGroup",
  }
}
