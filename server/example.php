<?php

/*
This is a php script designed to run on the Asterisk server. It connects to to the Asterisk Management Interface (AMI) on localhost
and makes changes to a dialplan file called fastdisa.conf.

This was created as a quick and dirty script to get the job done. It's imperfect. It will need to be modified to work with your dialplan,
but it should give you a good idea of how to go about it.

There are a couple of contexts used that are specific to FreePBX. (sub-record-check, from-internal)
*/

// Set some headers so nothing ever caches the output.
header ("Content-type: text/html; charset=utf-8");
header ("Cache-Control: no-cache, must-revalidate");
header ("Pragma: no-cache");

// Tell the client what API version we're using (for future use)
$api_version = 1; // Whole Numbers Only!

// The Username and Password for the AMI
$username = "admin";
$secret = "secret";

// How long will the dialplan automatically route an incoming call from the client, in seconds
$incoming_timeout = 15;

// The Password/Pin for incoming routing changes. This also becomes the pin code used for the fallback DISA method.
$authpw = "9999";

// The number that the script tells the client to dial to connect the call
$disanumber = "5555550001";

// The number that the script tells the client to use for a fallback DISA dial (It doesn't really need to be different than $disanumber, depending on your dial plan)
$fallback_disanumber = "5555550002";

// this is the user array. The key is the client's real caller id number. The value is what the outgoing caller id will be set to.
$user_array = array(
    "5555555555" => '"Test User" <5551111111>',
);

// The default output JSON is here.
$output_array = array(
    "api_version" => $api_version,
    "disanumber" => '', // We won't output this unless the call is successful.
    "status" => "FAILURE",  // This is the default. It's changed on success.
    "verbose" => "Undefined Failure",
    "exitcode" => "1",
);

// This is for testing from the command line
if (!isset($_SERVER["HTTP_HOST"])) {
  parse_str($argv[1], $_POST);
}

// Check to make sure that the POST data is correct and not missing
if(isset($_POST['myCallerId']) && isset($_POST['myDest']) && is_numeric($_POST['myCallerId']) && is_numeric($_POST['myDest']) && $_POST['password'] == $authpw ) {

    // Clean up input, just in case.
    $callerid = preg_replace('/[^0-9]/', '', $_POST['myCallerId']);
    $dest = preg_replace('/[^0-9]/', '', $_POST['myDest']);
    
    // Check incoming caller id. Make sure it's a valid phone.
    if (array_key_exists($callerid, $user_array)) {
        $outgoing_callerid = $user_array[$callerid];
    } else {
    $output_array['verbose'] = 'Unknown Caller ID';
        echo json_encode($output_array);
        die();
    }
    
    // Declare the socket
    $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
    if ($socket === false) {
    echo "socket_create() failed: reason: " . socket_strerror(socket_last_error()) . "\n";
    }
    
    // Set the socket timeout
    socket_set_option($socket,SOL_SOCKET,SO_RCVTIMEO,array("sec"=>0,"usec"=>100000)); // 100000 usec = 0.1 sec
    
    // Socket connect
    $result = socket_connect($socket, localhost, 5038);
    if ($result === false) {
        echo "socket_connect() failed.\nReason: ($result) " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    
    // Define AMI Login string
    $loginstring = "Action: Login\r\n";
    $loginstring .= "ActionID: 000\r\n";
    $loginstring .= "Username: ".$username."\r\n";
    $loginstring .= "Secret: ".$secret."\r\n\r\n";
    
    // Send Login String
    socket_write($socket, $loginstring, strlen($loginstring));
    
    // Read out Response to Login
    $buf = 'This is my buffer.';
    if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
        echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    
    if (strpos($buf, 'Success') == false) {
        $output_array['verbose'] = 'AMI Login Failure';
        die();
    }
    
    // Define string to disable event output
    $in = "Action: Events\r\n";
    $in .= "ActionID: 001\r\n";
    $in .= "EventMask: off\r\n\r\n";
    
    // Send disable event output string
    socket_write($socket, $in, strlen($in));
    
    // Read out response
    $buf = 'This is my buffer.';
    if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
        echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    if (strpos($buf, 'Success') == false) {
        $output_array['verbose'] = 'Unable to disable Event Output';
        die();
    }
    
    // Define Delete Category String
    $in = "Action: UpdateConfig\r\n";
    $in .= "ActionID: 002\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Srcfilename: fastdisa.conf\r\n";
    $in .= "Dstfilename: fastdisa.conf\r\n";
    $in .= "Action-000000: delcat\r\n";
    $in .= "Cat-000000: fastdisa-". $callerid . "\r\n\r\n";
    
    // Send Delete Category String
    socket_write($socket, $in, strlen($in));
    
    // Read out response
    $buf = 'This is my buffer.';
    if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
        echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    
    // Define add Category string
    $in = "Action: UpdateConfig\r\n";
    $in .= "ActionID: 003\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Srcfilename: fastdisa.conf\r\n";
    $in .= "Dstfilename: fastdisa.conf\r\n";
    $in .= "Action-000000: newcat\r\n";
    $in .= "Cat-000000: fastdisa-". $callerid . "\r\n\r\n";
    
    // Send data and read response
    socket_write($socket, $in, strlen($in));
    $buf = 'This is my buffer.';
    if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
        echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    if (strpos($buf, 'Success') == false) {
        $output_array['verbose'] = "Couldn't add new Category";
        die();
    }
    
    // Define append to Category string
    $in = "Action: UpdateConfig\r\n";
    $in .= "ActionID: 004\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Srcfilename: fastdisa.conf\r\n";
    $in .= "Dstfilename: fastdisa.conf\r\n";
    
    // Here are the numbered actions... increment the $num variable between them!
    $num = 0;
    
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,1,Set(_KEEPCID=TRUE)\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $until = time() + $incoming_timeout;
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Set(CALLERID(all)=" . $outgoing_callerid . ")\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Gosub(sub-record-check,s,1(out,\${EXTEN},force))\r\n"; // the calls WILL be recorded (FreePBX specific)
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,GotoIf($[\${EPOCH} < " . $until . "]?from-internal,". $dest .",1)\r\n"; // THIS LINE DEFINES THE OUTGOING CALL CONTEXT
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Read(PASSCODE)\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,GotoIf(\$[\${PASSCODE} = ".$authpw."]?goodtogo)\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Hangup()\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n(goodtogo),NoOp()\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Read(NUMBER_TO_CALL)\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Goto(from-internal,\${NUMBER_TO_CALL},1)\r\n"; // THIS LINE DEFINES THE OUTGOING CALL CONTEXT DURING FALLBACK
    
    $in .= "\r\n"; // this one goes at the end
    
    // Send data and read response
    socket_write($socket, $in, strlen($in));
    $buf = 'This is my buffer.';
    if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
        echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    if (strpos($buf, 'Success') == false) {
        $output_array['verbose'] = "Unable to add exten callerid record";
        die();
    }
    
    // Define append to Category string
    $in = "Action: GetConfig\r\n";
    $in .= "ActionID: 005\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Filename: fastdisa.conf\r\n";
    $in .= "Cat-000000: fastdisa-". $callerid . "\r\n\r\n";
    
    // Send data and read response
    socket_write($socket, $in, strlen($in));
    $buf = 'This is my buffer.';
    if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
        echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
    }
    if (strpos($buf, $dest) !== false) { // we search for the dest number in the config as a final test of success.
        // Define Reload string
        $in = "Action: Command\r\n";
        $in .= "ActionID: 006\r\n";
        $in .= "Command: dialplan reload\r\n\r\n";
        socket_write($socket, $in, strlen($in));
        $buf = 'This is my buffer.';
        if (false == ($bytes = socket_recv($socket, $buf, 2048, MSG_WAITALL))) {
            echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
        }
        if (strpos($buf, 'Follows') == false) {  // Granted, this only checks that asterisk as recieved the command. But waiting for it to say 'Dialplan reloaded' takes too long for me. :P
            die("FAILURE\r\nUnable to Reload Dialplan\r\n");
        }
        $output_array['disanumber'] = $disanumber;
        $output_array['status'] = 'SUCCESS';
        $output_array['verbose'] = 'success';
        $output_array['exitcode'] = 0;
        $output_array['fallback_disanumber'] = $fallback_disanumber;
        echo json_encode($output_array);
        
    } else {
        echo json_encode($output_array); // Failure
        die();
    }
    socket_close($socket);
}
else {
    $output_array['verbose'] = "Missing or Bad data";
    echo json_encode($output_array);
    die();
}
