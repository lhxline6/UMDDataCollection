
<?php
$get = json_decode(stripslashes($_POST['req']));
// Get data from object
$userName = $get->userName; // Get username you send
// $tripNum = $get->tripNumber;
// $responses = array();
// //$responses[] = array("username" => $userName . " " . $tripNum);
// $responses[] = array($userName . " " . $tripNum);

$mysql_server_name="localhost"; 
$mysql_username="root"; 
$mysql_password="123456789"; 
$mysql_database="Ford_UMD_DataCollection"; 
$conn=mysql_connect($mysql_server_name, $mysql_username,
                                $mysql_password);
//echo json_encode($userName);
//create table
mysql_select_db("Ford_UMD_DataCollection",$conn);
// $table = "CREATE TABLE " . $userName . $tripNum . "(phoneInfo text(100),wifiInfo text(100),gpsData text(100))";

// find user number
$sql = "SHOW TABLES FROM $mysql_database";
$result = mysql_query($sql);
$idx = 1;
while ($row = mysql_fetch_row($result)) {
    if(strpos($row[0], $userName) !== false)
    {
    	$idx++;
    }
}
$responses = $idx;
// $table = "CREATE TABLE " . $userName . $idx . "(data text(500))";
$table = "CREATE TABLE " . $userName . $idx . "(phoneData text(200),watchData text(200),vehicleData text(200))";
mysql_query($table,$conn);

//$sql = mysql_query("INSERT INTO myUser (name, username, password) VALUES ('$name','$username','$password')");

mysql_close($conn);

// set header as json
header("Content-type: application/json");
 
// send response
echo json_encode($responses);
?>
