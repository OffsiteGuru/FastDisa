<?php
/*

*** ALL SETTINGS ARE STORED IN fastdisa.inc! ***

This is a php script designed to run on the Asterisk server. It connects to to the Asterisk Management Interface (AMI) on localhost
and makes changes to a dialplan file called fastdisa.conf.

It may need to be modified to work with your dialplan, but it should give you a good idea of how to go about it.

There are a couple of contexts used that are specific to FreePBX. (sub-record-check, from-internal)
*/

// Set some headers so nothing ever caches the output.
header ("Content-type: text/html; charset=utf-8");
header ("Cache-Control: no-cache, must-revalidate");  // HTTP/1.1
header ("Pragma: no-cache");

// Include the configuration variables.
require 'fastdisa.inc';

// If we ever change the format of the output json, we can bump this version so the client knows.
$api_version = 1; // Whole Numbers Only!

// A debug function
function display_debug($key, $value) {
    global $debug;
    global $debug_file;
    if($debug){
        switch (gettype($value)) {
        case 'string' :
            file_put_contents($debug_file, $key . ' is ' . mb_strlen($value) . ' bytes' . PHP_EOL, FILE_APPEND | LOCK_EX);
            file_put_contents($debug_file, $key . " = " . $value . PHP_EOL, FILE_APPEND | LOCK_EX);
            break;
        case 'array' :
        case 'object' :
        default :
            file_put_contents($debug_file, $key . ' array is ' . mb_strlen(serialize($value)) . ' bytes when serialized' . PHP_EOL, FILE_APPEND | LOCK_EX);
            file_put_contents($debug_file, $key . " = " . PHP_EOL . print_r($value) . PHP_EOL, FILE_APPEND | LOCK_EX);
            break;
        }
    }
}

// Initallize the output array.
$output_array = array(
    "api_version" => $api_version,
    "disanumber" => '', // We won't output this unless the call is successful.
    "status" => "FAILURE",  // This is the default. It's changed on success.
    "verbose" => "Undefined Failure",
    "exitcode" => "1",
);

// This function just outputs the JSON string. We register it to be run when script is finished,
// so we don't need to call 'echo json_encode($array)' everywhere.
function output_json() {
    global $output_array;
    echo json_encode($output_array);
}
register_shutdown_function('output_json');

// Check for required fields imported from fastdisa.incoming
// These are all the string variables that cannot be empty
$var_array = array(
    '$debug_file',
    '$host',
    '$port',
    '$username',
    '$secret',
    '$incoming_timeout',
    '$authpw',
    '$disanumber',
    '$fallback_disanumber',
    '$user_array',
);

$missingvars = 0;
$strError = 'CONF FILE ERROR:';

// Make sure $debug is either true or false
if ($debug !== false && $debug !== true) {
    $missingvars++;
    $strError .= ' $debug boolean missing or invalid.';
}

// Loop though the other variables and make sure they're not empty.
foreach ($var_array as &$var) {
    if (eval('return empty(' . $var . ');')) {
        $strError .= " " . $var . " is empty.";
        $missingvars++;
    };
};

// These fields must only contain digits.
$var_numerics = array(
    '$port',
    '$incoming_timeout',
    '$authpw',
    '$disanumber',
    '$fallback_disanumber',
);

// Loop though the numeric fields annd make sure they only contain digits.
foreach ($var_numerics as &$var) {
    if (!eval('return ctype_digit(' . $var . ');')) {
        $strError .= " " . $var . " is not numeric.";
        $missingvars++;
    };
};

// If we have any problems, output them and die.
if ($missingvars > 0) {
    $output_array['verbose'] = $strError;
    $output_array['status'] = "SVR CONF BAD";
    display_debug('ERROR', $strError);
    die();
}

// Copied & Modified from https://github.com/osk2/php-ami-class
class AstMan {

    public $socket;
    public $error;
    public $amiHost = "127.0.0.1";
    public $amiPort = "5038";
    public $amiUsername = "admin";
    public $amiPassword = "admin";
    
    function __constructor() {
        $this -> socket = false;
        $this -> error = "";
    } 

    function Login() {
        
        $this -> socket = @fsockopen($this -> amiHost,$this -> amiPort, $errno, $errstr, 1);
        if (!$this -> socket) {
            $this -> error =  "Could not connect: $errstr ($errno)";
            return false;
        }else{
            stream_set_timeout($this -> socket, 1);
            $amiUsername = $this -> amiUsername;
            $amiPassword = $this -> amiPassword;
            $wrets = $this -> Query("Action: Login\r\nUserName: $amiUsername\r\nSecret: $amiPassword\r\nEvents: off\r\n\r\n");
            if (strpos($wrets, "Message: Authentication accepted") !== false) {
                return true;
            }else{
                $this -> error = "Could not login: Authentication failed.";
                fclose($this -> socket); 
                $this -> socket = false;
                return false;
            }
        }
    }
    
    function Logout() {
        if ($this -> socket) {
            fputs($this -> socket, "Action: Logoff\r\n\r\n");
            while (!feof($this -> socket)) {
                $wrets .= fread($this -> socket, 8192);
            }
            fclose($this -> socket);
            $this -> socket = false;
        }
        return; 
    }
    
    function Query($query, $debug = false) {
        $wrets = "";
        if ($this -> socket === false) {
            $this -> error = "No connection.";
            return false;
        }	
        fputs($this -> socket, $query);
        do {
            $line = fgets($this -> socket, 4096);
            $wrets .= $line;
            $info = stream_get_meta_data($this -> socket);
        } while ($line != "\r\n" && $info["timed_out"] === false );
        return $wrets;
    }

    function Reload() {
        $query = "Action: Command\r\nCommand: Reload\r\n\r\n";
        $wrets = "";
        
        if ($this -> socket === false) {
            $this -> error = "No connection.";
            return false;
        }
            
        fputs($this -> socket, $query);
        do
        {
            $line = fgets($this -> socket, 4096);
            $wrets .= $line;
            $info = stream_get_meta_data($this -> socket);
        }while ($line != "\r\n" && $info["timed_out"] === false );
        return $wrets;
    }

    function GetUsers() {
        $query = "Action: SIPpeers\r\n\r\n";
        $wrets = "";
        
        if ($this -> socket === false) {
            $this -> error = "No connection.";
            return false;
        }
            
        fputs($this -> socket, $query); 
        do
        {
            $line = fgets($this -> socket, 4096);
            $wrets .= $line;
            $info = stream_get_meta_data($this -> socket);
        } while ($line != "Event: PeerlistComplete\r\n" && $info["timed_out"] === false );
        return $wrets;
    }

    function AddUser($user, $type, $dir) {
        if ($user && $type && $dir) {
            $file = fopen($dir, "a+");
            switch ($type) {
                case "webrtc":
                    $str = "[".$user."]\n type=peer\n username=".$user."\n host=dynamic\n secret=".$user."\n context=default\n hasiax = no\n hassip = yes\n encryption = yes\n avpf = yes\n icesupport = yes\n videosupport=no\n directmedia=no\n nat=yes\n qualify=yes\n\n";
                    break;
                case "sip":
                    $str = "[".$user."]\n type=peer\n username=".$user."\n host=dynamic\n secret=".$user."\n context=default\n hasiax = no\n hassip = yes\n nat=yes\n\n";
                    break;
            }
            fwrite($file, $str);
            fclose($file);
            return true;
        }else{
            $this -> error = "One or more parameters are missing.";
            return false;
        }
    }

    function AddExtension($user, $dir) {
        if ($user && $dir) {
            $file = fopen($dir, "a+");
            $str = "exten => ".$user.",1,Dial(SIP/".$user.")\n";
            fwrite($file, $str);
            fclose($file);
            return true;
        }else{
            $this -> error = "One or more parameters are missing.";
            return false;
        }
    }
    
    function GetError() {
        return $this -> error;
    }
}

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
        $outgoing_callerid = $user_array[$callerid]['cid'];
        display_debug('$outgoing_callerid', $outgoing_callerid);
    } else {
        $output_array['verbose'] = 'Unknown Caller ID: ' . $callerid;
        display_debug('ERROR', 'Unknown Caller ID');
        die();
    }

    // Declare a AstMan Object
    $astman = new AstMan;

    // Set login info in class
    $astman->amiHost = $host;
    $astman->amiUsername = $username;
    $astman->amiPassword = $secret;
    $astman->amiPort = $port;

    // Open the socket then login
    $result = $astman->Login();
    if ($result === false) {
        display_debug('ERROR', "AMI Login Failed. Reason: " . $astman->GetError());
        $output_array['verbose'] = 'AMI Login Failure';
        die();
    } else {
        display_debug('DEBUG', "Login Successful as user '" . $username . "'");
        display_debug('DEBUG', "RESULT:\r\n" . $result);
    }
    
    // Define Delete Category String
    $in = "Action: UpdateConfig\r\n";
    $in .= "ActionID: 002\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Srcfilename: fastdisa.conf\r\n";
    $in .= "Dstfilename: fastdisa.conf\r\n";
    $in .= "Action-000000: delcat\r\n";
    $in .= "Cat-000000: fastdisa-". $callerid . "\r\n\r\n";

    display_debug('QUERY', "\r\n" . $in);

    $result = $astman->Query($in);

    if ($result === false) {
        display_debug('ERROR', "Unable to run query. Reason: " . $astman->GetError());
        $output_array['verbose'] = 'UpdateConfig failed';
        die();
    } else {
        display_debug('DEBUG', "Query Success. Result:\r\n" . $result);
    }

    // Define add Category string
    $in = "Action: UpdateConfig\r\n";
    $in .= "ActionID: 003\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Srcfilename: fastdisa.conf\r\n";
    $in .= "Dstfilename: fastdisa.conf\r\n";
    $in .= "Action-000000: newcat\r\n";
    $in .= "Cat-000000: fastdisa-". $callerid . "\r\n\r\n";

    display_debug('QUERY', "\r\n" . $in);

    $result = $astman->Query($in);

    if ($result === false) {
        display_debug('ERROR', "Unable to run query. Reason: " . $astman->GetError());
        $output_array['verbose'] = 'UpdateConfig failed';
        die();
    } else {
        display_debug('DEBUG', "Query Success. Result:\r\n" . $result);
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
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Set(CALLERID(all)=" . $outgoing_callerid . ")\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $in .= "Value-". $padnum .": >s,n,Gosub(sub-record-check,s,1(out,\${EXTEN}," . $user_array[$callerid]['recpol'] . "))\r\n";
    
    $num++;
    $padnum = str_pad($num,6,"0",STR_PAD_LEFT);
    $in .= "Action-". $padnum .": append\r\n";
    $in .= "Cat-". $padnum .": fastdisa-". $callerid . "\r\n";
    $in .= "Var-". $padnum .": exten\r\n";
    $until = time() + $incoming_timeout;
    $in .= "Value-". $padnum .": >s,n,GotoIf($[\${EPOCH} < " . $until . "]?from-internal,". $dest .",1)\r\n"; // THIS LINE DEFINES THE OUTGOING CALL CONTEXT (from-internal)
    
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
    $in .= "Value-". $padnum .": >s,n,Goto(from-internal,\${NUMBER_TO_CALL},1)\r\n";
    
    $in .= "\r\n"; // this one goes at the end

    display_debug('QUERY', "\r\n" . $in);

    $result = $astman->Query($in);

    if ($result === false) {
        display_debug('ERROR', "Unable to run query. Reason: " . $astman->GetError());
        $output_array['verbose'] = 'UpdateConfig failed';
        die();
    } else {
        display_debug('DEBUG', "Query Success. Result:\r\n" . $result);
    }
    
    // Define append to Category string
    $in = "Action: GetConfig\r\n";
    $in .= "ActionID: 005\r\n";
    $in .= "Reload: no\r\n";
    $in .= "Filename: fastdisa.conf\r\n";
    $in .= "Cat-000000: fastdisa-". $callerid . "\r\n\r\n";

    display_debug('QUERY', "\r\n" . $in);

    $result = $astman->Query($in);

    if ($result === false) {
        display_debug('ERROR', "Unable to run query. Reason: " . $astman->GetError());
        $output_array['verbose'] = 'GetConfig failed';
        die();
    } else {
        display_debug('DEBUG', "Query Success. Result:\r\n" . $result);
    }
    
    if (strpos($result, $dest) !== false) { // we search for the dest number in the config as a final test of success.
        // Define Reload string
        $in = "Action: Command\r\n";
        $in .= "ActionID: 006\r\n";
        $in .= "Command: dialplan reload\r\n\r\n";

        display_debug('QUERY', "\r\n" . $in);

        $result = $astman->Query($in);
    
        if ($result === false) {
            display_debug('ERROR', "Unable to run query. Reason: " . $astman->GetError());
            $output_array['verbose'] = 'Dialplan Reload Failed';
            die();
        } else {
            display_debug('DEBUG', "Query Success. Result:\r\n" . $result);
        }

        if (strpos($result, 'Follows') == false) {  // Granted, this only checks that asterisk as recieved the command. But waiting for it to say 'Dialplan reloaded' takes too long for me. :P
            display_debug('ERROR', "Unable to Reload Dialplan");
            $output_array['verbose'] = "Unable to Reload Dialplan";
            die();
        }
        display_debug('DEBUG',"Dialplan Reloaded");

        $output_array['disanumber'] = $disanumber;
        $output_array['status'] = 'SUCCESS';
        $output_array['verbose'] = 'success';
        $output_array['exitcode'] = 0;
        $output_array['fallback_disanumber'] = $fallback_disanumber;
    } else {
        display_debug('DEBUG',"Failed to find dest number in config"); //FAILURE
        die();
    }
    $astman->Logout();
}
else {
    $output_array['verbose'] = "Missing or Bad data";
    die();
}
