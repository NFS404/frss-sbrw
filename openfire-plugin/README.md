Openfire plugin that sends chat commands (starting with /) to SBRW core

# Usage
1. Download soapbox.jar and install as Openfire plugin
2. Set correct system properties (see below)

# System Properties
| Property              | Value for frss-sbrw                                                 |
| --------------------- | ------------------------------------------------------------------- |
| plugin.soapbox.url    | http(s)://\<server address\>/soapbox-race-core/Engine.svc/ofcmdhook |
| plugin.soapbox.secret | \<same as RestAPI secret\>                                          |