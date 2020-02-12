<?php

// Do we log debug messages, and to which file?
$debug = true;
$debug_file = 'fastdisa.log'; //NOTE: This file may be readable though the web server, and it will contain your authpw number. You want that to be kept secret! Use a .htaccess file to block it.

/* Example .htaccess file:
<Files "*.log">
    order allow,deny
    deny from all
    satisfy all
</Files>
*/

// This is the AMI login info.
$host = "127.0.0.1"; // I wouldn't recomend a remote host, but it may work. Remember it's unencrypted though.
$port = "5038"; // NUMBERS ONLY
$username = "admin";
$secret = "admin";

// How log to allow calls to be automatically routed once defined (in seconds)?
$incoming_timeout = "15"; // NUMBERS ONLY

// The numeric password for the FastDisa client to authenticate against.
//You'll want to keep it a reasonable length, since it's dialed by the app when using the fallback.
//The longer it is, the more time to connect the call on fallback. Has no delay when fast routed.
$authpw = "CHANGEME"; // NUMBERS ONLY

// The number we tell the FastDisa client to call to connect
$disanumber = "CHANGEME"; // 10 digit telephone number like '3119491234'
$fallback_disanumber = $disanumber; // The fallback number (for manually dialing though) can be different, if we want it to.

/* This is the user list (client caller-id = user), with a nested array of options per user.
$user_array = array(
    "<incoming caller-id number" => array(
                        "cid" => '"outgoing caller-id name" <OUTGOING CALLER ID NUMBER>',
                        "recpol" => "recording policy [force,yes,dontcare,no,never]", // this is a FreePBX setting: https://wiki.freepbx.org/display/FPG/Call+Recording+walk+through
                    ),
);

Example:
$user_array = array(
    "3119491234" => array(
                        "cid" => '"John Doe" <2126647665>',
                        "recpol" => "yes", // this is a FreePBX setting: https://wiki.freepbx.org/display/FPG/Call+Recording+walk+through
                    ),
);
*/

// You'll nead at least one user in this array. See the example above.
$user_array = array(
    
);
