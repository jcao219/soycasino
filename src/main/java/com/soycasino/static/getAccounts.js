$(function() {
  $("#refresh").click(function(){
  console.log("somestring");
    $.getJSON( "getAccounts", function( data ) {
    
    $("#genHere").empty();
  
	  for(i =0; i< data.length; i++){
		console.log(data[i]);
		var node ="<tr><td>\
		<label><input type=\"RADIO\" name=\"acc_id\" checked=\"\" value=\""+ data[i]._id + "\">";
		node = node + data[i].nickname + "</label></td>";
        node = node + "<td>";
		node = node + data[i].balance + "</td></tr>";

       $("#genHere").append(node);
	  }  
 
    });
  });
});