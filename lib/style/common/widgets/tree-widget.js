 /*
  *     Purpose: Default style sheet for KNP project web pages 
  *              Using this stylesheet rather than placing styles directly in 
  *              the KNP web documents allows us to globally change the 
  *              formatting styles of the entire site in one easy place.
  */
   
function showBranch(branch) {
	var objBranch = document.getElementById(branch).style;
	if(objBranch.display=="block")
		objBranch.display="none";
	else
		objBranch.display="block";
}

   
function swapFolder(openImageLocation, closedImageLocation, img) {
	var openImg = new Image();
	openImg.src = openImageLocation;
	var closedImg = new Image();
	closedImg.src = closedImageLocation;
	
	objImg = document.getElementById(img);
	if(objImg.src.indexOf(closedImageLocation)>-1)
		objImg.src = openImg.src;
	else
	objImg.src = closedImg.src;
}
  