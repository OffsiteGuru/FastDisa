
## FastDisa Project Brief

### Synopsis
FastDisa is an Android app with two primary functions:
1) To capture outgoing calls, and route them though Asterisk
2) To send and receive SMS text messages via a XMPP gateway.

**Settings**
These are the global app settings. They are listed here because they are referenced later.

[URI] - *Required-* The uri path to the asterisk gateway application. This will *always* be a https URI. When the user enters this setting, the `https://` portion of the URI will already be filled in and unchangeable.\
[PIN] - *Required-* A numeric only password for between 4 and 10 digits.\
[FALLBACK] - A hidden setting that is only changed via the app. It contains a fallback phone number in case of internet access failure.\
[XMPP_USERNAME] - *Required-* The username for the XMPP login, in user@domain format.\
[XMPP_PASSWORD] - *Required-* The password for the above login.\
[XMPP_HOST] - The hostname or IP address to connect to the XMPP server. Defaults to the @domain from the username if blank.\
[XMPP_PORT] - The port on the above host to connect to the XMPP server. Defaults to 5222.\
[XMPP_GATEWAY] - *Required-* This is the XMPP to SMS 'Component' domain.

### Call Capture and Routing Details
When the app detects an outgoing call, it retrieves the following information:
1) The dialed phone number [DIALED_NUMBER].
2) The physical phones number [CALLERID].
3) The URI for communicating to Asterisk which is stored in the apps configuration [URI].  
4) The numeric password/pin from the apps configuration [PIN].  
  
The app then makes a HTTP POST to the [URI] with the following information:\
myCallerId = [CALLERID]\
myDest = [DIALED_NUMBER]\
password = [PIN]

If the post is successful, a JSON string is returned with the following format:

	{
		"api_version": 1,
		"disanumber": "5555551234",
		"status": "SUCCESS",
		"verbose": "success",
		"exitcode": 0,
		"fallback_disanumber": "5555554321"
	}

If the post failed for some reason, the returned JSON looks like this:

	{
		"api_version": 1,
		"disanumber": "",
		"status": "FAILURE",
		"verbose": "Missing or Bad data",
		"exitcode": "1"
	}

**JSON field Descriptions:**

“api_version” - Currently Unused. For future use.\
“disanumber” - This is the number that the app should redirect to instead of the dialed number\
“status” - Either “SUCCESS” or “FAILURE”\
“verbose” - A plain language explanation of the status that can be displayed to the user or logged.\
“exitcode” - Either 0 on success or >0 on failure.\
“fallback_disanumber” - Only returned on success. Saved in the apps config for later use. Referred to as [FALLBACK] variable from here on.

**On Success (exitcode = 0)**\
The app will:
1) Display a toast "Fast Routing Outgoing Call".
2) Change the dialed number to what was returned as "disanumber" in the JSON string.
3) Connect the call.

**On Failure (exitcode = >0)**\
The app will:
1) Display a dialog box with the text:
	"Error Routing Call." and the contents of "verbose" in the JSON string.
2) Halt the call. Do not dial.

**On Failure (Fallback Method)**

It's also possible that the HTTP server could not be reached, or there was a failure on the server so that JSON is never returned. If this is the case, the app will redirect the call using the fallback method.

The app will:
1) Display a toast "Slow Routing Call".
2) Change the dialed number to:
	[FALLBACK] + "," + [PIN] + "#," [DIALED_NUMBER] + "#"
3) Connect the call.

### SMS via XMPP Details
The apps main interface when opened is a simple XMPP chat UI. At the top of the screen will be a 'plus' icon with a label "New Conversation". The middle of the screen will be a list of existing conversations.

Whenever a new conversation is created, the [XMPP_GATEWAY] variable is hidden from the user. A conversation is started with just a phone number. For instance, a new conversation started with 555-555-9876 would send it's messages to +15555559876@[XMPP_GATEWAY]. Notice that it also converts the outgoing number to e.164 format. The goal is to create an interface that does nothing but SMS, and has a interface familiar to users of other Android SMS apps.
