<?php
$valuePhone = stripslashes($_POST['phone_value']);
$valueWatch = stripslashes($_POST['watch_value']);
$valueVehicle = stripslashes($_POST['vehicle_value']);
// Get data from object
// $keyString = $get->keyString; // Get username you send
$idx = 1;
$tableName =  $_POST['tableName'];
//echo json_encode($getjson->table);
//echo json_encode($idx);
$keyString1 = "phoneData";
$keyString2 = "watchData";
$keyString3 = "vehicleData";
//$valuePhone = $getjson->phone_value;
//$valueWatch = $getjson->watch_value;
//echo json_encode($getjson);
//$valueVehicle = $getjson->vehicle_value;
//echo $valuePhone . "_" . $valueVehicle;
//$tableName = $getjson->tableName;

$mysql_server_name="localhost"; 
$mysql_username="root"; 
$mysql_password="123456789"; 
$mysql_database="Ford_UMD_DataCollection"; 
$conn=mysql_connect($mysql_server_name, $mysql_username,
                                $mysql_password);

mysql_select_db("Ford_UMD_DataCollection",$conn);
//mysql_query($table,$conn);

$sql = mysql_query("INSERT INTO $tableName ($keyString1,$keyString2,$keyString3) VALUES ('$valuePhone','$valueWatch','$valueVehicle')");

mysql_close($conn);

// set header as json
header("Content-type: application/json");
 
// send response
echo json_encode($valuePhone);
?>
