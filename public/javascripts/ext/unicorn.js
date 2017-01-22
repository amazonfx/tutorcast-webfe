/**
 * Unicorn Admin Template
 * Diablo9983 -> diablo9983@gmail.com
**/
$j(document).ready(function(){

	
	
	// === Sidebar navigation === //
	
	$j('.submenu > a').click(function(e)
	{
		e.preventDefault();
		var submenu = $j(this).siblings('ul');
		var li = $j(this).parents('li');
		var submenus = $j('#sidebar li.submenu ul');
		var submenus_parents = $j('#sidebar li.submenu');
		if(li.hasClass('open'))
		{
			if(($j(window).width() > 768) || ($j(window).width() < 479)) {
				submenu.slideUp();
			} else {
				submenu.fadeOut(250);
			}
			li.removeClass('open');
		} else 
		{
			if(($j(window).width() > 768) || ($j(window).width() < 479)) {
				submenus.slideUp();			
				submenu.slideDown();
			} else {
				submenus.fadeOut(250);			
				submenu.fadeIn(250);
			}
			submenus_parents.removeClass('open');		
			li.addClass('open');	
		}
	});
	
	var ul = $j('#sidebar > ul');
	
	$j('#sidebar > a').click(function(e)
	{
		e.preventDefault();
		var sidebar = $j('#sidebar');
		if(sidebar.hasClass('open'))
		{
			sidebar.removeClass('open');
			ul.slideUp(250);
		} else 
		{
			sidebar.addClass('open');
			ul.slideDown(250);
		}
	});
	
	// === Resize window related === //
	$j(window).resize(function()
	{
		if($j(window).width() > 479)
		{
			ul.css({'display':'block'});	
			$j('#content-header .btn-group').css({width:'auto'});		
		}
		if($j(window).width() < 479)
		{
			ul.css({'display':'none'});
			fix_position();
		}
		if($j(window).width() > 768)
		{
			$j('#user-nav > ul').css({width:'auto',margin:'0'});
            $j('#content-header .btn-group').css({width:'auto'});
		}
	});
	
	if($j(window).width() < 468)
	{
		ul.css({'display':'none'});
		fix_position();
	}
	if($j(window).width() > 479)
	{
	   $j('#content-header .btn-group').css({width:'auto'});
		ul.css({'display':'block'});
	}
	
	// === Tooltips === //
	$j('.tip').tooltip();	
	$j('.tip-left').tooltip({ placement: 'left' });	
	$j('.tip-right').tooltip({ placement: 'right' });	
	$j('.tip-top').tooltip({ placement: 'top' });	
	$j('.tip-bottom').tooltip({ placement: 'bottom' });	
	
	// === Search input typeahead === //
	$j('#search input[type=text]').typeahead({
		source: ['Dashboard','Form elements','Common Elements','Validation','Wizard','Buttons','Icons','Interface elements','Support','Calendar','Gallery','Reports','Charts','Graphs','Widgets'],
		items: 4
	});
	
	// === Fixes the position of buttons group in content header and top user navigation === //
	function fix_position()
	{
		var uwidth = $j('#user-nav > ul').width();
		$j('#user-nav > ul').css({width:uwidth,'margin-left':'-' + uwidth / 2 + 'px'});
        
        var cwidth = $j('#content-header .btn-group').width();
        $j('#content-header .btn-group').css({width:cwidth,'margin-left':'-' + uwidth / 2 + 'px'});
	}
	
	// === Style switcher === //
	$j('#style-switcher i').click(function()
	{
		if($j(this).hasClass('open'))
		{
			$j(this).parent().animate({marginRight:'-=190'});
			$j(this).removeClass('open');
		} else 
		{
			$j(this).parent().animate({marginRight:'+=190'});
			$j(this).addClass('open');
		}
		$j(this).toggleClass('icon-arrow-left');
		$j(this).toggleClass('icon-arrow-right');
	});
	
	$j('#style-switcher a').click(function()
	{
		var style = $j(this).attr('href').replace('#','');
		$j('.skin-color').attr('href','css/unicorn.'+style+'.css');
		$j(this).siblings('a').css({'border-color':'transparent'});
		$j(this).css({'border-color':'#aaaaaa'});
	});
});
