 /*
  *   '$RCSfile$'
  *     Purpose: Default style sheet for KNP project web pages 
  *              Using this stylesheet rather than placing styles directly in 
  *              the KNP web documents allows us to globally change the 
  *              formatting styles of the entire site in one easy place.
  *   Copyright: 2000 Regents of the University of California and the
  *               National Center for Ecological Analysis and Synthesis
  *     Authors: Matt Jones
  *
  *    '$Author: daigle $'
  *      '$Date: 2008-07-06 21:25:34 -0700 (Sun, 06 Jul 2008) $'
  *  '$Revision: 4080 $'
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
  
var dragColumn = null;

function getStyleClass (className) {
	for (var s = 0; s < document.styleSheets.length; s++) {
		if(document.styleSheets[s].rules) {
			for (var r = 0; r < document.styleSheets[s].rules.length; r++) {
				if (document.styleSheets[s].rules[r].selectorText == '.' + className) {
					return document.styleSheets[s].rules[r];
				}
			}
		}
		else if(document.styleSheets[s].cssRules){
			for (var r = 0; r < document.styleSheets[s].cssRules.length; r++) {
				if (document.styleSheets[s].cssRules[r].selectorText == '.' + className)
					return document.styleSheets[s].cssRules[r]; }
		}
	}	
	return null;
}	

function startColumnDrag(e) {
	dragColumn = findAdjustColumn(e);
}
   
function findAdjustColumn(e) {
	var objHeader2 = document.getElementById("col-header2");
	var col2XPos = objHeader2.offsetLeft;
	var objHeader3 = document.getElementById("col-header3");
	var col3XPos = objHeader3.offsetLeft;
	
	var mouseXPos = getMouseX(e);
	
	var col2XDiff = col2XPos - mouseXPos;
	var col3XDiff = col3XPos - mouseXPos;
	
	if (col2XDiff > -3 && col2XDiff < 3) {
		return 1;
	}
	
	if (col3XDiff > -3 && col3XDiff < 3) {
		return 2;
	}	

	return null;
}
		
function dragCol(e) {
	if (dragColumn != null) {
		var colLObj = document.getElementById("col-header" + dragColumn);
		var colRObj = document.getElementById("col-header" + (dragColumn + 1));
		var mouseXPos = getMouseX(e);
		var colRXPos = colRObj.offsetLeft;
		var mouseXOffset = mouseXPos - colRXPos;
		var colStyleClass = getStyleClass("col" + dragColumn);
		var colTopStyleClass = getStyleClass("col" + dragColumn + "-top");
		colStyleClass.style.width = colLObj.offsetWidth + mouseXOffset;
		colTopStyleClass.style.width = colLObj.offsetWidth + mouseXOffset -2;
	}	
}

function changeCursor(e) {
	tableClass = getStyleClass("col");
	if (findAdjustColumn(e) != null) {
		tableClass.style.cursor = "move";
	} else {
		tableClass.style.cursor = "auto";
	}
}

function handleMouseOut() {
	tableClass = getStyleClass("col");
	tableClass.style.cursor = "auto";
	reset();
}

function reset() {
	dragColumn = null;
	tableOffsetWidth = null;
}

function getMouseX(e) {	
	if (e.pageX) {
		var posx = e.pageX
	} else {
		posx = e.clientX + document.body.scrollLeft
			+ document.documentElement.scrollLeft;	
	}
		
	// catch possible negative values in NS4
	if (posx < 0) {
		posx = 0
	} 

	return posx
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
  