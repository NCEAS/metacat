
function get_reviews(docid, metacatUrl) {
	if (docid != null && docid != '') {
		new Ajax.Request( metacatUrl + "?action=read&docid=" + docid,
			{asynchronous:true, evalScripts:true, method:'post',
			onLoading:function(request){Element.show('busy')},
			onComplete: render_reviews});
	}
}

function render_reviews(request) {
	//alert(request.responseText);
	var reviews = request.responseXML.getElementsByTagName("review");
	var div_panel;

	// for each review...
	for (var i=0; i<reviews.length; i++) {

		div_panel = document.createElement("div");
		panelName = "panel" + (i+1);

		var packageId_string = getText(reviews[i], "packageId");
		var action = document.createTextNode("Action: " + getText(reviews[i], "action"));
		var datetime = document.createTextNode(getText(reviews[i], "datetime"));
		var text_string = getText(reviews[i], "text");
		var text = document.createTextNode(text_string);

		// 22 title chars or 34 blurb chars can fit in one title bar
		var title_max_length = 18;
		var blurb_max_length = 22;
		var title_length = packageId_string.length;

		var blurb_string = "";
		if (title_length > title_max_length) {
			// abbreviate the title
			packageId_string = packageId_string.substring(0, title_max_length-3) + "...";
		} else {
			// try to squeeze in the blurb
			blurb_length = Math.floor((title_max_length - title_length) * (blurb_max_length / title_max_length));
			if (text_string.length > blurb_length) {
				// abbreviate the blurb
				if (blurb_length > 3) {
					blurb_length -= 3;
					suffix = "...";
				} else {
					suffix = "";
				}
				blurb_string = text_string.substring(0, blurb_length) + suffix;
			} 
		}


		var packageId = document.createTextNode(packageId_string);
		var blurb = document.createTextNode(blurb_string);

		var div_header = document.createElement("div");
		var span_blurb = document.createElement("span");
		var div_content = document.createElement("div");
		var p_tstamp = document.createElement("p");
		var p_action = document.createElement("p");
		var p_text = document.createElement("p");


		div_header.appendChild(packageId);
		span_blurb.appendChild(blurb);
		div_header.appendChild(span_blurb);

		p_tstamp.appendChild(datetime);
		p_action.appendChild(action);
		p_text.appendChild(text);
		div_content.appendChild(p_tstamp);
		div_content.appendChild(p_action);
		div_content.appendChild(p_text);

		div_panel.setAttribute("id", panelName);
		div_header.setAttribute("id", panelName + "Header");
		div_header.setAttribute("class", "tabtitle");
		div_content.setAttribute("id", panelName + "Content");
		span_blurb.setAttribute("id", panelName + "Blurb");
		span_blurb.setAttribute("class", "review_blurb");
		div_content.setAttribute("class", "accordionTabContentBox");
		p_tstamp.setAttribute("class", "review_tstamp");
		p_action.setAttribute("class", "review_action");
		p_text.setAttribute("class", "review_text");

		div_header.setAttribute("className", "tabtitle");
		span_blurb.setAttribute("className", "review_blurb");
		div_content.setAttribute("className", "accordionTabContentBox");
		p_tstamp.setAttribute("className", "review_tstamp");
		p_action.setAttribute("className", "review_action");
		p_text.setAttribute("className", "review_text");

		div_panel.appendChild(div_header);
		div_panel.appendChild(div_content);

		document.getElementById("review_list").appendChild(div_panel);
	}

	var which_tab = 0;
	Element.hide("panel" + (which_tab+1) + "Blurb");
	var accordion = new Rico.Accordion('review_list',
		{
			borderColor:"#ddd",
			expandedBg:"#DEF1F1",
			expandedTextColor:"#000",
			collapsedBg:"#A3DADA",
			collapsedTextColor:"#444",
			hoverBg:"#DEF1F1",
			hoverTextColor:"#222",
			panelHeight:150,
			onLoadShowTab:which_tab
		});

	// could have used onShowTab/onHideTab but this has a better effect
	accordion.clickBeforeActions.push(show_current_blurb);
	accordion.clickAfterActions.push(hide_current_blurb);

	// not busy anymore
	setTimeout("Element.hide('busy')", 1000);
}

// for accordion
function get_current_panel(tab) {
  var panelName = tab.accordion.lastExpandedTab.titleBar.id;
  return panelName.substring(0, panelName.indexOf("Header"));
}

// for accordion
function show_current_blurb(tab) {
  Element.show(get_current_panel(tab) + "Blurb");
}

// for accordion
function hide_current_blurb(tab) {
  Element.hide(get_current_panel(tab) + "Blurb");
}


function getText(elem, tag) {
	var node = elem.getElementsByTagName(tag)[0];
	if (node != null && node.firstChild != null) {
		return elem.getElementsByTagName(tag)[0].firstChild.nodeValue.trim();
	}
	return "";
}

function expand_detail(rid) {
	id = "review_" + rid;
	Element.hide(id + '_show');
	Element.hide(id + '_blurb');
	Element.show(id + '_hide');
	new Effect.BlindDown(id + "_detail", {duration:0.3})
}

function hide_detail(rid) {
	id = "review_" + rid;
	new Effect.BlindUp(id + "_detail", {duration:0.3})
	Element.hide(id + '_hide');
	Element.show(id + '_show');
	Element.show(id + '_blurb');
	//setTimeout("Element.show(id + '_show'); Element.show(id + '_blurb')", 250)
}


