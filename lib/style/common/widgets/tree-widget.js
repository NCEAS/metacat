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
  