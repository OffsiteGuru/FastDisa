<?php
// This is for demo/testing

header ("Content-type: text/html; charset=utf-8");
header ("Cache-Control: no-cache, must-revalidate");  // HTTP/1.1
header ("Pragma: no-cache");

$api_version = 1; // Whole Numbers Only!

$authpw = "1234"; // the numeric password/pin

$disanumber = "5551111111"; // the number we redirect to on success

$fallback_disanumber = "5551111112"; // the number we tell the client to save for fallback use

// this is the user array. The key is the client's real caller id number. The value is what the outgoing caller id will be set to.
$user_array = array(
    "5555555555" => '"Test User" <5551111113>',
);

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
    
    // right here is where the asterisk configuration code would go.

    $output_array['disanumber'] = $disanumber;
    $output_array['status'] = 'SUCCESS';
    $output_array['verbose'] = 'success';
    $output_array['exitcode'] = 0;
    $output_array['fallback_disanumber'] = $fallback_disanumber;
    echo json_encode($output_array);
}
else {
    $output_array['verbose'] = "Missing or Bad data";
    echo json_encode($output_array);
    die();
}
    
