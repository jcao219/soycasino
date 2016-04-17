$(function() {
  $("#refresh").click(function(){
  $.getJSON( "ajax/test.json", function( data ) {
  
  for(i =0; data.length; i++){
	console.log(data[i]);
  }
  
  $.each( data, function( key, val ) {
    items.push( "<li id='" + key + "'>" + val + "</li>" );
  });
 
  $( "<ul/>", {
    "class": "accountsList",
    html: items.join( "" )
  }).appendTo( "body" );
});
  });
});