
  function drawpopup(bugid) {
    return "\
    <div id='buginfo'>\
    <img src=\"/webswamp/resources/ui/skins/common/images/rotating-tail.gif\"/> loading bug info for #" + bugid + "...\
    </div>";
  }

  function loadData(bugid) {
    return new Ajax.Updater({ success:'buginfo'}, '/webswamp/swamp',
    {
      method:'get', 
      parameters: {action: 'AjaxData', eventSubmit_doGetBugInfo: 'true', bugid: bugid},
      onFailure: function(transport){ 
        Element.update('buginfo', 
                '<div id="buginfo"><img src="$ui.image("alert_16")"/> ' + 
                '$ui.tr("An error ocurred while loading buginfo.", ${data.getUser()})</div>')
        }
    });
  }
